$(document).ready(function() {
  
  // simple graph that visualizes all data that it gets
  var graph = new Rickshaw.Graph( {
    element: document.querySelector("#graph"),
    width: 400,
    height: 300,
    renderer: 'line',
    series: new Rickshaw.Series([{ name: 'This' }])
  });
  graph.render();

 
  

  // add a trending graph with real time data
  var tv = 1000;
  //instantiate our graph!
  var graph2 = new Rickshaw.Graph( {
    element: document.getElementById("chart"),
    width: 400,
    height: 300,
    renderer: 'line',
    series: new Rickshaw.Series.FixedDuration([{ name: 'one' }], undefined, {
      timeInterval: tv,
      maxDataPoints: 100,
      timeBase: new Date().getTime() / 1000
    }) 
  } );
  
  graph2.render();
  

  while (typeof ws === "undefined") {
    try {
      console.log("[websocket] Trying to connect...");
      ws = new WebSocket("ws://" + document.domain + ":" + 
                        (parseInt(window.location.port) + 1));
      console.log("[websocket] Connected");
    } catch (e) {
      console.log("[websocket] Could not connect, retrying in 1 second...");
      sleep(1000);
    }
  }
  
  ws.onopen = function() {
    console.log("[websocket#onopen]\n");
    ws.send("resources")
  }
  
  ws.onerror = function(e) {
    console.log("[websocket#onerror]")
    console.dir(e)
  }

  ws.onmessage = function(e) {
    console.log("[websocket#onmessage] message: '" + e.data + "'\n");
 
    var newDataJson = eval("(" + e.data + ")");
    
    // graph 1
    var newData  = [];
    for (var index in newDataJson.workerStats[0]) {
      newData.push({ 'x': index, 'y': newDataJson.workerStats[0][index]});
    }
    graph.series[0].data = newData;
    graph.update();

    // graph 2
    newData = { one: newDataJson.workerStats[0][2] };
    graph2.series.addData(newData);
    graph2.render();
    
    setTimeout(function(){
      ws.send("resources")
    }, 2000);
    
  }

  ws.onclose = function() {
    console.log("[websocket#onclose]\n");
    ws = null;
  }
  
});


window.onbeforeunload = function() {
  ws.onclose = function () {}; // disable onclose handler first
  ws.close();
};
