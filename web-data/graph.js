$(document).ready(function() {
  var graph = new Rickshaw.Graph( {
    element: document.querySelector("#graph"),
    width: 400,
    height: 300,
    renderer: 'line',
    series: new Rickshaw.Series([{ name: 'This' }])
  });
  graph.render();

//  var xAxis = new Rickshaw.Graph.Axis.X({
//    graph: graph
//  });
//  xAxis.render();
//
//  var yAxis = new Rickshaw.Graph.Axis.Y({
//  graph: graph
//  });
//  yAxis.render();

  
  var ws = new WebSocket("ws://localhost:8081/resources");
  ws.onopen = function() {
    console.log("[websocket#onopen]\n");
  }

  ws.onmessage = function(e) {
    console.log("[websocket#onmessage] message: '" + e.data + "'\n");
 
    var newDataJson = eval("(" + e.data + ")");
    
    var newData = [];
    for (var index in newDataJson.workerStats[0]) {
      newData.push({ 'x': index, 'y': newDataJson.workerStats[0][index]});
    }
    graph.series[0].data = newData;
    graph.update();
  }

  ws.onclose = function() {
    console.log("[websocket#onclose]\n");
    ws = null;
  }

});
