scc.defaults.graph = {"layout": {
                        "cNodeSelection": "show",
                        "cGraphLayout": "show"
                      },
                      "choices": {
                        "Node Selection": "topk", 
                        "TopK": "degree",
                        "Graph Layout": "forced"
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
  var query;

  this.layout = function() {
    $.each(scc.settings.get().graph.layout, function (key, value) {
      if (value == "show") { $("#" + key).show(); }
    })
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

    force.on("tick", function() {
      /*link.attr("x1", function(d) { return d.source.x; })
          .attr("y1", function(d) { return d.source.y; })
          .attr("x2", function(d) { return d.target.x; })
          .attr("y2", function(d) { return d.target.y; });*/

      node.attr("cx", function(d) { return d.x; })
          .attr("cy", function(d) { return d.y; });
    });

    force.start();

    function redraw() {
      svg.attr("transform",
               "translate(" + d3.event.translate + ")"
               + " scale(" + d3.event.scale + ")");
    }

    scc.order({"provider": "graph"})
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

    node = node.data(force.nodes(), 
              function (d) { return d.id; });
    node.enter().append("circle")
        .attr("class", "node")
        .attr("r", 5)
        .call(force.drag);
    node.exit().remove();
    node.style("fill", function(d) { return color(d.state); })

    link = svg.selectAll(".link")
        .data(force.links(), 
              function (d) { d.source + "-" + d.target; });
    link.enter().append("line")
        .attr("class", "link")
    link.exit().remove();
    link.style("stroke-width", function(d) { return Math.sqrt(d.value); });

    /*node.append("title")
        .text(function(d) { return d.id; });*/

    if (newNodes) {force.start(); }

    scc.order({"provider": "graph"}, 1000)

  }

  this.onerror = function(e) { }

  this.onclose = function() {
    this.destroy()
  }

  this.destroy = function() {
    $("#graph_canvas").empty()
  }

  $("#searchById").click(function (e) {
    e.preventDefault();
    scc.order({"provider": "graph", 
               "search": "vicinity", 
               "id": $("#searchId").val()}, 0)
    return false;
  });
}
