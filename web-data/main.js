var scc = {"modules": {}, "consumers": {}, "defaults": {}, "orders": {}}

function Settings() {
  this.settings = loadSettings();
  this.set = function(fun) {
    fun(this.settings);
    top.location.hash = JSON.stringify(this.settings);
  }
  this.get = function() {
    return this.settings;
  }
  function loadSettings() {
    settings = { 
      "view": "graph",
      "hiddenSections": {}
    }; 
    $.each(scc.defaults, function (c) {
      settings[c] = scc.defaults[c];
    });
    hash = top.location.hash.slice(1);
    if (hash) {
      hash = JSON.parse(hash);
      $.each(settings, function (key, value) {
        if (hash[key]) { settings[key] = hash[key]; }
      });
    }
    return settings
  }
}

$(document).ready(function() {
  var hidingTimeout;
  scc.settings = new Settings();

  /* Message bar at the top */
  function hideMsg(fast) {
    if (fast) { $("#top").css("top", "-60px") }
    else { $("#top").stop().animate({"top": "-60px"}); }
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
  
  /* Console navigation and view handling */
  hideMsg();
  var clearViews = function(e) { 
    $("#modes span").removeClass("selected");
    $(".view").hide();
    $("#graph_panel_container").hide();
    $("#resources_panel_container").hide();
  }

  var showView = function(view) {
    if ($("#" + view + ".view").is(":visible")) { return }
    clearViews();
    scc.settings.set(function(s) { s.view = view; });
    $("#mode_" + view).addClass("selected");
    $("#" + view + ".view").fadeIn()
    $("#" + view + "_panel_container").show();
  }

  $("#mode_resources").click(function () { showView("resources"); });
  $("#mode_graph").click(function () { showView("graph"); });

  $(".panel_section .title").click(function () {
    section = $(this)
    if (section.next().is(":visible")) {
      scc.settings.set(function (s) { s.hiddenSections[section.text()] = true; })
      section.removeClass("expanded");
    }
    else {
      scc.settings.set(function (s) { delete s.hiddenSections[section.text()]; })
      section.addClass("expanded");
    }
    section.next().toggle(200)
  });
  $.each(scc.settings.get().hiddenSections, function (key, value) {
    $(".panel_section .title:contains(" + key + ")").each(function () {
        $(this).removeClass("expanded");
        $(this).next().hide();
    });
  });
  
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
    case "/resources": 
    case "/graph": 
      module = window.location.pathname.slice(1);
      enable_modules([module]); 
      showView(module)
      break;
    default:
      enable_modules(["graph", "resources"]);
      switch (scc.settings.get().view) {
        case "resources": showView("resources"); break;
        case "graph":
        default: showView("graph");
      }
  }
});

window.onbeforeunload = function() {
  ws.onclose = function () {}; // disable onclose handler first
  ws.close();
};
