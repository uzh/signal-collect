scc.defaults.graph = {"layout": {
                        "cNodeSelection": "show",
                        "cGraphDesign": "show"
                      },
                      "choices": {
                        "Node Selection": "topk", 
                        "TopK": "degree",
                        "Graph Layout": "forced"
                      },
                      "options": {
                        "gd_nodeSize": "Node degree",
                        "gd_nodeColor": "Node state",
                        "gd_nodeBorder": "Node id"
                      }}

scc.modules.graph = function() {
  this.requires = ["graph"]
  var s, svg, width, height, force;
  var color = d3.scale.category20();

  var nodes = [];
  var links = [];
  var nodeRefs = {};
  var linkRefs = {};
  var node;
  var link;
  var order = {"provider": "graph"};

  var scale = d3.scale.linear()
    .range([5, 12])
    .clamp(true)

  var normalize = function (s) {
    var n = parseFloat(s.replace(/[^0-9.,]/g, ''));
    return scale(n);
  }

  var nodeDesign = {
    "gd_nodeColor": { "Node state": function(d) { return color(d.state); },
                      "Node id": function(d) { return color(d.id); },
                      "Node degree": function (d) { return color(d.weight); }},
    "gd_nodeBorder": { "Node state": function(d) { return color(d.state); },
                       "Node id": function(d) { return color(d.id); },
                       "Node degree": function (d) { return color(d.weight); }},
    "gd_nodeSize": { "Node state": function(d) { return normalize(d.state); },
                     "Node degree": function(d) { return scale.copy().domain([1,20])(d.weight); }}
  }

  var nodeColor = nodeDesign["gd_nodeColor"]["Node state"]
  var setNodeColor = function (s) {
    nodeColor = s;
    node.transition().style("fill", s);
  }
  var nodeBorder = nodeDesign["gd_nodeBorder"]["Node id"]
  var setNodeBorder = function (s) {
    nodeBorder = s;
    node.transition().style("stroke", s);
  }
  var nodeSize = nodeDesign["gd_nodeSize"]["Node degree"]
  var setNodeSize = function (s) {
    nodeSize = s;
    node.transition().attr("r", s);
  }

  this.layout = function() {
    $.each(scc.settings.get().graph.layout, function (key, value) {
      if (value == "show") { $("#" + key).show(); }
    });
    $.each(scc.settings.get().graph.options, function (key, value) {
      $("#" + key).val(value);
    });
  }
  this.layout()

  this.onopen = function() {

    width = $("#content").width()
    height = $("#content").height()

    svg = d3.select("#graph_canvas").append("svg")
        .attr("width", width)
        .attr("height", height)
        .call(d3.behavior.zoom().on("zoom", redraw))
        .append('svg:g')
        .append('svg:g');

    force = d3.layout.force()
        .size([width, height])
        .nodes(nodes)
        .links(links)
        .linkDistance(30)
        .charge(-120)

    node = svg.selectAll(".node");
    link = svg.selectAll(".link");

    $.each(scc.settings.get().graph.options, function (key, value) {
      scc.consumers.graph.setNodeDesign(key, value);
    });

    force.on("tick", function() {
      if (force.alpha() < 0.02) {
        link.style("display", "block")
        link.attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });
      }
      else {
        link.style("display", "none")
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

    scc.order(order)
  }
   
  this.onmessage = function(j) {
    $("#graph_notready").fadeOut();

    nodes = force.nodes();
    links = force.links();
    var newNodes = false;

    for (var i = 0; i < nodes.length; i++) {
      nodeRefs[nodes[i].id] = i;
    }
    for (var i = 0; i < links.length; i++) {
      linkRefs[links[i].source + "-" + links[i].target] = i;
    }

    $.each(j.nodes, function(id, state) {
      if (nodeRefs[id] == undefined) {
        nodes.push({"id": id, "state": state});
        nodeRefs[id] = nodes.length - 1;
        newNodes = true;
      }
      else {
        nodes[nodeRefs[id]].state = state
      }
    });

    $.each(j.edges, function (source, targets) {
      for (var t = 0; t < targets.length; t++) {
        linkID = source + "-" + targets[t]
        if (linkRefs[linkID] == undefined) {
          links.push({"source": nodes[nodeRefs[source]], 
                      "target": nodes[nodeRefs[targets[t]]],
                      "value": 5})
          linkRefs[linkID] = links.length - 1;
        }
        else { 
          links[linkRefs[linkID]].value = 5
        }
      }
    });

    if (newNodes) { force.start(); }
    
    link = link.data(force.links(), 
              function (d) { return d.source.id + "-" + d.target.id; });
    link.enter().append("line")
        .attr("class", "link")
    link.exit().remove();
    link.style("stroke-width", 1);

    node = node.data(force.nodes(), 
              function (d) { return d.id; });
    node.enter().append("circle")
        .attr("class", "node")
        .call(force.drag)
        .style("fill", nodeColor)
        .style("stroke", nodeBorder)
        .attr("r", nodeSize)
    node.exit().remove();

    node.append("title")
        .text(function(d) { return d.id + ": " + d.state; });


    scc.order(order, 1000)

  }

  this.onerror = function(e) { }

  this.notready = function() {
    $("#graph_notready").fadeIn();
  }

  this.onclose = function() {
    this.destroy()
  }

  this.destroy = function() {
    $("#graph_canvas").empty()
    nodes = []
    links = []
    nodeRefs = {};
    linkRefs = {};
  }

  $("#searchById").click(function (e) {
    e.preventDefault();
    scc.consumers.graph.destroy()
    scc.consumers.graph.onopen()
    order = {"provider": "graph", 
             "search": "vicinity", 
             "vicinity": $("#searchId").val()}
    scc.order(order)
    return false;
  });
  $("#searchByTopk").click(function (e) {
    e.preventDefault();
    scc.consumers.graph.destroy()
    scc.consumers.graph.onopen()
    order = {"provider": "graph", 
             "search": "topk", 
             "topk": parseFloat($("#topk").val())}
    scc.order(order)
    return false;
  });

  this.setNodeDesign = function (property, metric) {
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
  }

  $("#cGraphDesign select").change(function (e) {
    var property = $(this);
    scc.consumers.graph.setNodeDesign(property.attr("id"), property.val());
  });

}
