scc.modules.graph = function() {
  this.requires = ["graph"]
  
  var s;

  this.onopen = function() {

    s = sigma.init(document.getElementById('graph_canvas'))
             .drawingProperties({
                 defaultLabelColor: '#fff',
             });
    s.resize($("#content").width(), $("#content").height())

    sigma.publicPrototype.gridLayout = function() {
      i = 0,
      L = this.getNodesCount();
       
      this.iterNodes(function(n){
        var coordinates = n.id.match(/\d+/g)
        n.x = coordinates[0]/15;
        n.y = coordinates[1]/15;
      });
       
      return this.position(0,0,1).draw();
    };

    s.drawingProperties({
      defaultLabelColor: '#ccc',
      font: 'Arial',
      edgeColor: '#22dd22',
      defaultEdgeType: 'line'
    }).graphProperties({
      minNodeSize: 1,
      maxNodeSize: 5
    });
    scc.webSocket.send("graph")
    s.gridLayout();
  }
   
  this.onmessage = function(j) {
    s.iterNodes(function(n) {
      if (!j["nodes"][n.id]) {
        s.dropNode(n)
      }
      else {
        n.label = j["nodes"][n.id];
        n.size = 3+3*parseInt(j["nodes"][n.id]);
        delete j["nodes"][n.id];
      }
    });
    for (var n in j["nodes"]) {
      var coordinates = n.match(/\d+/g)
      s.addNode(n, {
        'x': coordinates[0]/15,
        'y': coordinates[1]/15,
        'label': j["nodes"][n],
        'size': 3+3*parseInt(j["nodes"][n]),
        'color': '#16bbbd'
      });
    }
   
    s.iterEdges(function(e) {
      if (!j["edges"][e.id]) {
        s.dropEdge(e)
      }
      else {
        delete j["edges"][e.id];
      }
    });
    for(var e in j["edges"]) {
      var edge = j["edges"][e]
      s.addEdge(e, edge["source"], edge["target"]);
    }
   
    s.draw(2,2,0);
   
    reloadTimeout = setTimeout(function(){
      scc.webSocket.send("graph")
    }, 1000);

  }

  this.onerror = function(e) {
    console.log("[websocket#onerror]")
    //console.dir(e) // pollutes the console output when enabled
  }

  this.onclose = function() {
    this.destroy()
  }

  this.destroy = function() {
    $("#graph_canvas").empty()
    //clearTimeout(reloadTimeout)
  }

}
