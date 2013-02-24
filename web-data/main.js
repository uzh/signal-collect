var scc = {"modules": {}, "consumers": {}, "orders": {}}

$(document).ready(function() {
 
  var hidingTimeout;
  var resource = "resources", graph = "graph";

  function hideMsg(fast) {
    if (fast) {
      $("#top").css("top", "-60px")
    }
    else {
      $("#top").stop().animate({"top": "-60px"});
    }
  }
  hideMsg(true);

  function showMsg(type, msg, timeout) {
    clearTimeout(hidingTimeout);
    $(".msg").stop().addClass("hidden");
    $(type).html(msg);
    $(type).removeClass("hidden");
    $("#top").animate({"top": "0px"});

    if (timeout) {
      hidingTimeout = setTimeout(function() {
        hideMsg();
      }, 3000);
    }
  }
  
  /* Console navigation and handling */
  hideMsg();
  var clear_views = function(e) { 
    $("#modes span").removeClass("selected");
    $(".view").hide();
    $("#graph_panel_container").hide();
    $("#resource_panel_container").hide();
  }

  var show_graph = function(e) {
    if ($("#graph.view").is(":visible")) { return }
    clear_views();
    set_module(graph);
    $("#mode_graph").addClass("selected");
    $("#graph.view").fadeIn()
    $("#graph_panel_container").show();
  }
  $("#mode_graph").click(show_graph);

  var show_resources = function(e) {
    if ($("#resources.view").is(":visible")) { return }
    clear_views();
    set_module(resource);
    $("#mode_resources").addClass("selected");
    $("#resources.view").fadeIn()
    $("#resource_panel_container").show();
  }
  $("#mode_resources").click(show_resources);
  
  // add keyboard shortcuts to change between tabs
  $(document).keypress(function(e) {
    if (e.which == 103) { // g
      e.preventDefault();
      show_graph();
    }
    if (e.which == 114) { // r
      e.preventDefault();
      show_resources();
    }
  });

  /* WebSocket communication */
  scc.webSocket = new ReconnectingWebSocket(
                      "ws://" + document.domain + ":" + 
                      (parseInt(window.location.port) + 1));
  scc.webSocket.onopen = function(e) {
    console.log("[WebSocket] onopen");
    showMsg("#success", "WebSocket connection established", true);
    for (var m in scc.consumers) { scc.consumers[m].onopen(e) }
  } 
  scc.webSocket.onmessage = function(e) {
    j = JSON.parse(e.data)
    var provider = j["provider"]
    if (provider == "notready") {
      var request = j["request"]
      scc.order(request, 1000);
    }
    for (var m in scc.consumers) { 
      var consumer = scc.consumers[m]
      if (consumer.requires.indexOf(provider) >= 0) {
        consumer.onmessage(j)
      }
    }
  }
  scc.webSocket.onclose = function(e) {
    console.log("[WebSocket] onclose");
    showMsg("#error", "Connection Lost. Reconnecting to WebSocket...");
    for (var m in scc.consumers) { scc.consumers[m].onclose(e) }
  }
  scc.webSocket.onerror = function(e) {
    console.log("[WebSocket] onerror");
    for (var m in scc.consumers) { scc.consumers[m].onerror(e) }
  }
  scc.order = function(msg, delay) {
    if (!delay) { delay = 0; }
    if (scc.orders[msg]) {
      clearTimeout(scc.orders[msg])
    }
    scc.orders[msg] = setTimeout(function() {
      try { scc.webSocket.send(msg); }
      catch(err) { scc.order(msg, 1000); }
    }, delay);
  }

  /* Enable modules depending on URL and jump to a tab depending on hashtag */
  enable_modules = function(modules) {
    $("#modes span").css("width", (100/modules.length) + "%");
    for (var m in modules) {
      module = modules[m];
      scc.consumers[module] = new scc.modules[module]();
      $("span#mode_" + module).show();
    }
  }
  switch (window.location.pathname) {
    case "/" + resource: 
      enable_modules(["resources"]); 
      show_resources()
      break;
    case "/" + graph: 
      enable_modules(["graph"]);
      show_graph()
      break;
    default:
      enable_modules(["graph", "resources"]);
      switch (get_module()) {
        case resource: show_resources(); break;
        case graph:
        default: show_graph();
      }
  }
});

window.onbeforeunload = function() {
  ws.onclose = function () {}; // disable onclose handler first
  ws.close();
};

/* returns the hash without # and splitted at / */
function get_hash_splitted() {
  hash = top.location.hash.slice(1); // remove #
  parts = hash.split("/");
  return parts;    
}

/* Hash format: MODULE/SECTION */
function get_module() {
  parts = get_hash_splitted(); 
//  console.log("Module: " + parts[0]);
  return parts[0]
}
function set_module(m) {
  // change pathname in single-module-mode
  if (window.location.pathname != "/") {
    window.location.pathname = "/" + m;
  }
  // change hash tag
  parts = get_hash_splitted();
  if (parts[0] != m) { set_section(""); }
  parts[0] = m;
  top.location.hash = parts.join("/");
}
function get_section () {
  parts = get_hash_splitted(); 
  if (parts[1] == null) {
//    console.log("Section: ");
    return "";
  }
//  console.log("Module: " + parts[1]);
  return parts[1];
}
function set_section(m) {
  parts = get_hash_splitted();
  parts[1] = m;
  top.location.hash = parts.join("/");
}
