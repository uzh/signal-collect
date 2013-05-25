/**
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
 */

/**
 * The Layout class offers functionality that spans both views, such as the
 * message pop-up at the top of the console or panel-related features such as
 * expanding and collapsing panel sections.
 * @constructor
 */
scc.lib.Layout = function() {

  /**
   * Object-scope variable
   */ 
  var hidingTimeout;

  /**
   * Toggle a panel section (expand/collapse)
   * @param {string} view - the name of the view (for selecting the panel)
   * @param {string} section - the name of the section to toggle
   */
  var toggleSection = function(view, title) {
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

  /**
   * Hide the pop-up message.
   * @param {boolean} fast - hide immediately if true, else slide up
   */
  var hideMsg = function(fast) {
    if (fast) { $("#top").css("top", "-85px"); $(".msg").empty(); }
    else { $("#top").stop().animate({"top": "-85px"}, function () { $(".msg").empty(); }); }
  }

  /**
   * Show the pop-up of the given type with the given method, possibly hiding
   * it after a timeout.
   * @param {string} type - one of #error, #small_error, #warning or #success
   * @param {string} msg - the message to display
   * @param {boolean} withTimeout - hide the pop-up after a short while
   */
  var showMsg = function(type, msg, withTimeout) {
    clearTimeout(hidingTimeout);
    $(".msg").stop().addClass("hidden");
    $(type).html(escapedHtmlWithNewlines(msg));
    $(type).removeClass("hidden");
    $("#top").animate({"top": "0px"});

    if (withTimeout) {
      hidingTimeout = setTimeout(function() {
        hideMsg();
      }, 3000);
    }
  }
  this.showMsg = showMsg;

  /**
   * Hide all views.
   * @param {Event} e - The event that triggered the call
   */
  var clearViews = function(e) { 
    $("#modes span").removeClass("selected");
    $(".view").hide();
    $("#graph_panel_container").hide();
    $("#resources_panel_container").hide();
  };

  /**
   * Show the view with the given name (id without '#')
   * @param {Event} e - The event that triggered the call
   */
  var showView = function(view) {
    if ($("#" + view + ".view").is(":visible")) { return; }
    clearViews();
    scc.settings.set({"main": {"view": view}});
    $("#mode_" + view).addClass("selected");
    $("#" + view + ".view").fadeIn();
    $("#" + view + "_panel_container").show();
  };

  /**
   * Correctly sizes the view-switching buttons and binds handlers to view and
   * section buttons.
   * @param {array<string>} views - the list of views to enable
   */
  this.layout = function(views) {
    $("#modes span").css("width", (100/views.length) + "%");
    for (var m in views) {
      $("span#mode_" + views[m]).show();
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

  /**
   * Helper function to correctly translate a text containing \ns to a html
   * snippet containing <br/>s.
   * @param {string} text - the text to translate
   */
  var escapedHtmlWithNewlines = function(text) {
      var htmls = [];
      var lines = text.split(/\n/);
      var tmpDiv = jQuery(document.createElement('div'));
      for (var i = 0 ; i < lines.length ; i++) {
          htmls.push(tmpDiv.text(lines[i]).html());
      }
      return htmls.join("<br>");
  }
}

