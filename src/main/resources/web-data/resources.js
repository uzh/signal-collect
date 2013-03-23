scc.defaults.resources = {"layout":{
                            "cResourceComputation": "show",
                            "cResourceProblems": "show"
                          },
                          "section": "statistics"
                         };

scc.modules.configuration = function() {
  this.requires = ["executionConfiguration"];
  
  this.onopen = function () {
    scc.order({"provider": "configuration"});
  }
    
  this.onerror = function(e) { }
  this.notready = function() { }

  this.onclose = function() { }
  
  this.onmessage = function(msg) {
    $.each(msg.executionConfiguration, function(k,v) {
      if (v instanceof Array) {
        v = v.join(",");
      }
      $("#resStat" + k).html(v);
    });
    
  }
}

scc.modules.resources = function() {
  this.requires = ["resources"]

  // configure which content box to show in which section
  var resourceBoxes = {
      "statistics": [
        "computationStatBox",
        "infrastructureStatBox",
        "estimationStatBox"
      ],
      "logs"      : [ "logBox" ],
      "detailed"  : [  ], // all charts will be added automatically
      
      "nostart"   : [ 
        "signalCollectTitle",
        "logBox",
        "heartbeatMessagesReceivedChart",
        "messagesSentChart",
        "messagesReceivedChart",
        "signalMessagesReceivedChart",
        "signalOperationsExecuted",
        "collectOperationsExecuted",
        "toCollectSizeChart",
        "toSignalSizeChart",
        "infrastructureTitle",
        "jmx_system_loadChart",
        "jmx_process_timeChart",
        "jmx_process_loadChart",
        "jmx_swap_totalChart",
        "jmx_swap_freeChart",
        "jmx_mem_totalChart",
        "jmx_mem_freeChart",
        "jmx_commited_vmsChart",
        "runtime_mem_freeChart",
        "runtime_mem_maxChart",
        "runtime_mem_totalChart"
      ],
      "noconvergence" : [
        "signalCollectTitle",
        "messagesSentChart",
        "messagesReceivedChart",
      ], 
      "crash"     : [ "jmx_mem_freeChart", "logBox" ],
      "slow" : [
        "infrastructureTitle",
        "jmx_system_loadChart",
        "jmx_process_timeChart",
        "jmx_process_loadChart",
        "jmx_swap_totalChart",
        "jmx_swap_freeChart",
        "jmx_mem_totalChart",
        "jmx_mem_freeChart",
        "jmx_commited_vmsChart",
        "runtime_mem_freeChart",
        "runtime_mem_maxChart",
        "runtime_mem_totalChart"
      ],
  };
  
  // fill detailed view with all the charts we have
  setTimeout(function() {
    $("#resourceBoxes div[id$='Chart']").each(function() {
      resourceBoxes.detailed.push($(this).attr("id"));
    });
  }, 500);
  
  /* panel */
  $("#resources_panel_container label").click(function() {
    console.log("clicked");
    section = $(this).attr("for").split("_")[1];
    show_section(section)
  });
  function show_section(s) {
    console.log("Section: " + s);
    if (s == "") { return; }
    // hide all sections
    $("#resources .structured > div[id^=\"crs\"]").hide();
    // show the appropriate section
    $("#crs_" + s).show();
    show_boxes(s);
    // show change in the panel
    $("#resources_panel_container input").prop("checked", false);
    $("#resources_panel_container input#rs_" + s + "").prop("checked", true);
    // change body class
    $("body").attr("class", s);
    // set section to the hash tag
    var mod = {"resources": {"section": s }}
    scc.settings.set(mod);
  }
  function show_boxes(s) {
    var boxes = "#resourceBoxes";
    // first, hide all of them
    $(boxes + " > div").attr("class", "hidden");
    if (resourceBoxes[s] == null) { return; }
    // then only show the ones that are needed
    resourceBoxes[s].forEach(function(v) {
      var resourceBox = boxes + " > #" + v;
      $(resourceBox).removeClass("hidden");
      $(resourceBox).appendTo(boxes); // change order
    });
  }
  show_section(scc.settings.get().resources.section);
  
  
  
  // Intervals
  var interval = 3000;
  var intervalStatistics = 2*interval; // update statistics less often
  
  // statistics
  $("#resStatInterval").html(intervalStatistics / 1000);
  var statisticsLastUpdated = new Date(0);

  var hasAddedNewCharts = false;

  
  
  
  
  /**
   * Object that encapsulates a complete graph.
   * 
   * This object includes all data that needs to be stored about a graph, as well as the data
   * that is visualized in the graph. This object also offers methods to set up and update a
   * graph.
   */
  var LineChart = function()
  {
//    var private = "a private variable";
//    this.public = "a public variable";
//    var privatefn = function() { ... };
//    this.publicfn = function() { ... };
    
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
    Date.prototype.addSecond = function(seconds) {
      return (+this + seconds);
    };
    
    /**
     * Returns a pretty duration
     */
    Date.prototype.durationPretty = function() {
      var ms = +this;
      var duration  = "";
      var durations = {h:60*60*1000, m:60*1000, s:1000, ms:1};
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

  
  
  
  var sumSubArray = function(data) {
    return data.workerStatistics.messagesSent.map(function(array) {
      return Array.sum(array);
    });
  };
  
  var chartConfig = [
                     {jsonName : "messagesSent", prettyName: "Messages Sent (#)", dataCallback: sumSubArray },
                     {jsonName : "messagesReceived", prettyName : "Messages Received (#)"},
                     {jsonName : "signalMessagesReceived", prettyName : "Signal Messages Received (#)"},
                     {jsonName : "otherMessagesReceived", prettyName : "Other Messages Received (#)"},
                     {jsonName : "requestMessagesReceived", prettyName : "Request Messages Received (#)"},
                     {jsonName : "continueMessagesReceived", prettyName : "Continue Messages Received (#)"},
                     {jsonName : "bulkSignalMessagesReceived", prettyName : "Bulk Signal Messages Received (#)"},
                     {jsonName : "heartbeatMessagesReceived", prettyName : "Heartbeat Messages Received (#)"},
                     {jsonName : "receiveTimeoutMessagesReceived", prettyName : "Timeout Messages Received (#)"},
                     {jsonName : "outgoingEdgesAdded", prettyName : "Outgoing Edges Added (#)"},
                     {jsonName : "outgoingEdgesRemoved", prettyName : "Outgoing Edges Removed (#)"},
                     {jsonName : "numberOfOutgoingEdges", prettyName : "Number of Outgoing Edges (#)"},
                     {jsonName : "verticesRemoved", prettyName : "Vertices Removed (#)"},
                     {jsonName : "verticesAdded", prettyName : "Vertices Added (#)"},
                     {jsonName : "numberOfVertices", prettyName : "Number of Vertices (#)"},
                     {jsonName : "signalOperationsExecuted", prettyName : "Signal Operations Executed (#)"},
                     {jsonName : "collectOperationsExecuted", prettyName : "Collect Operations Executed (#)"},
                     {jsonName : "toCollectSize", prettyName : "To Collect Size (#)"},
                     {jsonName : "toSignalSize", prettyName : "To Signal Size (#)"},
                     {jsonName : "workerId", prettyName : "Worker ID"},
                     {jsonName : "runtime_cores", prettyName : "Available Processors (#)"},
                     {jsonName : "jmx_system_load", prettyName : "System CPU Load (%)"},
                     {jsonName : "jmx_process_time", prettyName : "Process CPU Time (NS)"},
                     {jsonName : "jmx_process_load", prettyName : "Process CPU Load (%)"},
                     {jsonName : "jmx_swap_free", prettyName : "Free Swap Space Size (B)"},
                     {jsonName : "jmx_swap_total", prettyName : "Total Swap Space Size (B)"},
                     {jsonName : "jmx_mem_total", prettyName : "Total Physical Memory Size (B)"},
                     {jsonName : "jmx_mem_free", prettyName : "Free Physical Memory Size (B)"},
                     {jsonName : "jmx_committed_vms", prettyName : "Committed Virtual Memory Size (B)"},
                     {jsonName : "runtime_mem_max", prettyName : "Max Memory (B)"},
                     {jsonName : "runtime_mem_free", prettyName: "Free Memory (B)"},
                     {jsonName : "runtime_mem_total", prettyName : "Total Memory (B)"},
                     {jsonName : "os", skip: true },
                    ];
  var lineCharts = [];
  
  var ChartsCreate = function(config) {
    var lineChart = new LineChart();
    lineChart.container = "#crs_detailed .chartContainer";
    lineChart.container = "#resourceBoxes";
    lineChart.setup(config);
    lineCharts.push(lineChart);    
  }
  
  chartConfig.forEach(function(config) {
    if (!config.skip) {
      ChartsCreate(config);
    }
  });
  
  // update boxes (needed to show the charts on reload)
  setTimeout(function() {
    show_boxes(scc.settings.get().resources.section);
  }, 600);
  
  
 
  
  
  // event handler
  
  this.onopen = function () {
    scc.order({"provider": "resources"})
  }
    
  this.onerror = function(e) { }
  this.notready = function() { }

  var ChartsContains = function(key) {
    var found = false;
    chartConfig.forEach(function(c) {
      if (c.jsonName == key) {
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
        if (msg.workerStatistics.hasOwnProperty(key) && !ChartsContains(key)) {
          ChartsCreate({jsonName : key});
        }
      }
      hasAddedNewCharts = true;
    }
    
    // update all graphs
    lineCharts.forEach(function(g) { g.update(msg); });
    
    // update statistics
    if (statisticsLastUpdated.addSecond(intervalStatistics) <= msg.timestamp) {
      var resStatStartTime = $("#resStatStartTime");
      if (resStatStartTime.html() == "?") {
        resStatStartTime.html(new Date(msg.timestamp).dateTime());
      }
      $("#resStatRunTime").html(new Date(msg.timestamp-(new Date(resStatStartTime.html()))).durationPretty());
      $("#resStatWorkers").html(msg.workerStatistics.workerId.length);
      statisticsLastUpdated = new Date(msg.timestamp);
    }
    
    scc.order({"provider": "resources"}, interval)
  }

  this.onclose = function() { }

}

