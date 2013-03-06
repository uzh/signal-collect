var scc = {"modules": {}, "consumers": {}, "defaults": {}, "orders": {}}
scc.defaults.main = {"view": "graph",
                      "choices": {
                        "Node Selection": "topk", 
                        "TopK": "degree",
                        "Graph Layout": "forced"
                    }}


function Settings() {
  this.settings = loadSettings();
  this.set = function(modification) {
    if (typeof(modification) == "function") {
      modification(this.settings);
    }
    else {
      var newSettings = {};
      $.extend(true, newSettings, this.settings);
      $.extend(true, newSettings, modification);
      this.settings = hideDefaults(scc.defaults, newSettings);
    }
    if (Object.keys(this.settings).length > 0) {
      top.location.hash = JSON.stringify(this.settings);
    }
    else {
      top.location.hash = ""
    }
  }
  this.get = function() {
    var s = {}
    $.extend(true, s, scc.defaults);
    $.extend(true, s, this.settings);
    return s;
  }
  function loadSettings() {
    settings = {}
    hash = top.location.hash.slice(1);
    if (hash) {
      hash = JSON.parse(hash);
      $.extend(true, settings, hash);
    }
    return settings
  }
  function hideDefaults(defaults, added) {
    if (typeof(added) == "object" ) {
      $.each(added, function(k, v) {
        added[k] = hideDefaults(defaults[k], v);
        if (added[k] == null || 
           (typeof(added[k]) == "object" &&  Object.keys(added[k]).length == 0)) {
          delete added[k];
        }
      });
    }
    else {
      if (defaults == added) { return null; }
    }
    return added
  }
}

$(document).ready(function() {
  scc.settings = new Settings();

  // add keyboard shortcuts to change between tabs
  $(document).keypress(function(e) {
    if (e.which == 103) { // g
      e.preventDefault();
      showView("graph");
    }
    if (e.which == 114) { // r
      e.preventDefault();
      showView("resources");
    }
  });

  /* WebSocket communication */
  function createWebSocket () {
     scc.webSocket = new ReconnectingWebSocket(
                         "ws://" + document.domain + ":" + 
                         (parseInt(window.location.port) + 100));
      scc.webSocket.onopen = function(e) {
        console.log("[WebSocket] onopen");
        showMsg("#success", "WebSocket connection established", true);
        for (var m in scc.consumers) { scc.consumers[m].onopen(e) }
      } 
      scc.webSocket.onmessage = function(e) {
        console.log("[WebSocket] onmessage")
        j = JSON.parse(e.data)
        var provider = j["provider"]
        if (provider == "notready") {
          var request = j["request"]
          scc.order(request, 500);
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
        $.each(scc.orders, function(k, v) { clearTimeout(v); })
        scc.orders = {}
        showMsg("#error", "Connection Lost. Reconnecting to WebSocket...")
        for (var m in scc.consumers) { scc.consumers[m].onclose(e) }
      }
      scc.webSocket.onerror = function(e) {
        console.log("[WebSocket] onerror");
        for (var m in scc.consumers) { scc.consumers[m].onerror(e) }
      }
  }
  createWebSocket()
  scc.terminate = function(type, msg) {
    scc.webSocket.close()
    showMsg(type, msg)
    setTimeout(createWebSocket, 10000)
  }
  scc.order = function(msg, delay) {
    if (typeof(msg) == "string") {
      msg = JSON.parse(msg)
    }
    id = msg.provider
    if (!delay) { var delay = 0; }
    if (scc.orders[id]) {
      clearTimeout(scc.orders[id])
      delete scc.orders[id];
    }
    scc.orders[id] = setTimeout(function() {
      var j = JSON.stringify(msg)
      try { 
        scc.webSocket.send(j); 
      }
      catch(err) { 
        console.log("cannot send message: " + j)
      }
    }, delay);
  }

  enable_modules = function(modules) {
    for (var m in modules) {
      module = modules[m];
      scc.consumers[module] = new scc.modules[module]();
    }
    layout(modules);
  }
  switch (window.location.pathname) {
    case "/resources": 
    case "/graph": 
      module = window.location.pathname.slice(1);
      enable_modules([module]); 
      break;
    default:
      enable_modules(["graph", "resources", "controls"]); break;
  }

});

window.onbeforeunload = function() {
  ws.onclose = function () {}; 
  ws.close();
};

