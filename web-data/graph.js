scc.modules.graph = function() {
  this.requires = ["graph"]
  
  var s;
  var paused = false;
  var layoutDone = false;

  var gridLayout = function () {
    this.ready = false,
    this.setup = function () {
      setCoordinateFunctions(function(n) { return n.id.match(/\d+/g)[0]/15; },
                             function(n) { return n.id.match(/\d+/g)[1]/15; });
      this.ready = true;
    }
    this.refresh = function () { 
      if (!this.ready) { this.setup(); }
      s.draw(2,2,2); 
    }
    this.teardown = function () { }
  }

  var forceLayout = function () {
    this.ready = false,
    this.paused = false,
    this.setup = function () {
      setCoordinateFunctions(function(n) { return Math.random(); },
                             function(n) { return Math.random(); });
      s.startForceAtlas2();
      this.ready = true;
    },
    this.refresh = function () {
      if (!this.ready) { this.setup(); }
      if (paused) { 
        if (!this.paused) {
          s.stopForceAtlas2();
          this.paused = true;
        }
        s.draw(2,0,0);
      }
      else {
        if (this.paused) {
          s.startForceAtlas2();
          this.paused = false;
        }
      }
    },
    this.teardown = function () {
      s.stopForceAtlas2();
      paused = false;
    }
  }

  var layout = new forceLayout();

  this.onopen = function() { s = sigma.init(document.getElementById('graph_canvas'))
             .drawingProperties({
                 defaultLabelColor: '#fff',
             });
    s.resize($("#content").width(), $("#content").height())

    s.drawingProperties({
      defaultLabelColor: '#ccc',
      font: 'Arial',
      edgeColor: '#22dd22',
      defaultEdgeType: 'line'
    }).graphProperties({
      minNodeSize: 1,
      maxNodeSize: 5
    });

    $("#graph_canvas").mousedown(function(e) {
      paused = true;
      layout.refresh()
    });
    $("#graph_canvas").mouseup(function(e) {
      paused = false;
      layout.refresh()
    });

    scc.webSocket.send("graph")

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
      s.addNode(n, {
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
   
    layout.refresh()

    reloadTimeout = setTimeout(function() {
      scc.webSocket.send("graph")
    }, 1000);
  }

  var setCoordinateFunctions = function(funX, funY) {
     s.iterNodes(function(n) {
      n.x = funX(n);
      n.y = funY(n);
    });
  }

  this.onerror = function(e) {
    console.log("[websocket#onerror]")
    //console.dir(e) // pollutes the console output when enabled
  }

  this.onclose = function() {
    this.destroy()
  }

  this.destroy = function() {
    s.stopForceAtlas2();
    started = paused = false;
    $("#graph_canvas").empty()
    //clearTimeout(reloadTimeout)
  }

  $("#grid_layout").click(function () {
    layout.teardown();
    layout = new gridLayout();
    layout.setup();
  });

  $("#force_layout").click(function () {
    layout.teardown();
    layout = new forceLayout();
    layout.setup();
  });

}
