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
    console.log(e)
    for (var m in scc.consumers) { scc.consumers[m].onerror(e) }
  }
  scc.order = function(msg, delay) {
    msg = JSON.stringify(msg)
    console.log("ordering " + msg)
    if (!delay) { delay = 0; }
    if (scc.orders[msg]) {
      clearTimeout(scc.orders[msg])
    }
    scc.orders[msg] = setTimeout(function() {
      try { scc.webSocket.send(msg); }
      catch(err) { scc.order(msg, 1000); }
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
      enable_modules(["graph", "resources"]); break;
  }
});

window.onbeforeunload = function() {
  ws.onclose = function () {}; // disable onclose handler first
  ws.close();
};

