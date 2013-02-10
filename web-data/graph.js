scc.modules.graph = function() {
  this.requires = ["graph"]
  
  var sigInst;

  this.onopen = function() {

    sigma.publicPrototype.gridLayout = function() {
      i = 0,
      L = this.getNodesCount();
       
      this.iterNodes(function(n){
        var coordinates = n.id.match(/\d+/g)
        n.x = coordinates[0]/20;
        n.y = coordinates[1]/20;
      });
       
      return this.position(0,0,1).draw();
    };

    sigInst = sigma.init(document.getElementById('graph_canvas'))
                   .drawingProperties({
                     defaultLabelColor: '#fff'
                   });

    sigInst.drawingProperties({
      defaultLabelColor: '#ccc',
      font: 'Arial',
      edgeColor: 'source',
      defaultEdgeType: 'line'
    }).graphProperties({
      minNodeSize: 1,
      maxNodeSize: 5
    });
    scc.webSocket.send("graph")
    sigInst.gridLayout();
  }
   
  this.onmessage = function(j) {
    sigInst.iterNodes(function(n) {
      if (!j["nodes"][n.id]) {
        sigInst.dropNode(n)
      }
      else {
        n.label = j["nodes"][n.id];
        n.size = 3+3*parseInt(j["nodes"][n.id]);
        delete j["nodes"][n.id];
      }
    });
    for (var n in j["nodes"]) {
      var coordinates = n.match(/\d+/g)
      sigInst.addNode(n, {
        'x': coordinates[0]/20,
        'y': coordinates[1]/20,
        'label': j["nodes"][n],
        'size': 3+3*parseInt(j["nodes"][n]),
        'color': 'rgb('+Math.round(Math.random()*256)+','+
                        Math.round(Math.random()*256)+','+
                        Math.round(Math.random()*256)+')'
      });
    }
   
    sigInst.iterEdges(function(e) {
      if (!j["edges"][e.id]) {
        sigInst.dropEdge(e)
      }
      else {
        delete j["edges"][e.id];
      }
    });
    for(var e in j["edges"]) {
      var edge = j["edges"][e]
      sigInst.addEdge(e, edge["source"], edge["target"]);
    }
   
    sigInst.draw(2,2,2);
   
    reloadTimeout = setTimeout(function(){
      scc.webSocket.send("graph")
    }, 1000);

  }

  this.onerror = function(e) {
    console.log("[websocket#onerror]")
    console.dir(e)
  }

  this.onclose = function() {
    this.destroy()
  }

  this.destroy = function() {
    $("#graph_canvas").empty()
    //clearTimeout(reloadTimeout)
  }

}
