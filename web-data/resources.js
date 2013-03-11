scc.defaults.resources = {"layout":{
                            "cResourceViews":"show"},
                          "section": "overview"
                         }

scc.modules.resources = function() {
  this.requires = ["resources"]

  /* panel */
  $("#resources_panel_container label").click(function() {
    console.log("clicked");
    section = $(this).attr("for").split("_")[1];
    show_section(section)
  });
  function show_section(s) {
    if (s == "") { return; }
    // hide all sections
    $("#resources .structured > div").hide();
    // show the appropriate section
    $("#crs_" + s).show();
    // show change in the panel
    $(".sectionLink").removeClass("active");
    $("#rs_" + s).addClass("active");
    // set section to the hash tag
    var mod = {"resources": {"section": s }}
    scc.settings.set(mod);
  }
  show_section(scc.settings.get().resources.section);
  
  
  
  var interval = 3000;
  
  
  
  
  
  
  var LineChart = function()
  {
//    var private = "a private variable";
//    this.public = "a public variable";
//    var privatefn = function() { ... };
//    this.publicfn = function() { ... };
    
    // default config
    this.config = {
        jsonName     : "",
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
    var svg, xAxis, yAxis, line, aLineContainer, x, y, path, currentHighestDate = 0, maxYValue = 0, zoom;
    
    // variables needed for the tool tips
    var div, formatTime;
    
    
    
    
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
        this.config.prettyName = this.config.jsonName;
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
      x.domain([new Date(+(now)-(10*1000)), new Date(+(now)+(120*1000))]);//new Date(+(now)+(5*1000))]);
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

      svg = d3.select(this.container).append("div").append("svg")
          .attr("width", this.config.width + this.config.margin.left + this.config.margin.right)
          .attr("height", this.config.height + this.config.margin.top + this.config.margin.bottom)
        .append("g")
          .attr("transform", "translate(" + this.config.margin.left + "," + this.config.margin.top + ")")
          .call(zoom);

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
      svg.select("g.x.axis").call(xAxis);
      svg.select("g.y.axis").transition().duration(300).ease("linear").call(yAxis);

      svg.selectAll("path.line").attr("d", line);
      aLineContainer.selectAll("circle.dot").attr("cx", line.x()).attr("cy", line.y());
//      d3.select("#footer span").text("U.S. Commercial Flights, " + x.domain().map(format).join("-"));
    }
    
    
    /**
     * Returns the minimum value and the corresponding index of an array
     */
    Array.min = function(array) {
      var value = Infinity, index = 0;
      array.forEach(function(v, i) {
        if (v < value) {
          value = v;
          index = i;
          if (value == 0) { // we do not have negative values
            return;
          }
        }
      });
      return { value : value, index : index };
    };
    
    /**
     * Returns the maximum value and the corresponding index of an array
     */
    Array.max = function(array) {
      var value = 0, index = 0;
      array.forEach(function(v, i) {
        if (v > value) {
          value = v;
          index = i;
        }
      });
      return { value : value, index : index };
    };
    
    /**
     * Returns the average value of an array
     */
    Array.avg = function(array) {
      var sum = 0, len = array.length;
      if (len == 0) { return 0; }
      array.forEach(function(v) { sum += v; });
      return sum / len;
    };
    
    /**
     * Performs all actions needed for the LineChart update
     */
    this.update = function(newData) {
      var currentDate = new Date(newData.timestamp);
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

      var newMin = Array.min(newData);
      var newMax = Array.max(newData);
      data[0].push({ date : currentDate, value : Array.avg(newData), id : "Average" });
      data[1].push({ date : currentDate, value : newMin.value,       id : "min" });
      data[2].push({ date : currentDate, value : newMax.value,       id : "max" });
      
      path.attr("d", line).attr("transform", null);
      
      // only perform animated transition when needed or we will have problems when dragging/zooming
      d3.transition().ease("linear").duration((shiftRight ? interval : 0)).each(function() {

        if (shiftRight) {
          // update x domain
          x.domain([new Date(+(lowestXDomain)+(interval)), currentDate]);
          zoom.x(x);
          
          // line transition
          var transformVal = new Date(+(currentDate) - (+(x.domain()[1])-(+(x.domain()[0])) + interval));
          path.transition()
              .ease("linear")
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
        if (newMax.value > maxYValue) {
          maxYValue = newMax.value;
        }
        if (maxYValue * 1.05 > y.domain()[1]) {
          y.domain([0, maxYValue * 1.1]);
        }
      
        draw();
      });

      currentHighestDate = currentDate;
    };
    
  };

//  var foo = new FooClass();
//  foo.public = "bar";
//  foo.publicfn();
  
  var chartConfig = [
                     {jsonName : "signalMessagesReceived"},
                     {jsonName : "messagesReceived"},
                     {jsonName : "outgoingEdgesAdded"},
                     {jsonName : "jmx_swap_total"},
                     {jsonName : "runtime_mem_free", prettyName: "Free Runtime Memory (B)"},
                    ];
  var lineCharts = [];
  
  var ChartsCreate = function(config) {
    var lineChart = new LineChart();
    lineChart.container = "#crs_detailed .chartContainer";
    lineChart.setup(config);
    lineCharts.push(lineChart);    
  }
  
  chartConfig.forEach(function(config) {
    ChartsCreate(config);
  });
  
 

  
  
  
  
  
  
  
  
  
  
  
  
  /**
   * Object that encapsulates a complete graph.
   * 
   * This object includes all data that needs to be stored about a graph, as well as the data
   * that is visualized in the graph. This object also offers methods to set up and update a
   * graph.
   */
  var GraphClass = function()
  {
    var data = [];
    
    // all graphs
    var margin, width, height, x, y, xAxis, yAxis, area, graph, circle;

    // stacked graphs
    var z, stack, nest, layers;
    
    // number of values to show in the chart
    var numOfValues = 100;
    
    this.conf = Object.create({
      graphName : "",
      friendlyName : "",
      dataCallback : null,
      stacked : false,
//      size : {
//        width  : 0,
//        height : 0
//      },
    });
    
    function getStackedData() {
      var d = new Date;
      var unixtime = parseInt(d.getTime() / 1000);
      return [
       {key:"Group1",value:Math.random()*40,date:unixtime-3000},
       {key:"Group2",value:Math.random()*40,date:unixtime-3000},
       {key:"Group3",value:Math.random()*40,date:unixtime-3000},
       {key:"Group1",value:Math.random()*40,date:unixtime-2000},
       {key:"Group2",value:Math.random()*40,date:unixtime-2000},
       {key:"Group3",value:Math.random()*40,date:unixtime-2000},
       {key:"Group1",value:Math.random()*40,date:unixtime-1000},
       {key:"Group2",value:Math.random()*40,date:unixtime-1000},
       {key:"Group3",value:Math.random()*40,date:unixtime-1000},
       {key:"Group1",value:Math.random()*40,date:unixtime},
       {key:"Group2",value:Math.random()*40,date:unixtime},
       {key:"Group3",value:Math.random()*40,date:unixtime}
      ];      
    }
    
    this.setup = function() {
      
      margin = {top: 20, right: 20, bottom: 30, left: 50},
      width = 500 - margin.left - margin.right,
      height = 300 - margin.top - margin.bottom;
      
      // configure x axis
      var tickFormatX = d3.time.format("%M:%S"); // show minutes:seconds
      x = d3.time.scale().range([0, width]);
      xAxis = d3.svg.axis().scale(x).orient("bottom")
                    // add ticks (axis and vertical line)
                    .tickSize(-height).tickFormat(tickFormatX).tickPadding(6).ticks(5)
                    ;

      // configure y axis
      var tickFormatY = d3.format("s"); // add SI-postfix (like 2k instead of 2000)
      y = d3.scale.linear().range([height, 0]);
      yAxis = d3.svg.axis().scale(y).orient("left")
                    // add ticks (axis and horizontal line)
                    .tickSize(-width).tickFormat(tickFormatY).tickPadding(6)
                    ;

      graph = d3.select("div#" + this.conf.graphName).append("svg")
                    .attr("width", width + margin.left + margin.right)
                    .attr("height", height + margin.top + margin.bottom)
                    // shifting the graph to the right to make space for the y axis
                    .append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")")
                    ;
      
      if (this.conf.stacked) {
        
        area = d3.svg.area()
                .interpolate("linear")
                .x(function(d) { return x(d.date); })
                .y0(function(d) { return y(d.y0); })
                .y1(function(d) { return y(d.y0 + d.y); });
        
        stack = d3.layout.stack()
                  .offset("zero")
                  .values(function(d) { return d.values; })
                  .x(function(d) { return d.date; })
                  .y(function(d) { return d.value; });

        // TODO Create 0 array first
        var dataStacked = getStackedData();
        nest = d3.nest().key(function(d) { return d.key; });
        layers = stack(nest.entries(dataStacked));
        
        x.domain(d3.extent(dataStacked, function(d) { return d.date; }));
        y.domain([0, d3.max(dataStacked, function(d) { return d.y0 + d.y; })]);
        z = d3.scale.category20c();

      } else {
        
        area = d3.svg.area()
                .x(function(d,i) { return x(d[1]); })
                .y0(height)
                .y1(function(d) { return y(d[0]); })
        //          .y(function(d) { return y(d); })
                .interpolate("linear") // basis or linear
                ;
        
        graph.append("path").attr("d", area(data)).attr("class", "area");
        
      }
    
      
      // draw x axis
      graph.append("g")
           .attr("class", "x axis")
           .attr("transform", "translate(0," + height + ")")
           .call(xAxis)
           .append("text")
           .attr("y", -10)
           .attr("x", width-6)
           .attr("dy", ".31em")
           .style("text-anchor", "end")
           .text(this.conf.friendlyName)
           .style("font-size", "14px");
      
      // draw y axis
      graph.append("g")
           .attr("class", "y axis")
           .call(yAxis);
      
      // add circle for more information
      circle = graph.append("circle").attr("r", 5).attr("display", "none");
      graph.on("mouseover", function() { circle.attr("display", "block"); })
           .on("mousemove", updateCircle)
           .on("mouseout", function() { circle.attr("display", "none"); });
    };
    
    
    
    function updateCircle() {
      console.log("updating");
      
      var xMouse = d3.mouse(this)[0];
      console.log("MouseX=" + xMouse);
      if (xMouse < 0) {
        return;
      }
      
//      var x = Math.min(Math.round(xMouse / (numOfValues-1))-1, numOfValues-1);
      // Normalize: NewValue = (((OldValue - OldMin) * (NewMax - NewMin)) / (OldMax - OldMin)) + NewMin
      var x = Math.round((((xMouse - 0) * (numOfValues-1 - 0)) / (width - 0)) + 0);
      var y;
      var SPACING = 5;
      
      if ( data[x] === undefined ) {
          var lower = x - (x % SPACING);
          var upper = lower + SPACING;
          var between = d3.interpolateNumber(data[lower], data[upper]);
          y = between( (x+0.001 % SPACING) / SPACING );
      } else {
          y = data[x][0];
      }
      
      var yMax = Math.max(d3.max(data, function(d) { return d[0]; }), 1)
//      console.log("yMax = " + yMax);
      var yVal = ((height / 1.1) / yMax) * y; // distance from top
      
      console.log("x=" + x);
      console.log("y=" + y);
      
      circle
      // De-Normalize: NewValue = (((OldValue - OldMin) * (NewMax - NewMin)) / (OldMax - OldMin)) + NewMin
           .attr("cx", (x*width) / (numOfValues-1))
           .attr("cy", height-yVal);
    }
    
    
    
    this.update = function(newData, timestamp) {

      // get new data
      newData = this.conf.dataCallback(newData);
      
      if (this.conf.stacked) {

        newData.forEach(function(d, i) {
          data.push({"key":"group"+i, "value":d, "date":timestamp});
        });

        layers = stack(nest.entries(data));
        
        // update data
        graph.selectAll("path.layer").remove();
        graph.selectAll(".layer").data(layers).enter()
            .append("path")
            .attr("class", "layer")
            .attr("d", function(d) { return area(d.values); })
            .style("fill", function(d, i) { return z(i); });
        
        // update domains
        x.domain(d3.extent(data, function(d) { return d.date; }));
        y.domain([0, d3.max(data, function(d) { return d.y0 + d.y; }) * 1.1]);

        // redraw axis
        d3.select("#" + this.conf.graphName + " g.y.axis").call(yAxis);
        d3.select("#" + this.conf.graphName + " g.x.axis").call(xAxis);
        
        return;
      }
      
      
      // pre-populate data array with 0s so that the graph starts on the right side
      if (data.length < numOfValues) {
        var minus = numOfValues-1; // -1 to not start with zero in the chart
        for (; data.length<numOfValues; minus--) {
          data.push([0, timestamp - (minus*1000)]);
        }
      }

      // delete first element and add a new one at the end
      data.shift();
      var newDataLength = data.push([
                                     newData,// - Math.floor(Math.random()*(newData/15)),
                                     timestamp / 1000 * 1000 // round to whole seconds
                                     ]);
      

      // update domains
      x.domain(d3.extent(data/*.slice(-numOfValues)*/, function(d) { return d[1]; }));
//      x.domain(d3.extent(data, function(d) { return 20; }));
      y.domain([0, Math.max(d3.max(data, function(d) { return d[0]; })*1.1, 1) ]); // 1 is the minimum
      
      // redraw axis
      d3.select("#" + this.conf.graphName + " g.y.axis").call(yAxis);
      d3.select("#" + this.conf.graphName + " g.x.axis").call(xAxis).transition().ease("linear").duration(500);
      
      // add new data
//      graph.selectAll("path").data([data.slice((newDataLength > 20 ? -20 : -newDataLength))]).attr("d", area);
      graph.selectAll("#" + this.conf.graphName + " path").data([data]).attr("d", area);
      
      // update with animation
//      graph.selectAll("#" + this.conf.graphName + " path")
//        .data([data]) // set the new data
//        .attr("transform", "translate(" + x(1) + ")") // set the transform to the right by x(1) pixels (6 for the scale we've set) to hide the new value
//        .attr("d", area) // apply the new data values ... but the new value is hidden at this point off the right of the canvas
//        .transition() // start a transition to bring the new value into view
//        .ease("linear")
//        .duration(interval) // for this demo we want a continual slide so set this to the same as the setInterval amount below
//        .attr("transform", "translate(" + x(0) + ")"); // animate a slide to the left back to x(0) pixels to reveal the new value
      
      $(graph).mousemove();
      
//      try {
//        updateCircle();
//      } catch(e) {
//        // propably no mouseover
//        console.log(e);
//      }
    };
    
  }

  

  // helper function to create a new graph
  function createGraph(graphName, friendlyName, dataCallback, stacked) {
    var graph = new GraphClass();
    graph.conf.graphName = graphName;
    graph.conf.friendlyName = friendlyName;
    graph.conf.dataCallback = dataCallback;
    graph.conf.stacked = stacked;
    return graph;
  }
  
  // stores all graph objects
  var allGraphs = [];
  
  // add some graphs and set them up
  allGraphs.push(createGraph("graphMessagesSent",
                             "Messages Sent (#)",
                             function(newData) { return newData.workerStatistics.messagesSent[0][0]; },
                             false));
  allGraphs.push(createGraph("graphMessagesReceived",
                             "Messages Received (#)",
                             function(newData) { return newData.workerStatistics.messagesReceived[0]; },
                             false));
  allGraphs.push(createGraph("graphOutgoingEdges",
                             "Outgoing Edges (#)",
                             function(newData) { return newData.workerStatistics.numberOfOutgoingEdges[0]; },
                             false));
  allGraphs.push(createGraph("graphToSignalSize",
                             "To Signal Size (#)",
                             function(newData) { return newData.workerStatistics.toSignalSize[0]; },
                             false));
  allGraphs.push(createGraph("graphRequestMessagesReceived",
                             "Request Messages Received (#)",
                             function(newData) { return newData.workerStatistics.requestMessagesReceived[0]; },
                             false));
  allGraphs.push(createGraph("graphSignalOperationsExecuted",
                             "signalOperationsExecuted (#)",
                             function(newData) { return newData.workerStatistics.signalOperationsExecuted[0]; },
                             false));
  allGraphs.push(createGraph("graphRamStacked",
                             "RAM (B)",
                             function(newData) { return newData.workerStatistics.jmx_mem_free; },
                             true));
  allGraphs.forEach(function(g) { g.setup(); });
  
  
  this.onopen = function () {
    scc.order({"provider": "resources"})
  }
    
  this.onerror = function(e) { }

  var hasAddedNewCharts = false;
  
  var ChartsContains = function(key) {
    var found = false;
    lineCharts.forEach(function(c) {
      if (c.config.jsonName == key) {
        found = true;
        return found;
      }
    });
    return found;
  }
  
  this.onmessage = function(msg) {
    
    // check if there is more data in the websockets
    if (!hasAddedNewCharts) {
      for (var key in msg.workerStatistics) {
        if (msg.workerStatistics.hasOwnProperty(key) && key != "messagesSent" && key != "os") {
          if (!ChartsContains(key)) {
            ChartsCreate({jsonName : key});
          }
        }
      }
      hasAddedNewCharts = true;
    }
    
    // update all graphs
    allGraphs.forEach(function(g) { g.update(msg, msg.timestamp); });
    lineCharts.forEach(function(g) { g.update(msg); });
    
    scc.order({"provider": "resources"}, interval)
  }

  this.onclose = function() { }

}

