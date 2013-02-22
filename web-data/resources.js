scc.modules.resources = function() {
  this.requires = ["resources"]

  var interval = 1000;
  /*
   * TODO
   * - fix animation
   * - Drag timeline
   * - add -1 as no data
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
    
    var margin, width, height, x, y, xAxis, yAxis, area, graph;

    // number of values to show in the chart
    var numOfValues = 20;
    
    this.conf = Object.create({
      graphName : "",
      friendlyName : "",
      dataCallback : null,
//      size : {
//        width  : 0,
//        height : 0
//      },
    });
    
    this.setup = function() {
      
      // D3
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
    };
    
    
    
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
    };
    
  }

  

  // helper function to create a new graph
  function createGraph(graphName, friendlyName, dataCallback) {
    var graph = new GraphClass();
    graph.conf.graphName = graphName;
    graph.conf.friendlyName = friendlyName;
    graph.conf.dataCallback = dataCallback;
    return graph;
  }
  
  // stores all graph objects
  var allGraphs = [];
  
  // add some graphs and set them up
  allGraphs.push(createGraph("graphMessagesSent",
                             "Messages Sent (#)",
                             function(newData) { return newData["messagesSent"][0]; }));
  allGraphs.push(createGraph("graphMessagesReceived",
                             "Messages Received (#)",
                             function(newData) { return newData["messagesReceived"][0]; }));
  allGraphs.push(createGraph("graphOutgoingEdges",
                             "Outgoing Edges (#)",
                             function(newData) { return newData["numberOfOutgoingEdges"][0]; }));
  allGraphs.push(createGraph("graphToSignalSize",
                             "To Signal Size (#)",
                             function(newData) { return newData["toSignalSize"][0]; }));
  allGraphs.push(createGraph("graphRequestMessagesReceived",
                             "Request Messages Received (#)",
                             function(newData) { return newData["requestMessagesReceived"][0]; }));
  allGraphs.push(createGraph("graphSignalOperationsExecuted",
                             "signalOperationsExecuted (#)",
                             function(newData) { return newData["signalOperationsExecuted"][0]; }));
  allGraphs.forEach(function(g) { g.setup(); });
  
  
  this.onopen = function () {
    scc.order("resources")
  }
    
  this.onerror = function(e) { }

  this.onmessage = function(j) {
    
    // update all graphs
    allGraphs.forEach(function(g) { g.update(j.workerStatistics, j.timestamp); });
    
    scc.order("resources", interval);
  }

  this.onclose = function() { }

}

