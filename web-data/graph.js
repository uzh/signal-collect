$(document).ready(function() {
  var ws = new WebSocket("ws://192.168.1.3:8081");
  ws.onopen = function() {
      console.log("[websocket#onopen]\n");
  }
  ws.onmessage = function(e) {
      console.log("[websocket#onmessage] message: '" + e.data + "'\n");
  }
  ws.onclose = function() {
      console.log("[websocket#onclose]\n");
      ws = null;
  }
});
