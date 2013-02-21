scc.modules.resources = function() {
  this.requires = ["resources"]

  var graph1, graph2, reloadTimeout

  this.onopen = function () {
//    graph1 = new Rickshaw.Graph( {
//      element: document.querySelector("#res_graph1"),
//      width: 400,
//      height: 250,
//      renderer: 'line',
//      series: new Rickshaw.Series([{ name: 'This' }])
//    });
//    graph1.render();

    var tv = 1000;
    graph2 = new Rickshaw.Graph( {
      element: document.getElementById("res_graph2"),
      width: 540,
      height: 240,
//      renderer: 'line',
      series: new Rickshaw.Series.FixedDuration([{ name: 'one', color: 'steelblue' }], undefined, {
        timeInterval: tv,
        maxDataPoints: 50,
//        timeBase: new Date().getTime() / 1000,
        color: 'steelblue'
      }) 
    });
    
    var hoverDetail = new Rickshaw.Graph.HoverDetail( {
    	graph: graph2
    } );
    
//    var x_axis = new Rickshaw.Graph.Axis.Time( { graph: graph } );
    
//    var y_axis = new Rickshaw.Graph.Axis.Y( {
//        graph: graph2,
//        orientation: 'left',
//        tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
//        element: document.getElementById('y_axis'),
//    });
//    y_axis.render();
    
//    graph2.render();
    scc.order("resources")
  }
    
  this.onerror = function(e) {
    console.log("[websocket#onerror]")
    //console.dir(e) // pollutes the console output when enabled
  }

  this.onmessage = function(j) {
    // graph 1
//    var newData  = [];
//    for (var index in j.workerStatistics["collectOperationsExecuted"]) {
//      newData.push({ 'x': index, 
//                     'y': j.workerStatistics["messagesSent"][index]});
//    }
//    graph1.series[0].data = newData;
//    graph1.update();

    // graph 2
	
    newData = { one: j.workerStatistics["messagesSent"][0] - Math.floor(Math.random()*(j.workerStatistics["messagesSent"][0]/5)) };
    graph2.series.addData(newData);
    graph2.render();
    
    scc.order("resources", 2000);
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

