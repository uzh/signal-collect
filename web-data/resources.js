scc.modules.resources = function() {
  this.requires = ["resources"]

  var graph1, graph2, reloadTimeout

  this.onopen = function () {
    graph1 = new Rickshaw.Graph( {
      element: document.querySelector("#res_graph1"),
      width: 400,
      height: 250,
      renderer: 'line',
      series: new Rickshaw.Series([{ name: 'This' }])
    });
    graph1.render();

    var tv = 1000;
    graph2 = new Rickshaw.Graph( {
      element: document.getElementById("res_graph2"),
      width: 450,
      height: 250,
      renderer: 'line',
      series: new Rickshaw.Series.FixedDuration([{ name: 'one' }], undefined, {
        timeInterval: tv,
        maxDataPoints: 100,
        timeBase: new Date().getTime() / 1000
      }) 
    } );
    
    graph2.render();
    scc.webSocket.send("resources")
  }
    
  this.onerror = function(e) {
    console.log("[websocket#onerror]")
    console.dir(e)
  }

  this.onmessage = function(j) {
    // graph 1
    var newData  = [];
    for (var index in j.workerStatistics["collectOperationsExecuted"]) {
      newData.push({ 'x': index, 
                     'y': j.workerStatistics["messagesSent"][index]});
    }
    graph1.series[0].data = newData;
    graph1.update();

    // graph 2
    newData = { one: j.workerStatistics["messagesSent"] };
    graph2.series.addData(newData);
    //graph2.render();
    
    reloadTimeout = setTimeout(function(){
      scc.webSocket.send("resources")
    }, 2000);
  }

  this.onclose = function() {
    this.destroy()
  }

  this.destroy = function() {
    $("#res_graph1").empty()
    $("#res_graph2").empty()
    clearTimeout(reloadTimeout)
  }
}

