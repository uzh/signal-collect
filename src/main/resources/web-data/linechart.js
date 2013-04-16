/**
 * Object that encapsulates a complete graph.
 * 
 * This object includes all data that needs to be stored about a graph, as well as the data
 * that is visualized in the graph. This object also offers methods to set up and update a
 * graph.
 */
var LineChart = function()
{
  
  // default config
  this.config = {
      jsonName     : "",
      skip         : false, // can be used to skip elements from the websocket (e.g. OS names)
      prettyName   : "",
      dataCallback : null,
      numOfValues  : 100,
      margin       : { top: 20, right: 20, bottom: 30, left: 50 },
      width        : 550,
      height       : 250,
  };
  
  this.container = null;
  
  // data array for avg, min, max values
  var data = [ [], [], [] ];
  
  // variables needed for the charts
  var svg, xAxis, yAxis, line, aLineContainer, x, y, path, currentHighestDate = 0, maxYValue = 0, zoom, zoomScale = 1.0;
  
  // variables needed for the tool tips
  var div, formatTime;
  
  // returns the number of time points
  this.dataLength = function() {
    return data[0].length;
  }
  
  // returns the average
  this.dataAvg = function() {
    return Array.avg($.map(data[0], function (e) { return e.value; }));
  }
  
  // returns the latest data value
  this.dataLatest = function() {
    return data[2][data[2].length-1].value;
  }
  
  // sets the zooming level
  this.setZoom = function(scale) {
    var newZoomScale   = zoomScale * scale;
    var lowestXDomain  = x.domain()[0];
    var highestXDomain = x.domain()[1];

    if (newZoomScale > 1.5) {
      return;
    }
    
    zoom.scale(zoom.scale() * scale);
    zoomScale = newZoomScale;
    
    var currentDateInWindow   = (lowestXDomain <= currentHighestDate
                                 && currentHighestDate <= new Date(highestXDomain.addMilliseconds(interval)));
    var dataComesAfterWindow  = (highestXDomain <= data[0][0].date);
    var dataComesBeforeWindow = (lowestXDomain >= data[0][this.dataLength()-1].date);
    if (dataComesAfterWindow || currentDateInWindow || dataComesBeforeWindow) {
      this.setMove(0);
    } else {
      drawAnimated(true);
    }
  }
  
  // moves the charts
  this.setMove = function(scale) {
    var lowestXDomain  = x.domain()[0];
    var highestXDomain = x.domain()[1];
    var differenceMS = highestXDomain - lowestXDomain;
    var newLowestXDomain, newHighestXDomain;
    
    if (scale == 0) {
      newHighestXDomain = new Date(currentHighestDate.addMilliseconds(interval));
      newLowestXDomain  = new Date(newHighestXDomain.addMilliseconds(-differenceMS));
    } else {      
      var moveMS       = scale * Math.round(differenceMS / 3);
      newLowestXDomain  = new Date(lowestXDomain.addMilliseconds(moveMS));
      newHighestXDomain = new Date(highestXDomain.addMilliseconds(moveMS));
    }
    
    d3.transition().ease("linear").duration(300).each(function() {
      x.domain([newLowestXDomain, newHighestXDomain]);
      zoom.x(x);
      drawAnimated(true);
    });
  }
  
  
  
  
  /**
   * Performs all actions needed for the LineChart setup
   */
  this.setup = function(config) {
    
    // replace default configuration
    $.extend(this.config, config);

    // set default data callback if needed
    if (this.config.dataCallback == null) {
      this.config.dataCallback = function(newData) {
        var that = this; // access this
        return newData.workerStatistics[that.jsonName];
      };
    }
    
    // set default prettyName to jsonName if needed
    if (this.config.prettyName == "") {
      if (chartNames[this.config.jsonName] != null) {
        this.config.prettyName = chartNames[this.config.jsonName];
      } else {
        this.config.prettyName = this.config.jsonName;
      }
    }
    
    // correct with and height
    this.config.width  = this.config.width  - this.config.margin.left - this.config.margin.right,
    this.config.height = this.config.height - this.config.margin.top  - this.config.margin.bottom;
    
    // start drawing actual chart
    
    // add ranges
    x = d3.time.scale().range([0, this.config.width]);
    y = d3.scale.linear().range([this.config.height, 0]);

    // add default scale of the axes
    var now = new Date();
    x.domain([new Date(+(now)-(10*1000)), new Date(+(now)+(120*1000))]);
    y.domain([0, 1]);

    xAxis = d3.svg.axis().scale(x)
        // add ticks (axis and vertical line)
        .tickSize(-this.config.height).tickPadding(6).ticks(5).orient("bottom");

    var tickFormatY = d3.format("s"); // add SI-postfix (like 2k instead of 2000)
    yAxis = d3.svg.axis().scale(y)
        // add ticks (axis and vertical line)
        .tickSize(-this.config.width).tickFormat(tickFormatY).tickPadding(6).ticks(5).orient("left");

    line = d3.svg.line()
        .x(function(d) { return x(d.date); })
        .y(function(d) { return y(d.value); });

    zoom = d3.behavior.zoom().x(x)
             .scaleExtent([0.005, 5]) // allow zooming in/out
             .on("zoom", draw);

    svg = d3.select(this.container).append("div")
        .attr("id", this.config.jsonName + "Chart")
        .attr("class", "hidden")
      .append("svg")
        .attr("width", this.config.width + this.config.margin.left + this.config.margin.right)
        .attr("height", this.config.height + this.config.margin.top + this.config.margin.bottom)
      .append("g")
        .attr("transform", "translate(" + this.config.margin.left + "," + this.config.margin.top + ")")
        //.call(zoom) // un-comment to enable on hover dragging and zooming 
        ;

    // needed for zooming and dragging
    var rect = svg.append("rect").attr("width", this.config.width).attr("height", this.config.height);

    // avoid data lines to overlap with axis
    var svgBox = svg.append("svg").attr("width", this.config.width).attr("height", this.config.height)
                    .attr("viewBox", "0 0 " + this.config.width + " " + this.config.height);

    var lines = svgBox.selectAll("g").data(data);
    //lines.attr("transform", function(d) { return "translate("+d.x+","+d.y+")"; });

    //for each array, create a 'g' line container
    aLineContainer = lines.enter().append("g");
    path = aLineContainer.append("path").attr("class", "line");
    path.style("stroke", function(d, i) {
      switch (i) {
        case 0: return "black";
        case 1: return "blue";
        case 2: return "red";
      }
    }).style("stroke-width", function(d, i) {
      if (i == 0) {
        return "3px";
      }
      return "1.5px";
    });

    // add x axis to chart
    svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + this.config.height + ")");

    // add y axis to chart
    svg.append("g")
        .attr("class", "y axis")
      .append("text")
        .attr("y", this.config.height-10)
        .attr("x", this.config.width-6)
        .attr("dy", ".31em")
        .style("text-anchor", "end")
        .text(this.config.prettyName)
        .style("font-size", "14px");;

    // show scatter points and tool tips
    formatTime = d3.time.format("%Y-%m-%d %H:%M:%S");
    div = d3.select("body").append("div").attr("class", "tooltip").style("opacity", 0);
    
    draw();
  };

  
  /**
   * Helper function to update the axis and other stuff
   */
  function draw() {
    drawAnimated(false);
  }
  function drawAnimated(animation) {
    svg.select("g.x.axis").call(xAxis);
    svg.select("g.y.axis").transition().duration(300).ease("linear").call(yAxis); // this should always be animated

    if (animation) {
      svg.selectAll("path.line").transition().duration(300).ease("linear").attr("d", line);
    } else {
      svg.selectAll("path.line").attr("d", line);
    }
    aLineContainer.selectAll("circle.dot").attr("cx", line.x()).attr("cy", line.y());
  }
  
  
  /**
   * Returns the minimum and maximum value of an array
   */
  Array.getMinMax = function(array) {
    var min = Infinity, max = 0;
    var minId = 0, maxId = 0;
    array.forEach(function(v, k) {
      if (v < min) { min = v; minId = k; }
      if (v > max) { max = v; maxId = k; }
    });
    return { "min": { "v":min, "id":minId }, "max": { "v":max, "id":maxId } };
  }
  
  /**
   * Returns the average value of an array
   */
  Array.avg = function(array) {
    var len = array.length;
    if (len == 0) { return 0; }
    return Array.sum(array) / len;
  };
  
  /**
   * Returns the sum of the values of an array
   */
  Array.sum = function(array) {
    var sum = 0;
    array.forEach(function(v) { sum += v; });
    return sum;
  };
  
  /**
   * Allows to transform Dates by adding a specific amount of seconds
   */
  Date.prototype.addMilliseconds = function(seconds) {
    return (+this + seconds);
  };
  
  /**
   * Returns a pretty duration
   */
  Date.prototype.durationPretty = function() {
    var ms = +this;
    var duration  = "";
    var durations = {h:60*60*1000, m:60*1000, s:1000};//, ms:1};
    $.each(durations, function(k, v) {
      if (ms / v >= 1) {
        duration += " " + Math.floor(ms / v) + k;
        ms = ms % v; // store the rest
      }
    });
    return duration;
  };
  
  /**
   * Returns a pretty date time
   */
  Date.prototype.dateTime = function() {
    function pad(i) {
      if (i < 10) {
        return "0" + i;
      }
      return "" + i;
    }
    var d = "";
    d += this.getFullYear() + "/" + pad(this.getMonth()+1) + "/" + pad(this.getDate()) + " ";
    d += pad(this.getHours()) + ":" + pad(this.getMinutes()) + ":" + pad(this.getSeconds());
    return d;
  };
  
  /**
   * Performs all actions needed for the LineChart update
   */
  this.update = function(newData) {
    var currentDate = new Date(newData.timestamp);
    var workerIds   = newData.workerStatistics.workerId;
    newData = this.config.dataCallback(newData);
    
    var shiftRight         = false;
    var lowestXDomain      = x.domain()[0];
    var highestXDomain     = x.domain()[1];
    
    // is current highest date currently being showed?
    if (lowestXDomain <= currentHighestDate && currentHighestDate <= highestXDomain) {
      // if new highest date is out of the domain, we have to shift the chart
      if (highestXDomain < currentDate) {
        shiftRight = true;
      }
    }

    var newMinMax = Array.getMinMax(newData);
    data[0].push({ date:currentDate, value:Array.avg(newData), id:"Average" });
    data[1].push({ date:currentDate, value:newMinMax.min.v, id:"Min = Worker ID: "+workerIds[newMinMax.min.id] });
    data[2].push({ date:currentDate, value:newMinMax.max.v, id:"Max = Worker ID: "+workerIds[newMinMax.max.id] });
    
    path.attr("d", line).attr("transform", null);
    
    // only perform animated transition when needed or we will have problems when dragging/zooming
    d3.transition().ease("linear").duration((shiftRight ? 300 : 0)).each(function() {

      if (shiftRight) {
        // update x domain
        x.domain([new Date(+(lowestXDomain)+(interval)), currentDate]);
        zoom.x(x);
        
        // line transition
        var transformVal = new Date(+(currentDate) - (+(x.domain()[1])-(+(x.domain()[0])) + interval));
        path.transition().ease("linear")
            .attr("transform", "translate(" + x(transformVal) + ")");
      }
      
      // update scatter points
      aLineContainer.selectAll(".dot")
        .data( function(d, i) { return d; } )  // This is the nested data call
        .enter()
          .append("circle")
          .attr("class", "dot")
          .attr("r", 6)
          .on("mouseover",
              function(d) {
                div.transition()        
                   .duration(100)      
                   .style("opacity", .9);      
                div.html(formatTime(d.date) + "<br/>"  + d.value + "<br/>"  + d.id)  
                   .style("left", (d3.event.pageX) + "px")     
                   .style("top", (d3.event.pageY - 28) + "px");    
              })                  
              .on("mouseout",
                  function(d) {       
                    div.transition()        
                       .duration(500)      
                       .style("opacity", 0);   
                  });
      
      // update x domains
      if (newMinMax.max.v > maxYValue) {
        maxYValue = newMinMax.max.v;
      }
      if (maxYValue * 1.05 > y.domain()[1]) {
        y.domain([0, maxYValue * 1.1]);
      }
    
      draw();
    });

    currentHighestDate = currentDate;
  };
  
};