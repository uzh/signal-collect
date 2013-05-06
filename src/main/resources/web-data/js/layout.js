/*
 *  @author Carol Alexandru
 *  
 *  Copyright 2013 University of Zurich
 *      
 *  Licensed below the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed below the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations below the License.
 *  
 */

var hidingTimeout;

function toggleSection(view, title) {
  var title = $(title);
  var section = title.next();
  var id = title.parent().attr("id");
  if (section.is(":visible")) {
    var l = {};
    l[view] = {"layout": {}};
    l[view]["layout"][id] = "hide";
    scc.settings.set(l);
    title.removeClass("expanded");
  }
  else {
    var l = {};
    l[view] = {"layout": {}};
    l[view]["layout"][id] = "show";
    scc.settings.set(l);
    title.addClass("expanded");
  }
  section.toggle(200);
}

/* Message bar at the top */
function hideMsg(fast) {
  if (fast) { $("#top").css("top", "-85px"); $(".msg").empty(); }
  else { $("#top").stop().animate({"top": "-85px"}, function () { $(".msg").empty(); }); }
}

function showMsg(type, msg, timeout) {
  clearTimeout(hidingTimeout);
  $(".msg").stop().addClass("hidden");
  $(type).html(escapedHtmlWithNewlines(msg));
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
};

var showView = function(view) {
  if ($("#" + view + ".view").is(":visible")) { return; }
  clearViews();
  scc.settings.set({"main": {"view": view}});
  $("#mode_" + view).addClass("selected");
  $("#" + view + ".view").fadeIn();
  $("#" + view + "_panel_container").show();
};

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
  showView(scc.settings.get().main.view);
  $.each(scc.settings.get(), function (key, value) {
    if (["graph", "resources"].indexOf(key) >= 0) { 
      $.each(value.layout, function (k, v) {
        if (v == "hide") {
          $("#" + k + " .title").removeClass("expanded");
          $("#" + k + " .contents").hide();
        }
        else {
          $("#" + k + " .title").addClass("expanded");
          $("#" + k + " .contents").show();
        }
      });
    }
  });
  $("#close_msgs").click(function () {
    hideMsg();
  });
}

function escapedHtmlWithNewlines(text) {
    var htmls = [];
    var lines = text.split(/\n/);
    var tmpDiv = jQuery(document.createElement('div'));
    for (var i = 0 ; i < lines.length ; i++) {
        htmls.push(tmpDiv.text(lines[i]).html());
    }
    return htmls.join("<br>");
}


