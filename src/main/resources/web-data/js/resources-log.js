/**
 *  @author Silvan Troxler
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
 * The Log module retrieves log modules from the console server. Fetched log
 * messages are populated into the HTML. When there are too many messages, the
 * old debug messages will be deleted from the DOM. It also handles the filter
 * to show or hide messages according to their level and source.
 * @constructor
 */
scc.modules.Log = function() {
  this.requires = ["log"];
  
  /**
   * Selector of the log resource box.
   * @type {Object}
   */
  var container = $("#resourceBoxes #logBox");
  
  /**
   * Selector of the scrollable container of the log messages.
   * @type {Object}
   */
  var box = container.find("div.scroll");

  /**
   * Selector of the inner container of the log messages, this is necessary for
   * formatting purposes.
   * @type {Object}
   */
  var boxInner = box.find("div");
  
  /**
   * Selector of the style element in the HTML header.
   * @type {Object}
   */
  var style = $('style');
  
  /**
   * Defines the maximum number of debug messages that will be stored in the
   * DOM. When there are more, old debug messages will be deleted from the DOM.
   * @type {number}
   */
  var maxDebugMessages = 100;
  
  /**
   * Object that stores the different levels of the log messages.
   * @type {Object}
   */
  var logLevelIndex = { "error":1, "warning":2, "info":3, "debug":4 };

  /**
   * Selector of the log level filter.
   * @type {Object}
   */
  var filterLevel = container.find("p.filter.level");

  /**
   * Object that stores the different sources of the log messages.
   * @type {Object}
   */
  var logSourceIndex = { "akka":1, "sc":2, "user":3 };

  /**
   * Selector of the log source filter.
   * @type {Object}
   */
  var filterSource = container.find("p.filter.source");
  
  // hide and show log messages based on their level
  $.each(logLevelIndex, function(v, k) {
    filterLevel.find("> span:eq(" + (k-1) + ")").on("click", function() {
      $(this).toggleClass("active");
      box.find("li.level_" + v).toggleClass("hidden_level");
    });
  });
  
  // hide and show log messages based on their source
  $.each(logSourceIndex, function(v, k) {
    filterSource.find("> span:eq(" + (k-1) + ")").on("click", function() {
      $(this).toggleClass("active");
      box.find("li.source_" + v).toggleClass("hidden_source");
    });
  });
  
  /**
   * Preset the selectors for the level filter buttons.
   * @type {Object}
   */
  var filterLevelButtons = {
    "Error":   filterLevel.find("> span:eq(0)"),
    "Warning": filterLevel.find("> span:eq(1)"),
    "Info":    filterLevel.find("> span:eq(2)"),
    "Debug":   filterLevel.find("> span:eq(3)")
  };

  /**
   * Preset the selectors for the source filter buttons.
   * @type {Object}
   */
  var filterSourceButtons = {
    "akka": filterSource.find("> span:eq(0)"),
    "sc":   filterSource.find("> span:eq(1)"),
    "user": filterSource.find("> span:eq(2)")
  };
  
  /**
   * Store the number of log messages per log level.
   * @type {Object}
   */
  var logMessagesPerLevel = {
    "Error":   0,
    "Warning": 0,
    "Info":    0,
    "Debug":   0
  };

  /**
   * Make the scrollable area use the whole viewport.
   * @tape {Callback}
   */
  var expandLogBox = (function() {
    style.text(" body.logs div#logBox div.scroll { height:" + ($(window).height() - 240) + "px; } ");
  });
  
  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests data from the ConfigurationProvider.
   */
  this.onopen = function() {
    $(document).ready(expandLogBox);
    $(window).resize(expandLogBox);

    scc.order({"requestor": "Resources", "provider": "log"});
  }

  /**
   * Function that is called by the main module when a message is received from
   * the WebSocket. It adds the new messages, scrolls the container to the
   * bottom if needed, deletes old debug messages if needed and retrieves another
   * set of log messages.
   * @param {object} msg - The message object received from the server.
   */
  this.onmessage = function(msg) {
    expandLogBox();
    var scrollDown = (Math.abs(boxInner.offset().top) + box.height() + box.offset().top >= boxInner.outerHeight());
    var fragments = [];
    var latest = null;
    msg["messages"].forEach(function(json) {
      json.occurrences = 1;
      json.cls = "";
      if (latest != null &&
          latest.level == json.level && 
          latest.cause == json.cause &&
          latest.logSource == json.logSource &&
          latest.logClass == json.logClass &&
          latest.message == json.message
         ) {
        fragments[fragments.length-1].occurrences += 1;
      } else {

        var cls = "level_" + json.level.toLowerCase() + " source_" + json.source;
        // check whether this message should be hidden
        if (!filterLevelButtons[json.level].hasClass("active"))  { cls += " hidden_level"; }
        if (!filterSourceButtons[json.source].hasClass("active")) { cls += " hidden_source"; }
        json.cls = cls;
        
        latest = json;
        fragments.push(json);
      }
      // update number of entries per level
      logMessagesPerLevel[json.level] += 1;
    });
    
    // update number of log messages in the filter buttons
    $.each(logMessagesPerLevel, function(level, nr) {
      filterLevelButtons[level].find("span").text(nr);
    })
    
    // convert fragments to HTML and append to the DOM
    var fragmentsHtml = "";
    fragments.forEach(function(json) {
      fragmentsHtml += "<li class=\"" + json.cls + "\">" + json.date + " " + json.level + ": " + json.message;
      if (json.cause != null && json.cause.length > 0) {
        fragmentsHtml += " (" + json.cause + ")";
      }
      fragmentsHtml += " &lt;" + json.logSource + ", " + json.logClass + "&gt;";
      if (json.occurrences > 1) {
        fragmentsHtml += " <small class=\"numberOfOccurences\">(" + json.occurrences + " occurrences)</small>";        
      }
      fragmentsHtml += "</li>";
    });
    box.find("ul").append(fragmentsHtml);

    // scroll
    if (scrollDown && msg.messages.length > 0) {
      box.animate({ scrollTop: box[0].scrollHeight }, 200);
    }

    // remove old debug messages
    var debugMessages = box.find('li.level_debug')
    var numDebugMessages = debugMessages.length;
    if (numDebugMessages > maxDebugMessages) {
      debugMessages.slice(0, numDebugMessages-maxDebugMessages).remove();
    }

    scc.order({"requestor": "Resources", "provider": "log"}, scc.conf.resources.intervalLogs);
  }
  
}
