/**
 *  @author Carol Alexandru
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
 */

/**
 * The GraphD3 class is responsible for drawing and modifying the graph on the
 * graph canvas. It provides functionality to add and remove vertices.
 * @constructor
 * @param {object} graphModule the graph module instance using this GraphD3
 */
scc.lib.graph.GraphD3 = function (graphModule) {

  // Object-scope variables
  var graphD3 = this;
  var svg, force;
  var color = d3.scale.category20();
  var colorCategories = d3.scale.ordinal()
    .domain(["n", "v"])
    .range(["#cc0000", "#00cc00"]);
  var scale = d3.scale.linear()
    .range([5, 25])
    .clamp(true);
  var vertices = [];
  var edges = [];
  var vertexRefs = {};
  var edgeRefs = {};
  var svgVertices;
  var svgEdges;
  var gradientDomain = [null,null];
  var zoomLevel = 1;
  var hoveringOverVertex = undefined;
  var fadeTooltipTimeout;
  var hideBackgroundTimeout;
  var vicinityAutoLoadDelay;
  var vertexSequence = 0;
  var vertexSequenceEnd = 0;
  var GSTR = scc.STR["Graph"];
  var signalThreshold = 0.01;
  var collectThreshold = 0.0;
  this.exposedVertexId = undefined;

  /**
   * The VertexStorageAgent provides methods for setting, getting and adding to the
   * local storage which contains an array of vertexIds as strings. It represents
   * the vertices which the user has loaded into the canvas.
   * @constructor
   */
  var VertexStorageAgent = function () {
    // Initialize the localStorage if necessary
    if (localStorage["vertexIds"] == undefined || localStorage["vertexIds"] == "") { 
      localStorage["vertexIds"] = "[]";
    }

    /**
     * Adds the given vertices to the local storage.
     * @param {array<string>} vertexIds - The vertices to be added to the storage
     */
    this.push = function (vertexIds) {
      var stored = this.get()
      $.each(stored, function (key, value) {
        if (vertexIds.indexOf(value) != -1) { vertexIds.splice(key, 1); }
      });
      stored.push.apply(stored, vertexIds)
      localStorage["vertexIds"] = JSON.stringify(stored);
    };

    /**
     * Replaces the existing list stored with the one passed to the function
     * @param {array<string>} vertexIds - The vertices to be stored
     */
    this.save = function () {
      localStorage["vertexIds"] = JSON.stringify(
        $.map(vertices, function (vertex, i) { return vertex.id; })
      );
    };

    /**
     * Loads the list of vertices from the local storage
     */
    this.get = function () {
      return JSON.parse(localStorage["vertexIds"]);
    };
  };
  // Instantiate an agent for us to use
  this.vertexStorage = new VertexStorageAgent();

  /**
   * Returns a d3 scale that that maps the domain passed to the function
   * to a blue-to-red color scale.
   * @param {array<double>} domain - A three-element array containing the
   *     lowest, median and highest values of the input domain
   * @return {object} - The d3 color scale for this input domain
   */
  var colorGradient = function (domain) {
    var scale = d3.scale.linear()
        .domain(domain)
        .range(["blue", "green", "red"]);
    return scale;
  };

  /**
   * Returns a d3 scale that that maps the domain passed to the function
   * to a green-to-red color scale.
   * @param {array<double>} domain - A three-element array containing the
   *     lowest, median and highest values of the input domain
   * @return {object} - The d3 color scale for this input domain
   */
  var sizeGradient = function (domain) {
    var scale = d3.scale.linear()
        .domain(domain)
        .range([4, 10, 35]);
    return scale;
  };

  /**
   * An object containing the necessary transformation functions that can be
   * used to style the graph.
   */
  var vertexDesign = {
    // functions returning a color
    "gd_vertexColor":  {
                      "Vertex type": function(d) { 
                            return color(d.t); },
                      "Vertex state": function(d) { 
                            return colorGradient(gradientDomain)(d.r); },
                      "Vertex id": function(d) { 
                            return color(d.id); },
                      "Latest query": function(d) { 
                            return  d.seq > vertexSequenceEnd?"#00ff00":"#ff0000"; },
                      "All equal": function(d) { 
                            return "#17becf"; },
                      "Outgoing degree": function(d) { 
                            return colorGradient([1, 5, 50])(d.es); },
                      "Signal threshold": function(d) { 
                            return d.ss > signalThreshold?"#00ff00":"#ff0000"; },
                      "Collect threshold": function(d) { 
                            return d.cs > collectThreshold?"#00ff00":"#ff0000"; }
    },
    "gd_vertexBorder": {
                      "Vertex type": function(d) { 
                            return color(d.t); },
                      "Vertex state": function(d) { 
                            return colorGradient(gradientDomain)(d.r); },
                      "Vertex id": function(d) { 
                            return color(d.id); },
                      "Outgoing degree": function(d) { 
                            return colorGradient([1, 5, 50])(d.es); },
                      "Signal threshold": function(d) { 
                            return d.ss > signalThreshold?"#00ff00":"#ff0000"; },
                      "Collect threshold": function(d) { 
                            return d.cs > collectThreshold?"#00ff00":"#ff0000"; },
                      "All equal": function(d) { 
                            return "#9edae5"; },
                      "Latest query": function(d) { 
                            return d.seq > vertexSequenceEnd?"#00ff00":"#ff0000"; }
    },
    // functions returning a radius
    "gd_vertexSize": {
                      "Vertex state": function(d) { 
                            return sizeGradient(gradientDomain)(d.r); },
                      "All equal": function(d) { 
                            return 5; }
    }
  };

  /**
   * Set the color of vertices in the graph. Providing a string, all vertices will
   * have the same color. If a function is provided, it will be run for each
   * vertex individually, resulting in different colors for different vertices.
   * @param {string|function} s - The color or a function returning a color
   */
  this.setVertexColor = function (s) {
    vertexColor = s;
    svgVertices.transition().style("fill", s);
  };
  /**
   * The default vertex color
   */
  var vertexColor = vertexDesign["gd_vertexColor"]["Vertex state"];

  /**
   * Set the color of vertex borders (outlines) in the graph.
   * @see setVertexColor
   * @param {string|function} s - The color or a function returning a color
   */
  this.setVertexBorder = function (s) {
    vertexBorder = s;
    svgVertices.transition().style("stroke", s);
  };
  /**
   * The default vertex border color
   */
  var vertexBorder = vertexDesign["gd_vertexBorder"]["Vertex id"];

  /**
   * Set the radius of vertices in the graph.
   * @see setVertexColor
   * @param {int|function} s - The radius or a function returning a radius
   */
  this.setVertexSize = function (s) {
    vertexSize = s;
    svgVertices.transition().attr("r", s);
  };
  /**
   * The default vertex radius
   */
  var vertexSize = vertexDesign["gd_vertexSize"]["Vertex state"];

  /**
   * Re-apply the current styling options to all vertices. This function is
   * used when vertices have been styled abnormally, for example because they
   * have been selected using the mouse.
   */
  this.resetVertexStyle = function () {
    svgVertices.style("fill", vertexColor)
               .style("stroke", vertexBorder)
               .attr("r", vertexSize);
  };

  /**
   * Choose under what circumstances edges are drawn.
   */
  this.setEdgeDrawing = function (setting) {
    switch (setting) {
        case "Always":
        case "When graph is still":
            svgEdges.attr("class", "edge"); break;
        case "Only on hover":
            svgEdges.attr("class", "edge hiddenOpacity"); break;
    }
  };

  /**
   * Function that is called by the Graph module when a new WebSocket connection
   * is established. Create the SVG element and prepare the graph drawing
   * functionality.
   * @param {Event} e - The event that triggered the call
   */
  this.onopen = function(e) {
    $("#graph_background").text("Loading...");

    // Add an SVG element to the canvas and enable the d3 zoom functionality
    svg = d3.select("#graph_canvas").append("svg")
        .attr("width", "100%")
        .attr("height", "100%")
        .call(d3.behavior.zoom().on("zoom", function () {
          if (zoomLevel == d3.event.scale) {
            svg.attr("transform",
                     "translate(" + d3.event.translate + ")" + 
                     " scale(" + zoomLevel + ")")
          }
          else {
            zoomLevel = d3.event.scale;
            svg.transition().duration(200).attr("transform",
                     "translate(" + d3.event.translate + ")" + 
                     " scale(" + zoomLevel + ")")
          }
        })).append("svg:g");
    // Disable double-click zooming, because we need double clicks to expand
    // the vicinity of a vertex. Zooming remains possible using the mouse wheel.
    d3.select("#graph_canvas > svg").on("dblclick.zoom", null);

    // When double clicking a vertex, load the vicinity of the vertex
    d3.select("#graph").on("dblclick", function (e) {
      if (d3.event.target.tagName == "circle") {
        var target = d3.event.target;
        var data = target.__data__;
        graphModule.order({"provider": "graph",
               "query": "vertexIds",
               "vertexIds": [data.id],
               "vicinityIncoming": ($("#gp_vicinityIncoming").val() == "Yes"),
               "vicinityRadius": parseInt($("#gp_vicinityRadius").val()) 
        });
      }
    });

    /**
     * Fill the tooltip with information from a data object
     * @param {object} data - The data to use.
     */
    var fillTooltip = function (data) {
        $("#vertex_id").text(data.id);
        $("#vertex_type").text(data.t);
        $("#vertex_state").text(data.state);
        $("#vertex_ss").text(data.ss);
        $("#vertex_cs").text(data.cs);
    };

    /**
     * Handler for when the user clicks on a node to load its exposed
     * information.
     * @param {Event} e - The event that triggered the call
     */
    d3.select("#graph").on("click.exposeVertex", function (e) {
      if ($("#exposition").is(":visible")) {
        var target = d3.event.target;
        var data = target.__data__;
        if (target.tagName == "circle") {
          var exposedVertexId = data.id;
          localStorage["exposedVertexId"] = exposedVertexId;
          $("#exposition_background").text("");
          graphModule.expose(data);
        }
      }
    });

    /**
     * Handler for when the user hovers over the graph. Shows a tooltip when
     * hovering over a vertex.
     * @param {Event} e - The event that triggered the call
     */
    d3.select("#graph").on("mousemove", function (e) {
      var coords = d3.mouse(this);
      var target = d3.event.target;
      var data = target.__data__;
      var vertex = $(target);
      var drawEdges = scc.settings.get().graph.options["gp_drawEdges"];
      var tooltip = $("#graph_tooltip");

      // When over a vertex, show the tooltip, highlight its edges and hide all
      // other edges in the graph
      if (target.tagName == "circle") {
        $("#graph_tooltip").css({"left": coords[0]+5 + "px", "top": coords[1]+5 + "px"});
        hoveringOverVertex = data.id; 
        fillTooltip(data);
        clearTimeout(fadeTooltipTimeout);
        tooltip.fadeIn(200);
        svgEdges.attr("class", function(o) {
          if (o.source.id === data.id) { return "edge outgoing"; }
          if (o.target.id === data.id) { return "edge"; }
          return "edge hiddenOpacity";
        });
        if (scc.settings.get().graph.options["gs_autoAddVicinities"] == "Yes") {
          clearTimeout(vicinityAutoLoadDelay);
          var target = d3.event.target;
          var data = target.__data__;
          vicinityAutoLoadDelay = setTimeout(function () {
            graphModule.order({"provider": "graph",
                   "query": "vertexIds",
                   "vertexIds": [data.id],
                   "vicinityIncoming": ($("#gp_vicinityIncoming").val() == "Yes"),
                   "vicinityRadius": parseInt($("#gp_vicinityRadius").val()) 
            });
          }, 150);
        }
      }
      // Otherwise, clear the tooltip and set a timeout to hide it soon
      else {
        hoveringOverVertex = undefined; 
        tooltip.css({"left": coords[0]+5 + "px", "top": coords[1]+5 + "px"});
        fillTooltip({"id": "-", "state": "-", "ss": "-", "cs": "-", "t": "-"});
        clearTimeout(fadeTooltipTimeout);
        clearTimeout(vicinityAutoLoadDelay);
        fadeTooltipTimeout = setTimeout(function() {
          tooltip.fadeOut(200);
        }, 500);
        if (drawEdges == "Only on hover") {
          svgEdges.attr("class", "edge hiddenOpacity");
        }
        else {
          svgEdges.attr("class", "edge");
        }
      }
    });

    // Enable d3's forced directed graph layout
    force = d3.layout.force()
        .size([$("#graph_canvas").width(), $("#graph_canvas").height()])
        .nodes(vertices)
        .links(edges)
        .friction(0.4)
        .linkDistance(30)
        .charge(function (d) {
          var weight = 2;
          if (d.weight > weight) { weight = d.weight; }
          return Math.max(vertexSize(d)*-40*weight, -10000)
        })
    
    svgEdges = svg.append('svg:g').selectAll(".edge");
    svgVertices = svg.append('svg:g').selectAll(".vertex");

    // apply graph design options from the settings
    $.each(scc.settings.get().graph.options, function (key, value) {
      graphD3.setGraphDesign(key, value);
    });

    /**
     * Handler on d3's force layout. This handler may be called several times
     * per second and as such causes the fluid animation to occur. On each 
     * 'tick', the vertex positions need to be updated.
     * @param {Event} e - The event that triggered the call
     */
    force.on("tick", function(e) {
      // The user may choose if the graph edges should be drawn always, never,
      // or only when the graph is moving only very little or not at all. The
      // amount of movement is expressed by d3 through the .alpha() property.
      
      // Update the vertex and edge positions
      svgVertices
          .attr("cx", function(d) { return d.x; })
          .attr("cy", function(d) { return d.y; });
      svgEdges
          .attr("x1", function(d) { return d.source.x; })
          .attr("y1", function(d) { return d.source.y; })
          .attr("x2", function(d) { return d.target.x; })
          .attr("y2", function(d) { return d.target.y; });

      // Add classes to edges depending on options and user interaction
      var drawEdges = scc.settings.get().graph.options["gp_drawEdges"];
      svgEdges.attr("class", function(o) {
        // If the user is hovering over a vertex, only draw edges of that vertex
        if (hoveringOverVertex) {
          if (o.source.id === hoveringOverVertex) { return "edge outgoing"; }
          if (o.target.id === hoveringOverVertex) { return "edge"; }
          return "edge hiddenOpacity";
        }
        // Else draw vertices depending on the drawEdges setting
        else {
          if (drawEdges == "Always" || 
             (drawEdges == "When graph is still" && 
             force.alpha() < 0.05)) { return "edge"; }
          else { return "edge hiddenOpacity"; }
        }
      });
    });
  };

  /**
   * Function that is called by the Graph module when a message is received
   * from the WebSocket. Reads the graph data received and updates the graph
   * accordingly.
   * @param {object} j - The message object received from the server
   */
  this.onmessage = function(j) {
    if (j.provider == "configuration") {
      if (j.executionConfiguration != "unknown") {
        signalThreshold = j.executionConfiguration.signalThreshold
        collectThreshold = j.executionConfiguration.collectThreshold
      }
      return;
    }
    // Keep references to the forced layout data
    var newVertices = false;

    // If the server sent an empty graph, do nothing
    if (j.vertices == undefined) { 
      $("#graph_background").text("There are no vertices matching your request");
      hideBackgroundTimeout = setTimeout(function () {
        if (vertices.length == 0) {
          $("#graph_background").text(GSTR["canvasEmpty"]);
        }
      }, 2000);
      return; 
    }

    // The server sends us two maps, one for vertices and one for edges. In both,
    // the vertices are identifies using strings. For example, given the following
    // vertex map...
    //
    // {"1111":{ <vertex properties> },
    //  "2222":{ <vertex properties> },
    //  "3333":{ <vertex properties> }}
    //
    // and the following edge map...
    //
    // {"1111":["2222","3333"],
    //  "2222":["3333"]}
    //
    // we'd had received a graph that looks like a triangle. 
    // 
    // d3 needs a different representation for the graph. It keeps a list of
    // vertices, not a map, and as such every vertex is only identifiable by its 
    // index in the list. This is not sufficient for our purpose, since we
    // want to be able to update existing vertices without having to redraw the 
    // whole graph but we have no way of mapping the actual vertex id to the 
    // index used by d3. For this reason, we keep a lookup table, 'vertexRefs',
    // which keeps the (vertexId -> index) mapping. The same thing happens for
    // the edges. For example given the graph above, the resulting maps could be:
    //
    // vertexRefs["1111"] = 0
    // vertexRefs["2222"] = 1
    // vertexRefs["3333"] = 2
    // edgeRefs["1111-2222"] = 0
    // edgeRefs["1111-3333"] = 1
    // edgeRefs["2222-3333"] = 3
    //
    // This way, we can later update vertex states, for example by re-assigning
    // to vertices[vertexRefs["1111"]], thereby updating the correct vertex in d3's
    // vertex array.

    // If there's a vertex limit, then check if we need to remove some vertices
    // before there's enough space for the new ones

    var maxVertexCount = parseInt(scc.settings.get().graph.options["gp_maxVertexCount"]);

    vertexSequenceEnd = vertexSequence;
    var newVertexCount = 0;
    var tooManyVertices = 0;

    $.each(j.vertices, function(id, data) {
      newVertexCount += 1;
      // If there are more vertices in the data than we're allowed to draw,
      // then remove the remaining elements from the vertexRefs if necessary
      if (newVertexCount > maxVertexCount) { 
        vertexRefs[id] = undefined;
        tooManyVertices += 1;
        return;
      }
      vertexSequence += 1;
      var radius = data.s.replace(/[^0-9.,]/g, '')
      if (radius == "NaN" || radius == "") { radius = 1; }
      if (isNaN(radius)) { radius = 1; }
      if (vertexRefs[id] == undefined) {
        // The vertex hasn't existed yet. Update d3's vertex array
        vertices.push({"id": id, "state": data.s, "seq": vertexSequence, 
                       "es": data.es, "ss": data.ss, "cs": data.cs,
                       "info": data.info, "r": radius, "t": data.t});
        // Store the index of the vertex in the lookup table
        vertexRefs[id] = vertices.length - 1;
        newVertices = true;
      }
      else {
        // Look up the vertex with this id in d3's vertex array and update it
        vertices[vertexRefs[id]].state = data.s;
        vertices[vertexRefs[id]].seq = vertexSequence;
        vertices[vertexRefs[id]].ss = data.ss;
        vertices[vertexRefs[id]].cs = data.cs;
        vertices[vertexRefs[id]].r = radius;
        vertices[vertexRefs[id]].t = data.t;
        vertices[vertexRefs[id]].info = data.info;
      }
    });
    var verticesToRemove = getOverflowingVertices()
    graphD3.vertexStorage.save();


    // Determine maximum and minimum state to determine color gradient
    var median = d3.median(vertices, function (d) { return d.r });
    var lowest = parseFloat(j.lowestState)
    var highest = parseFloat(j.highestState)
    if (lowest > median) { lowest = median; }
    if (highest < median) { highest = median; }
    gradientDomain = [lowest, median, highest]

    if (j.edges) {
      $.each(j.edges, function (source, targets) {
        for (var t = 0; t < targets.length; t++) {
          var edgeId = source + "-" + targets[t];
          if (edgeRefs[edgeId] == undefined) {
            // The edge hasn't existed yet. Update d3's edge array
            if (vertexRefs[source] != undefined && 
                vertexRefs[targets[t]] != undefined) {
              edges.push({"id": edgeId,
                          "source": vertices[vertexRefs[source]], 
                          "target": vertices[vertexRefs[targets[t]]]});
              // Store the index of the edge in the lookup table
              edgeRefs[edgeId] = edges.length - 1;
            }
          }
          else {
            // One could update d3's edge array here, like with the vertices
          }
        }
      });
    }
    
    if (verticesToRemove.length != 0) {
      graphD3.removeVerticesFromCanvas(verticesToRemove);
    }
    else {
      restart(newVertices);    
    }

    if (tooManyVertices > 0) {
      $("#graph_background").stop().text(
          "The query yields " + (tooManyVertices + vertices.length) + " vertices. Only the first " +
          maxVertexCount  + " are being displayed.");
      hideBackgroundTimeout = setTimeout(function () {
        $("#graph_background").text("Showing " + vertices.length + " vertices");
      }, 4000);
    }

    // Update exposed vertex
    if (typeof graphD3.exposedVertexId == "string") {
      var exposedVertex = svgVertices.filter(function (d, i) {
        return d.id == graphD3.exposedVertexId;
      })[0][0];
      if (exposedVertex != undefined) {
        var data = exposedVertex.__data__;
        $("#exposition_background").text("");
        graphModule.expose(data);
      }
    }

    // Order new graph if autorefresh is enabled
    if (scc.consumers.Graph.autoRefresh) {
      if ($("#gp_refreshRate").val() != "Never") {
        scc.consumers.Graph.update(parseInt($("#gp_refreshRate").val())*1000);
      }
    }
  };

  /**
   * Function that is called by the Graph module when a new WebSocket connection
   * breaks down. 
   * @param {Event} e - The event that triggered the call
   */
  this.onclose = function(e) {
    this.destroy();
    $("#graph_background").text("Connection lost");
  };

  /**
   * Clear graph canvas and then re-initialize graph (calling onopen).
   */
  this.reset = function() {
    this.destroy();
    scc.consumers.Graph.onopen();
  };

  /**
   * Clear graph canvas and cancel all pending orders. Clear data variables.
   */
  this.destroy = function() {
    scc.resetOrders("graph");
    $("#graph_canvas").empty();
    vertices = [];
    edges = [];
    vertexRefs = {};
    edgeRefs = {};
  };

  /**
   * Calculate which are the 'oldest' vertices in the sequence that do not
   * fit onto the canvas given current vertex count limitations.
   * @return {array<object>} the vertices that can be removed
   */
  var getOverflowingVertices = function () {
    var verticesToRemove = [];
    var maxVertexCount = parseInt(scc.settings.get().graph.options["gp_maxVertexCount"]);
    var vertexOverflow = maxVertexCount - vertices.length;
    if (vertexOverflow < 0) {
      var highestSeqToKeep = vertexSequence - maxVertexCount + 1;
      verticesToRemove = $("circle").filter(function (i) {
        if (this.__data__.seq < highestSeqToKeep) { return true; }
      });
    }
    return verticesToRemove;
  };

  /**
   * Set the design of the given vertex property to the given vertex metric.
   * For example, set the size ("gd_vertexSize") to be depending on the vertex
   * vertex state ("Vertex state").
   * @param {string} property - The visual vertex property to change
   * @param {string} metric - The vertex metric on which the visual
   *     representation should depend.
   */
  this.setGraphDesign = function (property, metric) {
    switch (property) {
      case "gd_vertexSize": 
          graphD3.setVertexSize(vertexDesign["gd_vertexSize"][metric]); 
          break;
      case "gd_vertexColor": 
          graphD3.setVertexColor(vertexDesign["gd_vertexColor"][metric]); 
          break;
      case "gd_vertexBorder": 
          graphD3.setVertexBorder(vertexDesign["gd_vertexBorder"][metric]); 
          break;
    }
  };

  /**
   * Find the DOM element of the graph vertex where the id contains the given
   * string. 
   * @param {string} s - The string that should appear in the vertex id
   * @return {DOMElement|undefined} - The vertex with the matching id
   */
  this.findExistingVertices = function (ids) {
    var existingVertices = svgVertices.filter(function (d, i) {
      return ids.indexOf(d.id) != -1
    });
    return existingVertices;
  };

  /**
   * Update the visual graph representation.
   * @param {boolean} graphChanged - Were vertices/edges added or removed?
   */
  var restart = function (graphChanged) {

    // Update the edge data
    svgEdges = svgEdges.data(edges, function(d) { return d.id; });
    svgEdges.enter().append("svg:line")
        .attr("class", "edge hiddenOpacity");
    svgEdges.exit().remove();

    // update the vertex data
    svgVertices = svgVertices.data(vertices, function(d) { return d.id; })
    svgVertices.enter().append("circle")
        .attr("class", "vertex")
        .call(force.drag)
    svgVertices.exit()
      .style("opacity", 1)
      .transition()
      .duration(100)
      .style("opacity", 0)
      .remove();
    svgVertices
      .style("fill", vertexColor)
      .style("stroke", vertexBorder)
      .transition()
      .duration(100)
      .attr("r", vertexSize);

    if (vertices.length == 0) {
      graphD3.resetDefaultStatus()
    }
    else {
      $("#graph_background").text("Showing " + vertices.length + " vertices");
    }

    // If the layout is still running, remember the current cooling value,
    // restart the layouting and reset the cooling value to what it was before.
    // Double it if it's very low already.
    if (force.alpha() > 0 && force.alpha < 0.2) {
      var a = force.alpha()
      force.start();
      force.alpha(a*1.5);
    }
    // Otherwise, give the layouting just a slight nudge.
    else {
      force.start();
      force.alpha(0.04);
    }
  };

  /**
   * Show the default graph status message when the graph is empty
   */
  this.resetDefaultStatus = function () {
    clearTimeout(hideBackgroundTimeout);
    $("#graph_background").show();
    $("#graph_background").text(GSTR["canvasEmpty"]);
  };

   /**
   * Removes the provided vertices from the graph cavas
   * @param {object} vertexList - List of vertex items
   */
  this.removeVerticesFromCanvas = function(vertexList) {
    // Extract vertex Ids from vertex items
    var vertexIds = $.map(vertexList, function (vertex, key) { 
      return vertex.__data__.id;
    });
    // remove the vertices
    for (var i = 0; i < vertices.length; i++) {
      var n = vertices[i];
      if (vertexIds.indexOf(n.id) != -1) {
        vertices.splice(i, 1);
        vertexRefs[n.id] = undefined;
        i--
      }
      else {
        vertexRefs[n.id] = i;
      }
    }
    // remove edges that were connected to these vertices
    for (var i = 0; i < edges.length; i++) {
      var l = edges[i];
      var edgeId = l.source.id + "-" + l.target.id;
      if (vertexIds.indexOf(l.source.id) != -1 ||
          vertexIds.indexOf(l.target.id) != -1) {
        edges.splice(i, 1);
        edgeRefs[l.id] = undefined;
        i--
      }
      else {
        edgeRefs[l.id] = i;
      }
    }
    // persist the new vertex selection and re-activate the graph layout
    graphD3.vertexStorage.save();
    if (vertices.length == 0) {
      this.resetDefaultStatus()
    }

    restart(true); 
  };

  /**
   * Return the list of vertices most recently retrieved from the server.
   */
  this.getLatestVertices = function () {
    var verticesToExpand = $("circle").filter(function (i) {
      if (this.__data__.seq > vertexSequenceEnd) { return true; }
    });
    var vertexIds = $.map(verticesToExpand, function (vertex, key) { 
      return vertex.__data__.id;
    });
    return vertexIds;
  };

  /** 
   * Remove vertices not belonging to the latest query.
   */
  this.removeNonLatestVertices = function() {
    var verticesToRemove = $("circle").filter(function (i) {
      if (this.__data__.seq <= vertexSequenceEnd) { return true; }
    });
    graphD3.removeVerticesFromCanvas(verticesToRemove);
  };

  /** 
   * Remove oldest vertices that don't fit on the canvas because of the vertex
   * count limit.
   */
  this.removeOverflowingVertices = function() {
    var verticesToRemove = getOverflowingVertices()
    graphD3.removeVerticesFromCanvas(verticesToRemove);
  };

  /** 
   * Remove all vertices from the canvas.
   */
  this.removeAllVertices = function() {
    vertices = [];
    graphD3.vertexStorage.save();
    graphD3.reset();
    graphModule.update();
  };

  /**
   * Find and remove vertices that have no edges (orphans).
   */
  this.removeOrphans = function () {
    var verticesWithEdges = {};
    $.map(edges, function (d, i) {
      verticesWithEdges[d.source.id] = 1; 
      verticesWithEdges[d.target.id] = 1; 
    });
    var vertexIds = Object.keys(verticesWithEdges);
    var verticesWithoutEdges = svgVertices.filter(function (d, i) {
      return vertexIds.indexOf(d.id) == -1;
    });
    graphD3.removeVerticesFromCanvas(verticesWithoutEdges[0]);
  };

  /**
   * Search the graph for the given id, and if the vertex is not present, order
   * it from the server
   * @param {string} id - The vertex id
   */
  this.addBySubstring = function (s) {
    graphModule.order({"provider": "graph", 
           "query": "substring", 
           "targetCount": Math.min(100, $("#gp_maxVertexCount").val()),
           "substring": s});
  };

  /**
   * Send an order for the top vertices by the criteria and in the quantity
   * specified by the user.
   * @param {Event} e - The event that triggered the call
   */
  this.addTop = function (e) {
    e.preventDefault();
    graphModule.order({"provider": "graph",
           "query": "top", 
           "signalThreshold": graphD3.signalThreshold,
           "collectThreshold": graphD3.collectThreshold,
           "targetCount": parseInt($("#gp_targetCount").val()),
           "topCriterium": $("#gs_topCriterium").val()
    });
  };

  /**
   * Handler called when the user wants to load the vicinity of all vertices
   * @param {Event} e - The event that triggered the call
   */
  this.addAllVicinities = function (e) { 
    e.preventDefault();
    graphModule.order({"provider": "graph",
           "query":  "vertexIds",
           "vertexIds":  graphD3.vertexStorage.get(),
           "vicinityIncoming": ($("#gp_vicinityIncoming").val() == "Yes"),
           "vicinityRadius": parseInt($("#gp_vicinityRadius").val()) 
    });
  };

  /**
   * Handler called when the user wants to load the vicinity of the vertices
   * that have been most recently queried
   * @param {Event} e - The event that triggered the call
   */
  this.addRecentVicinities = function (e) { 
    e.preventDefault();
    var vertexIds = graphD3.getLatestVertices();
    graphModule.order({"provider": "graph",
           "query":  "vertexIds",
           "vertexIds":  vertexIds,
           "vicinityIncoming": ($("#gp_vicinityIncoming").val() == "Yes"),
           "vicinityRadius": parseInt($("#gp_vicinityRadius").val()) 
    });
  };

}
