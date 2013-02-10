var scc = {"modules": {}, "monitors": {}}

$(document).ready(function() {
  
  /* Console navigation and handling */
  var load_view = function(e) { 
    $("#modes span").removeClass("selected");
    $(".view").hide()
  }

  var load_graph = function(e) {
    load_view()
    $("#mode_graph").addClass("selected");
    $("#graph.view").fadeIn()
  }
  $("#mode_graph").click(load_graph);

  var load_resources = function(e) {
    load_view()
    $("#mode_resources").addClass("selected");
    $("#resources.view").fadeIn()
  }
  $("#mode_resources").click(load_resources);

  /* WebSocket communication */
  scc.webSocket = new ReconnectingWebSocket(
                      "ws://" + document.domain + ":" + 
                      (parseInt(window.location.port) + 1));
  setInterval(function() {
    console.log("[WebSocket] status: " + scc.webSocket.readyState)
  }, 1000);
  scc.webSocket.onopen = function(e) {
    console.log("[WebSocket] onopen");
    for (var m in scc.monitors) { scc.monitors[m].onopen(e) }
  }
  scc.webSocket.onmessage = function(e) {
    console.log("[WebSocket] onmessage");
    for (var m in scc.monitors) { scc.monitors[m].onmessage(e) }
  }
  scc.webSocket.onclose = function(e) {
    console.log("[WebSocket] onclose");
    for (var m in scc.monitors) { scc.monitors[m].onclose(e) }
  }
  scc.webSocket.onerror = function(e) {
    console.log("[WebSocket] onerror");
    for (var m in scc.monitors) { scc.monitors[m].onerror(e) }
  }

  /* Enable modules */
  scc.monitors.resources = new scc.modules.resources()

  /* Autojump to right tab depending on URL */
  switch (window.location.pathname) {
    case "/resources": load_resources(); break;
    case "/graph": load_graph(); break;
  }
});

window.onbeforeunload = function() {
  ws.onclose = function () {}; // disable onclose handler first
  ws.close();
};

