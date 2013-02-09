$(document).ready(function() {
  
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
    ws.send("graph")
  }
  
  ws.onerror = function(e) {
    console.log("[websocket#onerror]")
    console.dir(e)
  }

  ws.onmessage = function(e) {
    console.log("[websocket#onmessage] message: '" + e.data + "'\n");
 
    var newDataJson = eval("(" + e.data + ")");
    
    setTimeout(function(){
      ws.send("graph")
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
