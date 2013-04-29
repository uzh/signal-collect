/*
 *  @author Silvan Troxler
 *  
 *  Copyright 2013 University of Zurich
 *      
 *  Licensed below the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed below the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations below the License.
 *  
 */

/**
 * Class that encapsulates a complete graph; it includes all data that needs to 
 * be stored about a graph, as well as the data that is visualized in the graph.
 * This class also offers methods to set up and update a graph.
 * @constructor
 */
var LineChart = function() {
  
  /**
   * Default configuration of a chart. This can be overriden by recreating a
   * similar object with other values.
   * @type {Object}
   */ 
  this.config = {
      jsonName     : "",    // the name that is used in JSON 
      skip         : false, // can be used to skip elements from the websocket (e.g. OS names)
      prettyName   : "",    // name that will be shown on the chart
      dataCallback : null,  // callback to get the data from JSON
      numOfValues  : 100,   // number of values to show without zooming
      margin       : { top: 20, right: 20, bottom: 30, left: 50 },
      width        : 550,   // the width of a chart
      height       : 250,   // the height of a chart
  };
  
  /**
   * The DOM container of the current chart.
   * @type {Object}
   */
  this.container = null;
  
  /**
   * The array which stores all the data (minimum, average, and maximum) that is
   * visualized in a chart.
   * @type {Array}
   */ 
  var data = [ [], [], [] ];
  
  /**
   * The container of the {@code <svg>} element.
   * @type {Object}
   */
  var svg;
  
  /**
   * DOM selector to the x-axis.
   * @type {Object}
   */
  var xAxis;
  
  /**
   * DOM selector to the y-axis.
   * @type {Object}
   */
  var yAxis;
  
  /**
   * The line object which offers accessors to x and y values.
   * @type {Object}
   */
  var line;
  
  /**
   * DOM selector of the lines and scatter points of a chart.
   * @type {Object}
   */
  var aLineContainer;
  
  /**
   * Object holding information about the x-axis including range and domain.
   * @type {Object}
   */
  var x;
  
  /**
   * Object holding information about the y-axis including range and domain.
   * @type {Object}
   */
  var y;
  
  /**
   * Selector for the path that contains all the chart lines.
   * @type {Object}
   */
  var path;
  
  /**
   * Stores the highest date that is being visualized in the chart.
   * @type {Date}
   */
  var currentHighestDate = 0;
  
  /**
   * Contains the the highest Y-value in the chart, this is used to expand the
   * domain when needed.
   * @type {number}
   */
  var maxYValue = 0;
  
  /**
   * Object that contains the D3 zooming object.
   * @type {Object}
   */
  var zoom;
  
  /**
   * Value of the current zoom scale, only changes when zooming in or out.
   * @type {number}
   */
  var zoomScale = 1.0;
  
  // variables needed for the tool tips
  /**
   * Selector for the container of tool-tips over the charts.
   * @type {Object}
   */
  var divTooltip;

  /**
   * Defines the date format used in the tool-tips over the charts.
   * @type {Object}
   */
  var formatTime;
  
  /**
   * Get the number of time points the chart is showing.
   * @return {number} - The number of time points the chart is showing.
   */
  this.dataLength = function() {
    return data[0].length;
  }
  
  /**
   * Get the average value over the whole chart.
   * @return {number} - The average y-value over the whole chart. 
   */
  this.dataAvg = function() {
    return Array.avg($.map(data[0], function (e) { return e.value; }));
  }
  
  /**
   * Get the latest data value of the maximum chart.
   * @return {number} - The latest data value of the maximum chart. 
   */
  this.dataLatest = function() {
    return data[2][data[2].length-1].value;
  }
  
  // sets the zooming level
  /**
   * Sets the zooming level of the chart. There is a limit for zooming in but no
   * limit for zooming out. When the latest data value is currently in the view,
   * it will be shifted to the right to be able to see a big enough part of the 
   * chart after zooming.
   * @param {number} scale - The scale to which the chart zooms. A positive
   *     scale zooms the chart in, a negative scale zooms the chart out.
   */
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
                                 && currentHighestDate <= new Date(highestXDomain.addMilliseconds(scc.conf.resources.intervalCharts)));
    var dataComesAfterWindow  = (highestXDomain <= data[0][0].date);
    var dataComesBeforeWindow = (lowestXDomain >= data[0][this.dataLength()-1].date);
    if (dataComesAfterWindow || currentDateInWindow || dataComesBeforeWindow) {
      this.setMove(0);
    } else {
      draw();
    }
  }
  
  /**
   * Shifts the chart content to the left, to the right, or to the newest data
   * value in the chart.
   * @param {Integer} scale - The scale to which the chart shifts. A positive
   *     scale shifts the chart to the right, a negative scale shifts the chart
   *     to the left, and a scale of zero moves the chart to the newest data
   *     value.
   */
  this.setMove = function(scale) {
    var lowestXDomain  = x.domain()[0];
    var highestXDomain = x.domain()[1];
    var differenceMS = highestXDomain - lowestXDomain;
    var newLowestXDomain, newHighestXDomain;
    
    if (scale == 0) {
      newHighestXDomain = new Date(currentHighestDate.addMilliseconds(scc.conf.resources.intervalCharts));
      newLowestXDomain  = new Date(newHighestXDomain.addMilliseconds(-differenceMS));
    } else {      
      var moveMS       = scale * Math.round(differenceMS / 3);
      newLowestXDomain  = new Date(lowestXDomain.addMilliseconds(moveMS));
      newHighestXDomain = new Date(highestXDomain.addMilliseconds(moveMS));
    }
    
    d3.transition().ease("linear").duration(300).each(function() {
      x.domain([newLowestXDomain, newHighestXDomain]);
      zoom.x(x);
      draw();
    });
  }
  
  
  
  
  /**
   * Creates the chart with the given configuration, sets up all the HTML
   * elements and adds first dummy data.
   * @param {Object} config - Object of the configuration.
   * @see this.config
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
    x.domain([new Date(now.addMilliseconds(-170*1000)), new Date(now.addMilliseconds(10*1000))]);
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
        .style("font-size", "14px");

    // show scatter points and tool tips
    formatTime = d3.time.format("%Y-%m-%d %H:%M:%S");
    divTooltip = d3.select("body").append("div").attr("class", "tooltip").style("opacity", 0);
    
    draw();
  };

  
  /**
   * Helper function to update the axis and the chart content when new data is
   * added or when an animation occurs.
   */
  function draw() {
    svg.select("g.x.axis").call(xAxis);
    svg.select("g.y.axis").transition().duration(300).ease("linear").call(yAxis);
    path.transition().duration(300).ease("linear").attr("d", line);
    aLineContainer.selectAll("circle.dot").attr("cx", line.x()).attr("cy", line.y());
  }
  
  
  /**
   * Creates a helper function which returns the minimum and the maximum value
   * of the given data object.
   * @param {Array} array - The array to find the minimum and maximum value in.
   * @return {Object} - An object containing the value and the id of both, the
   *     minimum and the maximum value of the given array.
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
   * Creates a helper function which returns the average value of the elements
   * contained in the given array.
   * @param {Array} array - The array to find the average value of.
   * @return {number} - The average value of the elements contained in the given
   *     array.
   */
  Array.avg = function(array) {
    var len = array.length;
    if (len == 0) { return 0; }
    return Array.sum(array) / len;
  };
  
  /**
   * Creates a helper function which returns the sum of the elements contained
   * in the given array.
   * @param {Array} array - The array to find the sum.
   * @return {number} - The sum of the elements contained in the given array.
   */
  Array.sum = function(array) {
    var sum = 0;
    array.forEach(function(v) { sum += v; });
    return sum;
  };
  
  /**
   * Extends the Date class with a method to add (or subtract) a given amount of
   * milliseconds.
   * @param {number} ms - The amount of milliseconds to add to the date, or when
   *     {@code ms} is negative, to substract from the date.
   * @return {number} - The transformed date in Unix time in milliseconds.
   */
  Date.prototype.addMilliseconds = function(ms) {
    return (+this + ms);
  };
  
  /**
   * Calculates the duration in the format "1h 2m 3s" given a {@code Date}
   * object.
   * @return {String} - Pretty printed duration in the format "1h 2m 3s". 
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
   * Extends a {@code Date} object with a method to returns a pretty-print date
   * time.
   * @return {String} Pretty printed date time in the format
   *     "YYYY/MM/DD HH:mm:SS".
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
   * Eventhandler to perform all actions needed for the LineChart to update.
   * This stores the new data, adds it to the chart and shifts it the left (if
   * necessary).
   * @param {object} newData - Object containing the new data from JSON.
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
    path.attr("transform", null); // needed to avoid shifting scatter points
    
    // only perform animated transition when needed or we will have problems when dragging/zooming
    d3.transition().ease("linear").duration((shiftRight ? 300 : 0)).each(function() {

      if (shiftRight) {
        // update x domain
        x.domain([new Date(lowestXDomain.addMilliseconds(scc.conf.resources.intervalCharts)), currentDate]);
        zoom.x(x);
        
        // line transition
        var transformVal = new Date(+(currentDate) - (+(x.domain()[1])-(+(x.domain()[0])) + scc.conf.resources.intervalCharts));
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
                divTooltip.transition()        
                   .duration(100)      
                   .style("opacity", .9);      
                divTooltip.html(formatTime(d.date) + "<br/>"  + d.value + "<br/>"  + d.id)  
                   .style("left", (d3.event.pageX) + "px")     
                   .style("top", (d3.event.pageY - 28) + "px");    
              })                  
              .on("mouseout",
                  function(d) {       
                    divTooltip.transition()        
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
