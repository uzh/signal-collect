var scc = {"modules": {}, "consumers": {}}

$(document).ready(function() {
  
  /* Console navigation and handling */
  var load_view = function(e) { 
    $("#modes span").removeClass("selected");
    $(".view").hide()
  }

  var load_graph = function(e) {
    load_view()
    top.location.hash = "graph";
    $("#mode_graph").addClass("selected");
    $("#graph.view").fadeIn()
  }
  $("#mode_graph").click(load_graph);

  var load_resources = function(e) {
    load_view()
    top.location.hash = "resources";
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
    for (var m in scc.consumers) { scc.consumers[m].onopen(e) }
  }
  scc.webSocket.onmessage = function(e) {
    j = JSON.parse(e.data)
    console.log("[WebSocket] onmessage");
    var provider = j["provider"]
    for (var m in scc.consumers) { 
      var consumer = scc.consumers[m]
      if (consumer.requires.indexOf(provider) >= 0) {
        consumer.onmessage(j)
      }
    }
  }
  scc.webSocket.onclose = function(e) {
    console.log("[WebSocket] onclose");
    for (var m in scc.consumers) { scc.consumers[m].onclose(e) }
  }
  scc.webSocket.onerror = function(e) {
    console.log("[WebSocket] onerror");
    for (var m in scc.consumers) { scc.consumers[m].onerror(e) }
  }

  /* Enable modules */
  scc.consumers.resources = new scc.modules.resources()
  scc.consumers.graph = new scc.modules.graph()

  /* Autojump to right tab depending on URL */
  switch (top.location.hash) {
    case "#resources": load_resources(); break;
    default: load_graph(); break;
  }
});

window.onbeforeunload = function() {
  ws.onclose = function () {}; // disable onclose handler first
  ws.close();
};

