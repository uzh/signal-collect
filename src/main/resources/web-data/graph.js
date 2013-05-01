/*
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
 *  
 */

/**
 * The default settings for the graph module.
 */
scc.defaults.graph = {"layout": {
                        "cNodeSelection": "show",
                        "cGraphDesign": "show"
                      },
                      "options": {
                        "gs_addIds": "",
                        "gs_topCriterium": "Highest degree",
                        "gd_nodeSize": "Node state",
                        "gd_nodeColor": "Node state",
                        "gd_nodeBorder": "Latest query",
                        "gp_vicinityIncoming": "No",
                        "gp_vicinityRadius": "1",
                        "gp_targetCount": "10",
                        "gp_refreshRate": "5",
                        "gp_drawEdges": "When graph is still"
                      }
};

/**
 * The Graph module provides the graph-related menu panel and the graph
 * drawing itself.
 * @constructor
 */
scc.modules.Graph = function() {
  /**
   * Messages from the specified provider are routed to this module by the
   * main module.
   * @type {Array.<string>}
   */
  this.requires = ["graph"];

  /**
   * If this is true, the graph will reload itself periodically. This is the
   * case when the computation is running.
   * @type {boolean}
   */
  this.autoRefresh = false;

  // Object-scope variables
  var s, svg, force;
  var color = d3.scale.category20();
  var colorCategories = d3.scale.ordinal()
    .domain(["n", "v"])
    .range(["#cc0000", "#00cc00"]);
  var scale = d3.scale.linear()
    .range([5, 25])
    .clamp(true);
  var nodes = [];
  var links = [];
  var nodeRefs = {};
  var linkRefs = {};
  var node;
  var link;
  var fadeTimer;
  var orderTemplate = {"provider": "graph"};
  var gradientDomain = [null,null];
  var zoomLevel = 1;
  var hoveringOverNode = undefined;
  var mouseClickCoords = undefined;
  var selectedNodes;
  var pickAction;

  /**
   * The NodeStorageAgent provides method for setting, getting and adding to the
   * local storage which contains an array of nodeIds as strings. It represents
   * the nodes which the user has loaded into the canvas
   * @constructor
   */
  var NodeStorageAgent = function () {
    // Initialize the localStorage if necessary
    if (localStorage["nodeIds"] == undefined || localStorage["nodeIds"] == "") { 
      localStorage["nodeIds"] = "[]";
    }

    /**
     * Adds the given nodes to the local storage.
     * @param {array<string>} nodeIds - The nodes to be added to the storage
     */
    this.push = function (nodeIds) {
      var stored = this.get()
      $.each(stored, function (key, value) {
        if (nodeIds.indexOf(value) != -1) { nodeIds.splice(key, 1); }
      });
      stored.push.apply(stored, nodeIds)
      localStorage["nodeIds"] = JSON.stringify(stored);
    };

    /**
     * Replaces the existing list stored with the one passed to the funciton
     * @param {array<string>} nodeIds - The nodes to be stored
     */
    this.save = function () {
      localStorage["nodeIds"] = JSON.stringify(
        $.map(nodes, function (node, i) { return node.id; })
      );
    };

    /**
     * Loads the list of nodes from the local storage
     */
    this.get = function () {
      return JSON.parse(localStorage["nodeIds"]);
    };
  };
  // Instantiate an agent for us to use
  var nodeStorage = new NodeStorageAgent();

  /**
   * Returns a d3 scale that that maps the domain passed to the function
   * to the range [0, 0.5, 1] so that colors from green to yellow to red
   * can be used to represent the input domain. In essence, it creates a
   * green-to-red scale for any given input domain.
   * @param {array<double>} domain - A two-element array containing the
   *     lowest and highest values of the input domain
   * @return {object} - The d3 color scale for this input domain
   */
  var colorGradient = function (domain) {
    var scale = d3.scale.linear().domain(domain);
    scale.domain([0, 0.5, 1].map(scale.invert));
    scale.range(["green", "yellow", "red"]);
    return scale;
  };

  /**
   * Adds the settings selected by the user (such as the vicinity size to be 
   * retrieved) to the given order
   * @param {object} o - The order message to be augmented
   * @return {object} - The order message augmented by the settings
   */
  var finalize = function(o) { 
    o["provider"] = "graph"; 
    o["vicinityIncoming"] = ($("#gp_vicinityIncoming").val() == "Yes");
    return o;
  };

  /**
   * An object containing the necessary transformation functions that can be
   * used to style the graph.
   */
  var nodeDesign = {
    // functions returning a color
    "gd_nodeColor":  {"Node state": function(d) { 
                            return colorGradient(gradientDomain)(d.state); },
                      "Node id": function(d) { 
                            return color(d.id); },
                      "Latest query": function(d) { 
                            return d.recent == "new"?"#00ff00":"#ff0000"; },
                      "All equal": function(d) { 
                            return "#17becf"; },
                      "Signal threshold": function(d) { 
                            return color(d.ss); },
                      "Collect threshold": function(d) { 
                            return color(d.ss); }
    },
    "gd_nodeBorder": {"Node state": function(d) { 
                            return colorGradient(gradientDomain)(d.state); },
                      "Node id": function(d) { 
                            return color(d.id); },
                      "Signal threshold": function(d) { 
                            return color(d.ss); },
                      "Collect threshold": function(d) { 
                            return color(d.ss); },
                      "All equal": function(d) { 
                            return "#9edae5"; },
                      "Latest query": function(d) { 
                            return d.recent == "new"?"#00ff00":"#ff0000"; }
    },
    // functions returning a radius
    "gd_nodeSize": { "Node state": function(d) { 
                            return scale(d.state.replace(/[^0-9.,]/g, '')); },
                     "All equal": function(d) { 
                            return 5; }
    }
  };

  /**
   * Set the color of nodes in the graph. Providing a string, all nodes will
   * have the same color. If a function is provided, it will be run for each
   * node individually, resulting in different colors for different nodes.
   * @param {string|function} s - The color or a function returning a color
   */
  var setNodeColor = function (s) {
    nodeColor = s;
    node.transition().style("fill", s);
  };
  /**
   * The default node color
   */
  var nodeColor = nodeDesign["gd_nodeColor"]["Node state"];
  
  /**
   * Set the color of node borders (outlines) in the graph.
   * @see setNodeColor
   * @param {string|function} s - The color or a function returning a color
   */
  var setNodeBorder = function (s) {
    nodeBorder = s;
    node.transition().style("stroke", s);
  };
  /**
   * The default node border color
   */
  var nodeBorder = nodeDesign["gd_nodeBorder"]["Node id"];

  /**
   * Set the radius of nodes in the graph.
   * @see setNodeColor
   * @param {int|function} s - The radius or a function returning a radius
   */
  var setNodeSize = function (s) {
    nodeSize = s;
    node.transition().attr("r", s);
  };
  /**
   * The default node radius
   */
  var nodeSize = nodeDesign["gd_nodeSize"]["Node state"];

  /**
   * Wrapper function to do things that allways come with a graph order
   */
  var order = function (order, delay) {
    if (!delay) { delay = 0; }
    scc.order(order, delay)
  };

  /**
   * Order graph data using the options set in the GUI. This function may be
   * called by other modules as well, in case they require a graph refresh.
   */
  this.update = function(delay) {
    if (nodeStorage.get().length > 0) {
      order({"provider": "graph",
             "query": "nodeIds",
             "nodeIds": nodeStorage.get()
      }, delay);
    }
  };

  /**
   * Modify the panel html, setting options and adding dynamic fields
   */
  this.layout = function() {
    for (var i = 5; i<=15; i+=5) {
      $("#gp_targetCount").append('<option value="' + i + '">' + i + '</option>');
    }
    for (var i = 20; i<=200; i+=20) {
      $("#gp_targetCount").append('<option value="' + i + '">' + i + '</option>');
    }
    for (var i = 0; i<=4; i++) {
      $("#gp_vicinityRadius").append('<option value="' + i + '">' + i + '</option>');
    }
    for (var i = 1; i<=10; i+=1) {
      $("#gp_refreshRate").append('<option value="' + i + '">' + i + '</option>');
    }
    for (var i = 15; i<=60; i+=5) {
      $("#gp_refreshRate").append('<option value="' + i + '">' + i + '</option>');
    }
    $('input[type="text"]').click(function(e) { $(this).select(); });
    $('#gs_addIds').keypress(function(e) {
      if ( e.which == 13 ) { 
        e.preventDefault();
        $("#gs_addByIdsButton").addClass("active")
        setTimeout(function () { $("#gs_addByIdsButton").removeClass("active"); }, 100);
        scc.consumers.Graph.addByIds($("#gs_addIds").val());
      }
    });
    $.each(scc.settings.get().graph.layout, function (key, value) {
      if (value == "show") { $("#" + key).show(); }
    });
    $.each(scc.settings.get().graph.options, function (key, value) {
      $("#" + key).val(value);
    });
    if (scc.settings.get().graph.options["gs_addIds"] == "") {
      $("#gs_addIds").val(STR.addByIds);
    }
  }
  this.layout();

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Create the SVG element and prepare the graph drawing
   * functionality.
   * @param {Event} e - The event that triggered the call
   */
  this.onopen = function(e) {
    $("#graph_background").text("Loading...").fadeIn(50);

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
        })).append('svg:g');
    // Disable double-click zooming, because we need double clicks to expand
    // the vicinity of a node. Zooming remains possible using the mouse wheel.
    d3.select("#graph_canvas > svg").on("dblclick.zoom", null);

    // When double clicking a node, load the vicinity of the node
    d3.select("#graph").on("dblclick", function (e) {
      if (d3.event.target.tagName == "circle") {
        var target = d3.event.target;
        var data = target.__data__;
        order({"provider": "graph",
               "query": "nodeIds",
               "nodeIds": [data.id],
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
        $("#node_id").text(data.id);
        $("#node_state").text(data.state);
        $("#node_ss").text(data.ss);
        $("#node_cs").text(data.cs);
    };

    /**
     * Handler for when the user hovers over the graph. Shows a tooltip when
     * hovering over a node.
     * @param {Event} e - The event that triggered the call
     */
    d3.select("#graph").on("mousemove", function (e) {
      var coords = d3.mouse(this);
      var target = d3.event.target;
      var data = target.__data__;
      var node = $(target);
      var drawEdges = scc.settings.get().graph.options["gp_drawEdges"];
      var tooltip = $("#graph_tooltip");

      // When over a node, show the tooltip, highlight its edges and hide all
      // other edges in the graph
      if (d3.event.target.tagName == "circle") {
        $("#graph_tooltip").css({"left": coords[0]+5 + "px", "top": coords[1]+5 + "px"});
        hoveringOverNode = data.id; 
        fillTooltip(data);
        clearTimeout(fadeTimer);
        tooltip.fadeIn(200);
        link.attr("class", function(o) {
          if (o.target.id === data.id) { return "link outgoing"; }
          if (o.source.id === data.id) { return "link"; }
          return "link hiddenOpacity";
        });
      }
      // Otherwise, clear the tooltip and set a timeout to hide it soon
      else {
        hoveringOverNode = undefined; 
        tooltip.css({"left": coords[0]+5 + "px", "top": coords[1]+5 + "px"});
        fillTooltip({"id": "-", "state": "-", "ss": "-", "cs": "-"});
        clearTimeout(fadeTimer);
        fadeTimer = setTimeout(function() {
          tooltip.fadeOut(200);
        }, 500);
        if (drawEdges == "Only on hover") {
          link.attr("class", "link hiddenOpacity");
        }
        else {
          link.attr("class", "link");
        }
      }
    });

    // Enable d3's forced directed graph layout
    force = d3.layout.force()
        .size([$("#content").width(), $("#content").height()])
        .nodes(nodes)
        .links(links)
        .linkDistance(150)
        .charge(-500);
    
    // node and link shall contain the SVG elements of the graph
    node = svg.selectAll(".node");
    link = svg.selectAll(".link");

    // apply graph design options from the settings
    $.each(scc.settings.get().graph.options, function (key, value) {
      scc.consumers.Graph.setGraphDesign(key, value);
    });

    /**
     * Handler on d3's force layout. This handler may be called several times
     * per second and as such causes the fluid animation to occur. On each 
     * 'tick', the node positions need to be updated.
     * @param {Event} e - The event that triggered the call
     */
    force.on("tick", function(e) {
      // The user may choose if the graph edges should be drawn always, never,
      // or only when the graph is moving only very little or not at all. The
      // amount of movement is expressed by d3 through the .alpha() property.
      
      // Update the node and link positions
      node.attr("cx", function(d) { return d.x; })
          .attr("cy", function(d) { return d.y; });
      link.attr("x1", function(d) { return d.source.x; })
          .attr("y1", function(d) { return d.source.y; })
          .attr("x2", function(d) { return d.target.x; })
          .attr("y2", function(d) { return d.target.y; });

      // Add classes to edges depending on options and user interaction
      var drawEdges = scc.settings.get().graph.options["gp_drawEdges"];
      link.attr("class", function(o) {
        // If the user is hovering over a node, only draw edges of that node
        if (hoveringOverNode) {
          if (o.target.id === hoveringOverNode) { return "link outgoing"; }
          if (o.source.id === hoveringOverNode) { return "link"; }
          return "link hiddenOpacity";
        }
        // Else draw nodes depending on the drawEdges setting
        else {
          if (drawEdges == "Always" || 
             (drawEdges == "When graph is still" && 
             force.alpha() < 0.02)) { return "link"; }
          else { return "link hiddenOpacity"; }
        }
      });
    });

    // enable the forced layout
    restart(true);
  }

  /**
   * Update the visual graph representation.
   * @param {boolean} graphChanged - Were nodes/edges added or removed?
   */
  var restart = function (graphChanged) {

    // Update the link data
    link = link.data(links, function(d) { return d.id; });
    link.enter().append("line")
        .attr("class", "link hiddenOpacity");
    link.exit().remove();

    // update the node data
    node = node.data(nodes, function(d) { return d.id; });
    node.enter().append("circle")
        .attr("class", "node")
        .call(force.drag)
        .on("mousedown.drag", null);
    node.exit().remove();
    node.transition(100)
        .style("fill", nodeColor)
        .style("stroke", nodeBorder)
        .attr("r", nodeSize);

    if (nodes.length == 0) {
      $("#graph_background").text(
          "Canvas is empty: use the tools on the left to add and remove nodes"
      ).fadeIn(50);
    }

    // Edges should be drawn first so that nodes are draw above edges
    svg.selectAll("circle, line").sort(function (a, b) { 
      if (a.source == undefined) { return true }
      else { return false }
    }).order();

    // Restart the forced layout if necessary
    if (graphChanged) {
      force.start();
    }
  };

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. Reads the graph data received and updates the graph
   * accordingly.
   * @param {object} j - The message object received from the server
   */
  this.onmessage = function(j) {
    scc.notBusy();
    $("button").removeClass("disabled");
    // Keep references to the forced layout data
    var newNodes = false;

    // If the server sent an empty graph, do nothing
    if (j.nodes == undefined) { 
      $("#graph_background").text("There are no nodes matching your request").fadeIn(50);
      return; 
    }
    else {
      $("#graph_background").fadeOut(50);
    }

    // The server sends us two maps, one for nodes and one for links. In both,
    // the nodes are identifies using strings. For example, given the following
    // node map...
    //
    // {"1111":{ <node properties> },
    //  "2222":{ <node properties> },
    //  "3333":{ <node properties> }}
    //
    // and the following link map...
    //
    // {"1111":["2222","3333"],
    //  "2222":["3333"]}
    //
    // we'd had received a graph that looks like a triangle. 
    // 
    // d3 needs a different representation for the graph. It keeps a list of
    // nodes, not a map, and as such every node is only identifiable by its 
    // index in the list. This is not sufficient for our purpose, since we
    // want to be able to update existing nodes without having to redraw the 
    // whole graph but we have no way of mapping the actual node id to the 
    // index used by d3. For this reason, we keep a lookup table, 'nodeRefs',
    // which keeps the (nodeId -> index) mapping. The same thing happens for
    // the links. For example given the graph above, the resulting maps could be:
    //
    // nodeRefs["1111"] = 0
    // nodeRefs["2222"] = 1
    // nodeRefs["3333"] = 2
    // linkRefs["1111-2222"] = 0
    // linkRefs["1111-3333"] = 1
    // linkRefs["2222-3333"] = 3
    //
    // This way, we can later update node states, for exmaple by re-assigning
    // to nodes[nodeRefs["1111"]], thereby updating the correct node in d3's
    // node array.

    $.each(nodes, function(id, node) {
      node["recent"] = "old";
    });
    $.each(j.nodes, function(id, data) {
      if (nodeRefs[id] == undefined) {
        // The node hasn't existed yet. Update d3's node array
        nodes.push({"id": id, "state": data.s, "recent": "new", 
                    "ss": data.ss, "cs": data.cs});
        // Store the index of the node in the lookup table
        nodeRefs[id] = nodes.length - 1;
        newNodes = true;
      }
      else {
        // Look up the node with this id in d3's node array and update it
        nodes[nodeRefs[id]].state = data.s;
        nodes[nodeRefs[id]].recent = "new";
        nodes[nodeRefs[id]].ss = data.ss;
        nodes[nodeRefs[id]].cs = data.cs;
      }
    });
    nodeStorage.save();

    // Determine maximum and minimum state to determine color gradient
    gradientDomain = [null,null];
    $.each(nodes, function(id, data) {
      var state = parseFloat(data.state);
      if (gradientDomain[0] == null || gradientDomain[0] < state) { 
        gradientDomain[0] = state;
      }
      if (gradientDomain[1] == null || gradientDomain[1] > state) { 
        gradientDomain[1] = state;
      }
    });

    if (j.edges) {
      $.each(j.edges, function (source, targets) {
        for (var t = 0; t < targets.length; t++) {
          linkId = source + "-" + targets[t];
          if (linkRefs[linkId] == undefined) {
            // The link hasn't existed yet. Update d3's link array
            links.push({"id": linkId,
                        "source": nodes[nodeRefs[source]], 
                        "target": nodes[nodeRefs[targets[t]]]});
            // Store the index of the link in the lookup table
            linkRefs[linkId] = links.length - 1;
          }
          else {
            // One could update d3's link array here, like with the nodes
          }
        }
      });
    }
    
    restart(newNodes);    

    // Order new graph if autorefresh is enabled
    if (scc.consumers.Graph.autoRefresh) {
      scc.consumers.Graph.update(parseInt($("#gp_refreshRate").val())*1000);
    }

  }

  /**
   * Function that is called by the main module when a WebSocket error is
   * encountered. Does nothing.
   * @param {Event} e - The event that triggered the call
   */
  this.onerror = function(e) { };

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Show a message on the graph canvas
   */
  this.notready = function() {
    $("#graph_background").text("Data Provider not ready, retrying...").fadeIn(50);
  };

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. 
   * @param {Event} e - The event that triggered the call
   */
  this.onclose = function(e) {
    this.destroy();
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
    nodes = [];
    links = [];
    nodeRefs = {};
    linkRefs = {};
  };


  /**
   * Take two coordinates and use them as opposite corner of a rectangle.
   * Calculate the position and size of the resulting rectangle.
   * @param {array<int>} cornerA - The [x,y] position of the first corner
   * @param {array<int>} cornerB - The [x,y] position of the second corner
   * @return {array<int>} dimensions - The [x, y, w, h] dimensions
   */
  var cornersToDimensions = function (cornerA, cornerB) {
    var left = Math.min(cornerA[0],cornerB[0]);
    var top = Math.min(cornerA[1],cornerB[1]);
    var width = Math.max(cornerA[0],cornerB[0]) - left;
    var height = Math.max(cornerA[1],cornerB[1]) - top;
    return [left, top, width, height];
  };

  /**
   * d3 handler that initiates a selection using the mouse to draw a rectangle.
   */
  d3.select("#graph_canvas_overlay").on("mousedown.selectNodes", function (e) {
    // Store the positions of where the mouse button was pressed
    mouseClickCoords = d3.mouse(this);
    // Place the selection rectangle at the right spot and show it
    $("#graph_canvas_selection").css({"height": 0, "width": 0});
    $("#graph_canvas_selection").show();
  });

  /**
   * d3 handler that adjusts the selection rectangle when moving the mouse
   */
  d3.select("#graph_canvas_overlay").on("mousemove.selectNodes", function (e) {
    if (mouseClickCoords) {
      // update the width
      var coords = d3.mouse(this);
      var dimensions = cornersToDimensions(coords, mouseClickCoords);
      $("#graph_canvas_selection").css({"left": dimensions[0], 
                                        "top": dimensions[1],
                                        "width": dimensions[2], 
                                        "height": dimensions[3]
      });
    }
  });

  /**
   * d3 handler that finally calculates which nodes fall into the selection
   * and then adjusts the panel buttons depending on the action that follows
   * this selection.
   */
  d3.select("#graph_canvas_overlay").on("mouseup.selectNodes", function (e) {
    // Filter nodes to find the ones inside the selection rectangle
    var coords = d3.mouse(this);
    selectedNodes = $("circle").filter(function (i) {
      var boundingRect = this.getBoundingClientRect()
      if (boundingRect.left-301 > Math.min(coords[0], mouseClickCoords[0]) && 
          boundingRect.bottom > Math.min(coords[1], mouseClickCoords[1]) &&
          boundingRect.right-301 < Math.max(coords[0], mouseClickCoords[0]) && 
          boundingRect.bottom < Math.max(coords[1], mouseClickCoords[1])) { 
        return true;
      }
      return false;
    });

    // Highlight the selected nodes
    selectedNodes.css({"fill": "#00ff00", "stroke": "#bbbbbb"});

    // Modify the panel buttons depending on the current action
    $("button").removeClass("active");
    switch (pickAction) {
      case "remove":
        $("#gd_removeBySelect").addClass("snd");
        $("#gd_removeBySelectRemove").removeClass("hidden");
        break;
      case "addVicinities":
        $("#gs_addVicinitiesBySelect").addClass("snd");
        $("#gs_addVicinitiesBySelectAdd").removeClass("hidden");
        break;
    }

    // Hide the selection rectangle and overlay
    $("#graph_canvas_selection").hide();
    $("#graph_canvas_overlay").fadeOut(100);
    $("#graph_canvas_overlay").removeClass("pickingNodes");
  });

  /**
   * Handler for selecting nodes whose vicinities shall be loaded.
   */
  $("#gs_addVicinitiesBySelect").click(function (e) { 
    e.preventDefault();
    pickAction = "addVicinities";
    // restore styling on all nodes
    node.style("fill", nodeColor)
        .style("stroke", nodeBorder);
    $("button").removeClass("active");
    selectedNodes = undefined;
    if ($("#gs_addVicinitiesBySelect").text() == "Cancel") {
      // modify panel buttons
      $("#gs_addVicinitiesBySelect").text("Select");
      $("#gs_addVicinitiesBySelect").removeClass("snd");
      $("#gs_addVicinitiesBySelectAdd").addClass("hidden");
      $("#graph_canvas_selection").hide();
      $("#graph_canvas_overlay").fadeOut(100);
      $("#graph_canvas_overlay").removeClass("pickingNodes");
    }
    else { // TODO: merge with code from removeBySelect
      $("button").removeClass("snd");
      $("#gd_removeBySelect").text("Select");
      $("#gd_removeBySelectRemove").addClass("hidden");
      $("#graph_canvas_overlay").addClass("pickingNodes");
      $("#graph_canvas_overlay").fadeIn(100);
      $("#gs_addVicinitiesBySelect").addClass("active");
      $("#gs_addVicinitiesBySelect").text("Cancel");
    }
  });

  /**
   * Handler for finally loading the vicinities of the selected nodes.
   */
  $("#gs_addVicinitiesBySelectAdd").click(function (e) { 
    e.preventDefault();
    $("#gs_addVicinitiesBySelect").text("Select");
    $("#gs_addVicinitiesBySelect").removeClass("snd");
    $("#gs_addVicinitiesBySelectAdd").addClass("hidden");
    // extract list of Ids from node list and order data
    var selectedNodeIds = $.map(selectedNodes, function (node, key) { 
      return node.__data__.id;
    });
    order({"provider": "graph",
           "query": "nodeIds",
           "nodeIds": selectedNodeIds,
           "vicinityIncoming": ($("#gp_vicinityIncoming").val() == "Yes"),
           "vicinityRadius": parseInt($("#gp_vicinityRadius").val()) 
    });
  });

  /**
   * Handler for selecting nodes which shall be removed.
   */
  $("#gd_removeBySelect").click(function (e) { 
    e.preventDefault();
    pickAction = "remove";
    // restore styling on all nodes
    node.style("fill", nodeColor)
        .style("stroke", nodeBorder);
    selectedNodes = undefined;
    $("button").removeClass("active");
    if ($("#gd_removeBySelect").text() == "Cancel") {
      $("#gd_removeBySelect").text("Select");
      $("#gd_removeBySelect").removeClass("snd");
      $("#gd_removeBySelectRemove").addClass("hidden");
      $("#graph_canvas_selection").hide();
      $("#graph_canvas_overlay").fadeOut(100);
      $("#graph_canvas_overlay").removeClass("pickingNodes");
    }
    else {
      $("button").removeClass("snd");
      $("#graph_canvas_overlay").addClass("pickingNodes");
      $("#gs_addVicinitiesBySelect").text("Select");
      $("#gs_addVicinitiesBySelectAdd").addClass("hidden");
      $("#graph_canvas_overlay").fadeIn(100);
      $("#gd_removeBySelect").addClass("active");
      $("#gd_removeBySelect").text("Cancel");
    }
  });

  /**
   * Handler for finally removing the selected nodes.
   */
  $("#gd_removeBySelectRemove").click(function (e) { 
    e.preventDefault();
    $("#gd_removeBySelect").text("Select");
    $("#gd_removeBySelect").removeClass("snd");
    $("#gd_removeBySelectRemove").addClass("hidden");
    removeNodesFromCanvas(selectedNodes);
  });

  /**
   * Handler called when the user clicks on button to remove all nodes
   * @param {Event} e - The event that triggered the call
   */
  $("#gd_removeOrphans").click(function (e) { 
    e.preventDefault();
    var nodesWithEdges = {};
    $.map(links, function (d, i) {
      nodesWithEdges[d.source.id] = 1; 
      nodesWithEdges[d.target.id] = 1; 
    });
    var nodeIds = Object.keys(nodesWithEdges);
    var nodesWithoutEdges = node.filter(function (d, i) {
      return nodeIds.indexOf(d.id) == -1;
    });
    removeNodesFromCanvas(nodesWithoutEdges[0]);
  });

  /**
   * Find the DOM element of the graph node where the id contains the given
   * string. 
   * @param {string} s - The string that should appear in the node id
   * @return {DOMElement|undefined} - The node with the matching id
   */
  this.findExistingNodes = function (ids) {
    var existingNodes = node.filter(function (d, i) {
      return ids.indexOf(d.id) != -1
    });
    return existingNodes;
  };

  /**
   * Search the graph for the given id, and if the node is not present, order
   * it from the server
   * @param {string} id - The node id
   */
  this.addByIds = function (ids) {
    if (ids == "") {
      $("#gs_addIds").val("Search and hit Enter to execute");
      return;
    }
    order({"provider": "graph", 
           "query": "nodeIds", 
           "nodeIds": ids});
  }

  $("#gs_addByIdsButton").click(function (e) {
    e.preventDefault();
    var ids = $("#gs_addIds").val();
    scc.consumers.Graph.addById(ids);
  });
          
  /**
   * Send an order for the top nodes by the criteria and in the quantity
   * specified by the user.
   * @param {Event} e - The event that triggered the call
   */
  var addTop = function (e) {
    e.preventDefault();
    scc.busy();
    order({"provider": "graph",
           "query": "top", 
           "targetCount": parseInt($("#gp_targetCount").val()),
           "topCriterium": $("#gs_topCriterium").val()
    });
    $("button").addClass("disabled");
  };
  $("#gs_addByTop").click(addTop);

  /**
   * Set the design of the given node property to the given node metric.
   * For example, set the size ("gd_nodeSize") to be depending on the node
   * node state ("Node state").
   * @param {string} property - The visual node property to change
   * @param {string} metric - The node metric on which the visual
   *     representation should depend.
   */
  this.setGraphDesign = function (property, metric) {
    switch (property) {
      case "gd_nodeSize": 
          setNodeSize(nodeDesign["gd_nodeSize"][metric]); 
          scc.settings.set({"graph": {"options": {"gd_nodeSize": metric}}});
          break;
      case "gd_nodeColor": 
          setNodeColor(nodeDesign["gd_nodeColor"][metric]); 
          scc.settings.set({"graph": {"options": {"gd_nodeColor": metric}}});
          break;
      case "gd_nodeBorder": 
          setNodeBorder(nodeDesign["gd_nodeBorder"][metric]); 
          scc.settings.set({"graph": {"options": {"gd_nodeBorder": metric}}});
          break;
    }
  };

  /**
   * Handler called when a 'select' HTML element is changed. The choice is
   * presisted to the settings hash.
   * @param {Event} e - The event that triggered the call
   */
  $("#cGraphDesign select").change(function (e) {
    var property = $(this);
    scc.consumers.Graph.setGraphDesign(property.attr("id"), property.val());
  });

  /**
   * Handler called when the user wants to load the vicinity of all nodes
   * @param {Event} e - The event that triggered the call
   */
  $("#gs_addAllVicinities").click(function (e) { 
    e.preventDefault();
    order({"provider": "graph",
           "query":  "nodeIds",
           "nodeIds":  nodeStorage.get(),
           "vicinityIncoming": ($("#gp_vicinityIncoming").val() == "Yes"),
           "vicinityRadius": parseInt($("#gp_vicinityRadius").val()) 
    });
  });

  /**
   * Removes the provided nodes from the graph cavas
   * @param {object} nodeList - List of node items
   */
  var removeNodesFromCanvas = function(nodeList) {
    // Extract node Ids from node items
    scc.resetOrders("graph");
    var nodeIds = $.map(nodeList, function (node, key) { 
      return node.__data__.id;
    });
    // remove the nodes
    for (var i = 0; i < nodes.length; i++) {
      var n = nodes[i];
      if (nodeIds.indexOf(n.id) != -1) {
        nodes.splice(i, 1);
        nodeRefs[n.id] = undefined;
        i--
      }
      else {
        nodeRefs[n.id] = i;
      }
    }
    // remove links that were connected to these nodes
    for (var i = 0; i < links.length; i++) {
      var l = links[i];
      linkId = l.source.id + "-" + l.target.id;
      if (nodeIds.indexOf(l.source.id) != -1 ||
          nodeIds.indexOf(l.target.id) != -1) {
        links.splice(i, 1);
        linkRefs[l.id] = undefined;
        i--
      }
      else {
        linkRefs[l.id] = i;
      }
    }
    // persist the new node selection and re-activate the graph layout
    nodeStorage.save();
    restart(true);
    scc.consumers.Graph.update();
  };

  /**
   * Handler called when the user clicks on button to remove all nodes
   * @param {Event} e - The event that triggered the call
   */
  $("#gd_removeAll").click(function (e) { 
    e.preventDefault();
    nodes = [];
    nodeStorage.save();
    scc.consumers.Graph.reset();
    scc.consumers.Graph.update();
  });

   /**
    * Handler called when the vicinity 'incoming' option changes. Persists the 
    * choice to the settings hash and re-orders the graph with the new setting
    * @param {Event} e - The event that triggered the call
    */
  $("#gp_vicinityIncoming").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_vicinityIncoming": property.val() }}});
   });

  /**
   * Handler called when the vicinity radius option changes. Persists the 
   * choice to the settings hash and re-orders the graph with the new setting
   * @param {Event} e - The event that triggered the call
   */
  $("#gp_vicinityRadius").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_vicinityRadius": property.val() }}});
  });

  /**
   * Handler called when the maximum vertices option changes. Persists the 
   * choice to the settings hash and re-orders the graph with the new setting
   * @param {Event} e - The event that triggered the call
   */
  $("#gp_targetCount").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_targetCount": property.val() }}});
  });

  /**
   * Handler called when the refresh rate option changes. Persists the choice 
   * to the settings hash.
   * @param {Event} e - The event that triggered the call
   */
  $("#gp_refreshRate").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_refreshRate": property.val() }}});
  });

  /**
   * Handler called when the 'draw edges' option changes. Persists the choice 
   * to the settings hash and updates the representation (shows or hides the
   * edges)
   * @param {Event} e - The event that triggered the call
   */
  $("#gp_drawEdges").change(function (e) { 
    var val = $(this).val();
    switch (val) {
        case "Always":
        case "When graph is still":
            link.attr("class", "link"); break;
        case "Only on hover":
            link.attr("class", "link hiddenOpacity"); break;
    }
    scc.settings.set({"graph": {"options": {"gp_drawEdges": val }}});
  });
};

