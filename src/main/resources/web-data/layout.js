var hidingTimeout;

function toggleSection(view, title) {
  var title = $(title)
  var id = $(title).attr("id")
  if (title.next().is(":visible")) {
    var l = {}
    l[view] = {"layout": {}}
    l[view]["layout"][id] = "hide"
    scc.settings.set(l);
    title.removeClass("expanded");
  }
  else {
    var l = {}
    l[view] = {"layout": {}}
    l[view]["layout"][id] = "show"
    scc.settings.set(l);
    title.addClass("expanded");
  }
  title.next().toggle(200)
}

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
  if ($("#" + view + ".view").is(":visible")) { return; }
  clearViews();
  scc.settings.set({"main": {"view": view}});
  $("#mode_" + view).addClass("selected");
  $("#" + view + ".view").fadeIn()
  $("#" + view + "_panel_container").show();
}

function layout(modules) {
  $("#modes span").css("width", (100/modules.length) + "%");
  for (var m in modules) {
    $("span#mode_" + modules[m]).show();
  }
  $("#graph_panel_container .title").click( function() { 
    toggleSection("graph", this);
  });
  $("#resources_panel_container .title").click( function() { 
    toggleSection("resources", this);
  });
  $("#mode_resources").click(function () { showView("resources"); });
  $("#mode_graph").click(function () { showView("graph"); });
  showView(scc.settings.get().main.view)
  $.each(scc.settings.get(), function (key, value) {
    if (["graph", "resources"].indexOf(key) >= 0) { 
      $.each(value.layout, function (k, v) {
        if (v == "hide") {
          $("#" + k).removeClass("expanded");
          $("#" + k).next().hide();
        }
        else {
          $("#" + k).addClass("expanded");
          $("#" + k).next().show();
        }
      });
    }
  });
}

