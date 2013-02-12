var scc = {"modules": {}, "consumers": {}}

$(document).ready(function() {
  
  /* Console navigation and handling */
  var clear_views = function(e) { 
    $("#modes span").removeClass("selected");
    $(".view").hide()
  }

  var load_graph = function(e) {
    if ($("#graph.view").is(":visible")) { return }
    clear_views()
    top.location.hash = "graph";
    $("#mode_graph").addClass("selected");
    $("#graph.view").fadeIn()
    console.log(scc.consumers)
  }
  $("#mode_graph").click(load_graph);

  var load_resources = function(e) {
    if ($("#resources.view").is(":visible")) { return }
    clear_views()
    top.location.hash = "resources";
    $("#mode_resources").addClass("selected");
    $("#resources.view").fadeIn()
  }
  $("#mode_resources").click(load_resources);
  
  // add keyboard shortcuts to change between tabs
  $(document).keypress(function(e) {
	if (e.which == 103) { // g
	  e.preventDefault();
	  load_graph();
	}
	if (e.which == 114) { // r
	  e.preventDefault();
	  load_resources();
	}
  });

  /* WebSocket communication */
  scc.webSocket = new ReconnectingWebSocket(
                      "ws://" + document.domain + ":" + 
                      (parseInt(window.location.port) + 1));
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

