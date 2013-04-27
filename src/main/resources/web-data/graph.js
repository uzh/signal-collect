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
                        "gs_searchId": "",
                        "gs_topCriterium": "Degree",
                        "gd_nodeSize": "Node state",
                        "gd_nodeColor": "Node state",
                        "gd_nodeBorder": "Is Vicinity",
                        "gp_vicinityIncoming": "No",
                        "gp_vicinityRadius": "1",
                        "gp_maxVertices": "10",
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
    .range([5, 12])
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

  var colorGradient =  function (domain) {
    var scale = d3.scale.linear().domain(domain);
    scale.domain([0, 0.5, 1].map(scale.invert));
    scale.range(["green", "yellow", "red"]);
    return scale
  }

  /**
   * Adds the settings selected by the user (such as the vicinity size to be 
   * retrieved) to the given order
   * @param {object} o - The order message to be augmented
   * @return {object} - The order message augmented by the settings
   */
  var addOptionsToOrder = function(o) { 
    o["vicinityRadius"] = parseInt($("#gp_vicinityRadius").val());
    o["vicinityIncoming"] = ($("#gp_vicinityIncoming").val() == "Yes");
    o["maxVertices"] = parseInt($("#gp_maxVertices").val());
    return o;
  };

  /**
   * An object containing the necessary transformation functions that can be
   * used to style the graph.
   */
  var nodeDesign = {
    // functions returning a color
    "gd_nodeColor": { "Node state": function(d) { return colorGradient(gradientDomain)(d.state); },
                      "Node id": function(d) { return color(d.id); },
                      "Is Vicinity": function(d) { return colorCategories(d.category); },
                      "All equal": function(d) { return "#17becf"; },
                      "Node degree": function (d) { return color(d.weight); }},
    "gd_nodeBorder": { "Node state": function(d) { return color(d.state); },
                       "Node id": function(d) { return color(d.id); },
                       "All equal": function(d) { return "#9edae5"; },
                       "Is Vicinity": function(d) { return colorCategories(d.category); },
                       "Node degree": function (d) { return color(d.weight); }},
    // functions returning a radius
    "gd_nodeSize": { "Node state": function(d) { return scale(d.state.replace(/[^0-9.,]/g, '')); },
                     "All equal": function(d) { return 5; }}
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
   * Order graph data using the options set in the GUI. This function may be
   * called by other modules as well, in case they require a graph refresh.
   */
  this.order = function() {
    scc.order(addOptionsToOrder(orderTemplate));
  };

  /**
   * Modify the panel html, setting options and adding dynamic fields
   */
  this.layout = function() {
    for (var i = 5; i<=15; i+=5) {
      $("#gp_maxVertices").append('<option value="' + i + '">' + i + '</option>');
    }
    for (var i = 20; i<=200; i+=20) {
      $("#gp_maxVertices").append('<option value="' + i + '">' + i + '</option>');
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
    $('#gs_searchId').keypress(function(e) {
      if ( e.which == 13 ) { scc.consumers.Graph.searchById($("#gs_searchId").val()); }
    });
    window.addEventListener("keydown", function (e) {
      if (e.ctrlKey && e.keyCode == 70) { 
        $("#gs_searchId").focus();
        $("#gs_searchId").select();
        e.preventDefault();
      }
    });
    $.each(scc.settings.get().graph.layout, function (key, value) {
      if (value == "show") { $("#" + key).show(); }
    });
    $.each(scc.settings.get().graph.options, function (key, value) {
      $("#" + key).val(value);
    });
    if (scc.settings.get().graph.options["gs_searchId"] == "") {
      $("#gs_searchId").val(STR.searchByID);
    }
    if (scc.settings.get().graph.options["gc_nodeId"] == "") {
      $("#gc_nodeId").val(STR.pickNode);
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
    $("#graph_background").text("Loading...").fadeIn();

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
        }))
        .append('svg:g')
        .append('svg:g');

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
      $("#graph_tooltip").css({"left": coords[0]+5 + "px", "top": coords[1]+5 + "px"});
      if (d3.event.target.tagName == "circle") {
          $("#node_id").text(data.id);
          $("#node_state").text(data.state);
          $("#node_ss").text(data.ss);
          $("#node_cs").text(data.cs);
          clearTimeout(fadeTimer);
          var tooltip = $("#graph_tooltip");
          $("#graph_tooltip").fadeIn(200);
          link.attr("class", function(o) {
            if (o.target.id === data.id) { return "link outgoing"; }
            return "link"
          });
      }
      else {
          $("#node_id").text("-");
          $("#node_state").text("-");
          $("#node_ss").text("-");
          $("#node_cs").text("-");
          clearTimeout(fadeTimer);
          fadeTimer = setTimeout(function() {
            $("#graph_tooltip").fadeOut(200);
          }, 500);
          link.attr("class", "link");
      }
    });

    // Enable d3's forced directed graph layout
    force = d3.layout.force()
        .size([$("#content").width(), $("#content").height()])
        .nodes(nodes)
        .links(links)
        .linkDistance(50)
        .charge(-200);
    node = svg.selectAll(".node");
    link = svg.selectAll(".link");

    // apply graph design options
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
      var drawEdges = scc.settings.get().graph.options["gp_drawEdges"];
      if (drawEdges == "Always" || 
         (drawEdges == "When graph is still" && force.alpha() < 0.02)) {
        link.style("display", "block");
        var test = true;
        link.attr("x1", function(d) { if (!test) { test = true; } return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });
      }
      else {
        link.style("display", "none");
      }

      // update the node positions
      node.attr("cx", function(d) { return d.x; })
          .attr("cy", function(d) { return d.y; });
    });

    // enable the forced layout and order the first batch of graph data
    force.start();
    scc.consumers.Graph.order();
  }

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. Reads the graph data received and updates the graph
   * accordingly.
   * @param {object} j - The message object received from the server
   */
  this.onmessage = function(j) {
    // Keep references to the forced layout data
    nodes = force.nodes();
    links = force.links();
    var newNodes = false;

    // If the server sent an empty graph, do nothing
    if (j.nodes == undefined) { return; }

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

    $.each(j.nodes, function(id, data) {
      if (nodeRefs[id] == undefined) {
        // Determine maximum and minimum state to determine color gradient
        var state = parseFloat(data.s);
        if (gradientDomain[0] == null || gradientDomain[0] < state) { 
          gradientDomain[0] = state;
        }
        if (gradientDomain[1] == null || gradientDomain[1] > state) { 
          gradientDomain[1] = state;
        }
        // Update d3's node array
        nodes.push({"id": id, "state": data.s, "category": data.c, 
                    "ss": data.ss, "cs": data.cs});
        // Store the index of the node in the lookup table
        nodeRefs[id] = nodes.length - 1;
        newNodes = true;
      }
      else {
        // Look up the node with this id in d3's node array and update it
        nodes[nodeRefs[id]].state = data.s;
        nodes[nodeRefs[id]].category = data.c;
        nodes[nodeRefs[id]].ss = data.ss;
        nodes[nodeRefs[id]].cs = data.cs;
      }
    });

    $.each(j.edges, function (source, targets) {
      for (var t = 0; t < targets.length; t++) {
        linkID = source + "-" + targets[t];
        if (linkRefs[linkID] == undefined) {
          // Update d3's link array
          links.push({"source": nodes[nodeRefs[source]], 
                      "target": nodes[nodeRefs[targets[t]]]});
          // Store the index of the link in the lookup table
          linkRefs[linkID] = links.length - 1;
        }
        else {
          // One could update d3's link array here, like with the nodes
        }
      }
    });
    
    // Hide or show the graph canvas background text
    $("#graph_background").fadeOut(100);
    if (nodes.length == 0) {
      $("#graph_background")
        .text("There are no nodes to display for the current criteria").fadeIn();
      return;
    }

    
    // Finally, map data to links and nodes
    link = svg.selectAll(".link");
    link = link.data(links)
    link.enter().append("line")
        .attr("class", "link");
    link.exit().remove();

    node = svg.selectAll(".node");
    node = node.data(nodes)
    node.enter().append("circle")
        .attr("class", "node")
        .call(force.drag);
    node.exit().remove();
    node.transition(100)
        .style("fill", nodeColor)
        .style("stroke", nodeBorder)
        .attr("r", nodeSize);

    // (Re-)enable forced layout in case new nodes were added
    if (newNodes) { force.start(); }

    // Order new graph if autorefresh is enabled
    if (scc.consumers.Graph.autoRefresh) {
      scc.order(addOptionsToOrder(orderTemplate), parseInt($("#gp_refreshRate").val())*1000);
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
    $("#graph_background").text("Loading...").fadeIn();
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
   * Find the DOM element of the graph node where the id contains the given
   * string. 
   * @param {string} s - The string that should appear in the node id
   * @return {DOMElement|undefined} - The node with the matching id
   */
  this.findExistingNode = function (s) {
    var node = undefined;
    d3.selectAll(".node").each(function () {
      var data = this.__data__;
      if (data.id.indexOf(s) !== -1) {
        node = this;
      }
    });
    return node;
  };
  /**
   * Visually highlight the node where the id contains the given string.
   * @param {string} s - The string that should appear in the node id
   */
  this.highlightNode = function (s) {
    var node = scc.consumers.Graph.findExistingNode(s);
    if (!node) { console.log("node containing string '" + s + "' is not present"); return; }
    var data = node.__data__;
    var n = d3.select(node);
    var r = n.attr("r");
    var flash = function (count) {
      n.transition().duration(120).ease("linear").attr("r", 20).each("end", function () {
        n.transition().duration(120).ease("linear").attr("r", r).each("end", function () {
          if (count > 0) { flash(count-1) }
        });
      });
    };
    flash(3);
    $("#node_id").text(data.id);
    $("#node_state").text(data.state);
    $("#node_ss").text(data.ss);
    $("#node_cs").text(data.cs);
    $("#graph_tooltip").css({"left": $(node).attr("cx")+5 + "px", 
                             "top": $(node).attr("cy")+5 + "px"});
    $("#graph_tooltip").fadeIn(200);
  };

  /**
   * Order graph data from the server for a single node with the given id.
   * @param {string} id - The node id
   * @param {function} cb - Callback called upon receiving the data
   */
  this.loadNodeById = function (id, cb) {
    orderTemplate = {"provider": "graph", 
                     "query": "id", 
                     "id": id};
    scc.order(addOptionsToOrder(orderTemplate), 0, cb);
  }

  /**
   * Search the graph for the given id, and if the node is not present, order
   * it from the server
   * @param {string} id - The node id
   */
  this.searchById = function (id) {
    if (id == "") {
      $("#gs_searchId").val("Search and hit Enter to execute");
      return;
    }
    var node = scc.consumers.Graph.findExistingNode(id);
    if (!node) {
      $("#graph_tooltip").fadeOut(200);
      scc.consumers.Graph.loadNodeById(id, function () {
        setTimeout(function () { scc.consumers.Graph.highlightNode(id) }, 1500);
      });
    }
    else {
      scc.consumers.Graph.highlightNode(id); 
    }
  }
  $("#gs_searchById").click(function (e) {
    e.preventDefault();
    var id = $("#gs_searchId").val();
    scc.consumers.Graph.searchById(id);
  });
          
  /**
   * Send an order for the top nodes by the criteria and in the quantity
   * specified by the user.
   * @param {Event} e - The event that triggered the call
   */
  var searchTop = function (e) {
    e.preventDefault();
    scc.consumers.Graph.reset();
    orderTemplate = {"provider": "graph", 
             "query": "top", 
             "topCriterium": $("#gs_topCriterium").val()};
    scc.order(addOptionsToOrder(orderTemplate));
    $("button").removeClass("active");
    $("#gs_searchByTop").addClass("active");
    return false;
  };
  $("#gs_searchByTop").click(searchTop);

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

  // TODO: Do we need this functionality?
  this.setNodeSelection = function (property, choice) {
    switch (property) {
      case "gs_searchId": 
          scc.settings.set({"graph": {"options": {"gs_searchId": choice}}});
          break;
      case "gs_topCriterium": 
          scc.settings.set({"graph": {"options": {"gs_topCriterium": choice}}});
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

  // TODO: Do we need this functionality?
  $("#cNodeSelection").find("select,input").keyup(function (e) {
    var property = $(this);
    scc.consumers.Graph.setNodeSelection(property.attr("id"), property.val());
  });

  /**
   * Handler called when the vicinity 'incoming' option changes. Persists the 
   * choice to the settings hash and re-orders the graph with the new setting
   * @param {Event} e - The event that triggered the call
   */
  $("#gp_vicinityIncoming").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_vicinityIncoming": property.val() }}});
    scc.consumers.Graph.reset();
    scc.consumers.Graph.order();
  });


  /**
   * Handler called when the vicinity radius option changes. Persists the 
   * choice to the settings hash and re-orders the graph with the new setting
   * @param {Event} e - The event that triggered the call
   */
  $("#gp_vicinityRadius").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_vicinityRadius": property.val() }}});
    scc.consumers.Graph.reset();
    scc.consumers.Graph.order();
  });

  /**
   * Handler called when the maximum vertices option changes. Persists the 
   * choice to the settings hash and re-orders the graph with the new setting
   * @param {Event} e - The event that triggered the call
   */
  $("#gp_maxVertices").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_maxVertices": property.val() }}});
    scc.consumers.Graph.reset();
    scc.consumers.Graph.order();
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
            link.style("display", "block"); break;
        case "Never":
            link.style("display", "none"); break;
    }
    scc.settings.set({"graph": {"options": {"gp_drawEdges": val }}});
  });
};

