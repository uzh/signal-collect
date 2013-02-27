scc.defaults.resources = {"layout":{
                            "cResourceViews":"show"},
                          "section": "overview"
                         }

scc.modules.resources = function() {
  this.requires = ["resources"]

  /* panel */
  $(".sectionLink").click(function() {
    section = $(this).attr("id").split("_")[1];
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
  
  
  
  var interval = 1000;
  /*
   * TODO
   * - fix animation
   * - Drag timeline
   * - add hover effects
   */
  
  
  
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

      area = d3.svg.area()
          .x(function(d,i) { return x(d[1]); })
          .y0(height)
          .y1(function(d) { return y(d[0]); })
//          .y(function(d) { return y(d); })
          .interpolate("linear") // basis or linear
          ;

      graph = d3.select("div#" + this.conf.graphName).append("svg")
                    .attr("width", width + margin.left + margin.right)
                    .attr("height", height + margin.top + margin.bottom)
                    // shifting the graph to the right to make space for the y axis
                    .append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")")
                    ;
      
      graph.append("path").attr("d", area(data)).attr("class", "area");
      
      if (this.conf.stacked) {
        z = d3.scale.category20c();
        
        stack = d3.layout.stack()
                  .offset("zero")
                  .values(function(d) { return d.values; })
                  .x(function(d) { return d.date; })
                  .y(function(d) { return d.value; });

        nest = d3.nest().key(function(d) { return d.key; });
        
//        layers = stack(nest.entries(data));
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
          this.conf.dataCallback(newData) - Math.floor(Math.random()*(this.conf.dataCallback(newData)/15)),
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
                             function(newData) { return newData.workerStatistics["messagesSent"][0]; },
                             false));
  allGraphs.push(createGraph("graphMessagesReceived",
                             "Messages Received (#)",
                             function(newData) { return newData.workerStatistics["messagesReceived"][0]; },
                             false));
  allGraphs.push(createGraph("graphOutgoingEdges",
                             "Outgoing Edges (#)",
                             function(newData) { return newData.workerStatistics["numberOfOutgoingEdges"][0]; },
                             false));
  allGraphs.push(createGraph("graphToSignalSize",
                             "To Signal Size (#)",
                             function(newData) { return newData.workerStatistics["toSignalSize"][0]; },
                             false));
  allGraphs.push(createGraph("graphRequestMessagesReceived",
                             "Request Messages Received (#)",
                             function(newData) { return newData.workerStatistics["requestMessagesReceived"][0]; },
                             false));
  allGraphs.push(createGraph("graphSignalOperationsExecuted",
                             "signalOperationsExecuted (#)",
                             function(newData) { return newData.workerStatistics["signalOperationsExecuted"][0]; },
                             false));
  allGraphs.push(createGraph("graphRamStacked",
                             "RAM (B)",
                             function(newData) { return newData.systemStatistics[0]["jmx_mem_free"]; },
                             true));
  allGraphs.forEach(function(g) { g.setup(); });
  
  
  this.onopen = function () {
    scc.order({"provider": "resources"})
  }
    
  this.onerror = function(e) { }

  this.onmessage = function(j) {
    
    // update all graphs
    allGraphs.forEach(function(g) { g.update(j, j.timestamp); });
    
    scc.order({"provider": "resources"}, interval)
  }

  this.onclose = function() { }

}

