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

scc.defaults.graph = {"layout": {
                        "cNodeSelection": "show",
                        "cGraphDesign": "show"
                      },
                      "options": {
                        "gs_searchId": "",
                        "gs_topCriterium": "State (Numerical)",
                        "gd_nodeSize": "Node degree",
                        "gd_nodeColor": "Node state",
                        "gd_nodeBorder": "Is Vicinity",
                        "gp_vicinityRadius": "1",
                        "gp_maxVertices": "10",
                        "gp_refreshRate": "5",
                        "gp_drawEdges": "When graph is still"
                      }
};

scc.modules.graph = function() {
  this.requires = ["graph"];
  this.autoRefresh = false;
  var s, svg, force;
  var color = d3.scale.category20();
  var colorCategories = d3.scale.ordinal()
    .domain(["n", "v"])
    .range(["#cc0000", "#00cc00"]);

  var nodes = [];
  var links = [];
  var nodeRefs = {};
  var linkRefs = {};
  var node;
  var link;
  var fadeTimer;

  var orderTemplate = {"provider": "graph"};

  var completeOrder = function(o) { 
    o["vicinityRadius"] = parseInt($("#gp_vicinityRadius").val());
    o["maxVertices"] = parseInt($("#gp_maxVertices").val());
    return o;
  };

  var scale = d3.scale.linear()
    .range([5, 12])
    .clamp(true);

  var normalize = function (s) {
    var n = parseFloat(s.replace(/[^0-9.,]/g, ''));
    return scale(n);
  };

  var nodeDesign = {
    "gd_nodeColor": { "Node state": function(d) { return color(d.state); },
                      "Node id": function(d) { return color(d.id); },
                      "Is Vicinity": function(d) { return colorCategories(d.category); },
                      "All equal": function(d) { return "#17becf"; },
                      "Node degree": function (d) { return color(d.weight); }},
    "gd_nodeBorder": { "Node state": function(d) { return color(d.state); },
                       "Node id": function(d) { return color(d.id); },
                       "All equal": function(d) { return "#9edae5"; },
                       "Is Vicinity": function(d) { return colorCategories(d.category); },
                       "Node degree": function (d) { return color(d.weight); }},
    "gd_nodeSize": { "Node state": function(d) { return normalize(d.state); },
                     "All equal": function(d) { return 5; },
                     "Node degree": function(d) { return scale.copy().domain([1,20])(d.weight); }}
  };

  var nodeColor = nodeDesign["gd_nodeColor"]["Node state"];
  var setNodeColor = function (s) {
    nodeColor = s;
    node.transition().style("fill", s);
  };
  var nodeBorder = nodeDesign["gd_nodeBorder"]["Node id"];
  var setNodeBorder = function (s) {
    nodeBorder = s;
    node.transition().style("stroke", s);
  };
  var nodeSize = nodeDesign["gd_nodeSize"]["Node degree"];
  var setNodeSize = function (s) {
    nodeSize = s;
    node.transition().attr("r", s);
  };

  this.order = function() {
    scc.order(completeOrder(orderTemplate));
  };
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
      if ( e.which == 13 ) { scc.consumers.graph.searchById($("#gs_searchId").val()); }
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

  this.onopen = function() {
    $("#graph_background").text("Loading...").fadeIn();

    svg = d3.select("#graph_canvas").append("svg")
        .attr("width", "100%")
        .attr("height", "100%")
        .call(d3.behavior.zoom().on("zoom", redraw))
        .append('svg:g')
        .append('svg:g');

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
      }
      else {
          $("#node_id").text("--");
          $("#node_state").text("--");
      }
    });

    d3.select("#graph").on("mouseout", function (e) {
      clearTimeout(fadeTimer);
      fadeTimer = setTimeout(function() {
        $("#graph_tooltip").fadeOut(200);
      }, 500);
    });

    force = d3.layout.force()
        .size([$("#content").width(), $("#content").height()])
        .nodes(nodes)
        .links(links)
        .linkDistance(50)
        .charge(-200);

    node = svg.selectAll(".node");
    link = svg.selectAll(".link");

    $.each(scc.settings.get().graph.options, function (key, value) {
      scc.consumers.graph.setGraphDesign(key, value);
    });

    force.on("tick", function() {
      var drawEdges = scc.settings.get().graph.options["gp_drawEdges"];
      if (drawEdges == "Always" || 
         (drawEdges == "When graph is still" && force.alpha() < 0.02)) {
        link.style("display", "block");
        link.attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });
      }
      else {
        link.style("display", "none");
      }

      node.attr("cx", function(d) { return d.x; })
          .attr("cy", function(d) { return d.y; });
    });

    force.start();

    function redraw() {
      svg.attr("transform",
               "translate(" + d3.event.translate + ")"
               + " scale(" + d3.event.scale + ")");
    }

    scc.consumers.graph.order();
  }
   
  this.onmessage = function(j) {
    nodes = force.nodes();
    links = force.links();
    var newNodes = false;

    for (var i = 0; i < nodes.length; i++) {
      nodeRefs[nodes[i].id] = i;
    }
    for (var i = 0; i < links.length; i++) {
      linkRefs[links[i].source + "-" + links[i].target] = i;
    }

    if (j.nodes == undefined) { return; }

    $("#graph_background").fadeOut(100);

    $.each(j.nodes, function(id, data) {
      if (nodeRefs[id] == undefined) {
        nodes.push({"id": id, "state": data.s, "category": data.c, 
                    "ss": data.ss, "cs": data.cs});
        nodeRefs[id] = nodes.length - 1;
        newNodes = true;
      }
      else {
        nodes[nodeRefs[id]].state = data.s;
        nodes[nodeRefs[id]].category = data.c;
      }
    });

    $.each(j.edges, function (source, targets) {
      for (var t = 0; t < targets.length; t++) {
        linkID = source + "-" + targets[t];
        if (linkRefs[linkID] == undefined) {
          links.push({"source": nodes[nodeRefs[source]], 
                      "target": nodes[nodeRefs[targets[t]]],
                      "value": 5});
          linkRefs[linkID] = links.length - 1;
        }
        else { 
          links[linkRefs[linkID]].value = 5;
        }
      }
    });


    if (nodes.length == 0) {
      $("#graph_background")
        .text("There are no nodes to display for the current criteria").fadeIn();
      return;
    }

    if (newNodes) { force.start(); }
    
    link = link.data(force.links(), 
              function (d) { return d.source.id + "-" + d.target.id; });
    link.enter().append("line")
        .attr("class", "link");
    link.exit().remove();
    link.style("stroke-width", 1);

    node = node.data(force.nodes(), 
              function (d) { return d.id; });
    node.enter().append("circle")
        .attr("class", "node")
        .call(force.drag);
    node.exit().remove();
    node.transition(100)
        .style("fill", nodeColor)
        .style("stroke", nodeBorder)
        .attr("r", nodeSize);

    if (scc.consumers.graph.autoRefresh) {
      scc.order(completeOrder(orderTemplate), parseInt($("#gp_refreshRate").val())*1000);
    }

  }

  this.onerror = function(e) { };

  this.notready = function() {
    $("#graph_background").text("Loading...").fadeIn();
  };

  this.onclose = function() {
    this.destroy();
  };
  this.reset = function() {
    this.destroy();
    scc.consumers.graph.onopen();
  };
  this.destroy = function() {
    scc.resetOrders("graph");
    $("#graph_canvas").empty();
    nodes = [];
    links = [];
    nodeRefs = {};
    linkRefs = {};
  };
  this.findExistingNode = function (id) {
    var node = undefined;
    d3.selectAll(".node").each(function () {
      var data = this.__data__;
      if (data.id.indexOf(id) !== -1) {
        node = this;
      }
    });
    return node;
  };
  this.highlightNode = function (id) {
    var node = scc.consumers.graph.findExistingNode(id);
    if (!node) { console.log("node is not present: " + id); return; }
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

  this.loadNodeById = function (id, cb) {
    orderTemplate = {"provider": "graph", 
                     "query": "id", 
                     "id": id};
    scc.order(completeOrder(orderTemplate), 0, cb);
  }

  this.searchById = function (id) {
    if (id == "") {
      $("#gs_searchId").val("Search and hit Enter to execute");
      return;
    }
    var node = scc.consumers.graph.findExistingNode(id);
    if (!node) {
      $("#graph_tooltip").fadeOut(200);
      scc.consumers.graph.loadNodeById(id, function () {
        setTimeout(function () { scc.consumers.graph.highlightNode(id) }, 1500);
      });
    }
    else {
      scc.consumers.graph.highlightNode(id); 
    }
  }

  $("#gs_searchById").click(function (e) {
    e.preventDefault();
    var id = $("#gs_searchId").val();
    scc.consumers.graph.searchById(id);
  });
          
  var searchTop = function (e) {
    e.preventDefault();
    scc.consumers.graph.reset();
    orderTemplate = {"provider": "graph", 
             "query": "top", 
             "topCriterium": $("#gs_topCriterium").val()};
    scc.order(completeOrder(orderTemplate));
    $("button").removeClass("active");
    $("#gs_searchByTop").addClass("active");
    return false;
  };

  $("#gs_searchByTop").click(searchTop);

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

  $("#cGraphDesign select").change(function (e) {
    var property = $(this);
    scc.consumers.graph.setGraphDesign(property.attr("id"), property.val());
  });
  $("#cNodeSelection").find("select,input").keyup(function (e) {
    var property = $(this);
    scc.consumers.graph.setNodeSelection(property.attr("id"), property.val());
  });
  $("#gp_vicinityRadius").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_vicinityRadius": property.val() }}});
    scc.consumers.graph.reset();
    scc.consumers.graph.order();
  });
  $("#gp_maxVertices").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_maxVertices": property.val() }}});
    scc.consumers.graph.reset();
    scc.consumers.graph.order();
  });
  $("#gp_refreshRate").change(function (e) { 
    var property = $(this);
    scc.settings.set({"graph": {"options": {"gp_refreshRate": property.val() }}});
  });
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
