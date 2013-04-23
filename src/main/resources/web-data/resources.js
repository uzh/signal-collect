/*
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
 *  
 */

/**
 * The default settings for the graph module.
 * @constant
 * @default
 * @type {Object}
 */
scc.defaults.resources = {"layout":{
                            "cResourceComputation": "show",
                            "cResourceProblems": "show"
                          },
                          "section": "statistics"
};


/**
 * Event handler to catch onHashChange events to update the current section.
 */
$(window).on('hashchange', function() {
  scc.settings.reload();
  var settings = scc.settings.get();
  if (settings.resources.section != null) {
    show_section(settings.resources.section);
  }
});

/**
 * Hides all sections and shows only the given section.
 * @param {string} s - The name of the section to show.
 */
function show_section(s) {
  console.log("Section: " + s);
  if (s == "") { return; }
  // hide all sections
  $("#resources .structured > div[id^=\"crs\"]").hide();
  // show the appropriate section
  $("#crs_" + s).show();
  show_boxes(s);
  // show change in the panel
  $("#resources_panel_container input").prop("checked", false);
  $("#resources_panel_container input#rs_" + s + "").prop("checked", true);
  // change body class
  $("body").attr("class", s);
  // set section to the hash tag
  var mod = {"resources": {"section": s }}
}

/**
 * Hides all resource boxes and only show the ones that are needed for the
 * given section.
 * @param {string} s - The name of the section to show the resource boxes for.
 */
function show_boxes(s) {
  var boxes = "#resourceBoxes";
  // first, hide all of them
  $(boxes + " > div").attr("class", "hidden");
  if (scc.conf.resources.resourceBoxes[s] == null) { return; }
  // then only show the ones that are needed
  scc.conf.resources.resourceBoxes[s].forEach(function(v) {
    var resourceBox = boxes + " > #" + v;
    $(resourceBox).removeClass("hidden");
    $(resourceBox).appendTo(boxes); // change order
  });
}

/**
 * Event handler that gets called when the DOM is ready.
 */
$(document).ready(function() {
  $("#resources_panel_container label").click(function() {
    section = $(this).attr("for").split("_")[1];
    scc.settings.set({'resources':{'section':section}});
  });
});

/**
 * The Configuration module retrieves data about different configuration and
 * parameter statistics from the console server. The module shows the JVM
 * parameters, the computation statistics as well as the graph statistics.
 * @constructor
 */
scc.modules.Configuration = function() {
  this.requires = ["configuration"];

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests data from the ConfigurationProvider.
   */
  this.onopen = function () {
    scc.order({"provider": "configuration"});
  }
    
  /**
   * Function that is called by the main module when a WebSocket error is
   * encountered. Does nothing.
   * @param {Event} e - The event that triggered the call.
   */
  this.onerror = function(e) { }

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Does nothing.
   */
  this.notready = function() { }

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. Does nothing.
   */
  this.onclose = function() { }

  /**
   * Function that is called by the main module when a message is received from
   * the WebSocket. It populates the given information about the JVM,
   * computation and the graph in the proper elements.
   * @param {object} msg - The message object received from the server.
   */
  this.onmessage = function(msg) {
    $.each(msg.executionConfiguration, function(k,v) {
      if (v instanceof Array) {
        v = v.join(",");
      }
      $("#resStat" + k).html(v);
    });
    var ul = $("#infrastructureStatBox ul").html('');
    $.each(msg.systemProperties, function(index) {
      $.each(msg.systemProperties[index], function(k, v) {
        ul.append('<li>' + k + ': ' + v + '</li>');
      });
    });
    ul = $("#graphStatBox ul").html('');
    $.each(msg.graphConfiguration, function(k, v) {
      ul.append('<li>' + k + ': ' + v[0] + '</li>');
    });
  }
}




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
   * Stores the latest log message to test whether a new one is exactly the same
   * and to not show it again.
   * @type {Object}
   */
  var latest;
  
  /**
   * Stores the number of identical log messages which occurred directly after
   * each other to not show anyone of these but only one occurrence and add the
   * number of occurrences to the end.
   * @type {number}
   */
  var identicalLogMessages = 0;
  
  /**
   * Selector of the log resource box.
   * @type {Object}
   */
  var container = $("#resourceBoxes #logBox");
  
  /**
   * Selector of the scrollable container of the log messages.
   * @type {Object}
   */
  var box = $(container).find("div.scroll");

  /**
   * Selector of the inner container of the log messages, this is necessary for
   * formatting purposes.
   * @type {Object}
   */
  var boxInner = $(box).find("div");
  
  /**
   * Defines the maximum number of debug messages that will be stored in the
   * DOM. When there are more, old debug messages will be deleted from the DOM.
   * @type {number}
   */
  var maxDebugMessages = 200;
  
  /**
   * Object that stores the different levels of the log messages.
   * @type {Object}
   */
  var logLevelIndex = { "error":1, "warning":2, "info":3, "debug":4 };

  /**
   * Selector of the log level filter.
   * @type {Object}
   */
  var filterLevel = $(container).find("p.filter.level");

  /**
   * Object that stores the different sources of the log messages.
   * @type {Object}
   */
  var logSourceIndex = { "akka":1, "sc":2, "user":3 };

  /**
   * Selector of the log source filter.
   * @type {Object}
   */
  var filterSource = $(container).find("p.filter.source");
  
  // hide and show log messages based on their level
  $.each(logLevelIndex, function(v, k) {
    $(filterLevel).find("> span:eq(" + (k-1) + ")").on("click", function() {
      $(this).toggleClass("active");
      $(box).find("li.level_" + v).toggleClass("hidden_level");
    });
  });
  
  // hide and show log messages based on their source
  $.each(logSourceIndex, function(v, k) {
    $(filterSource).find("> span:eq(" + (k-1) + ")").on("click", function() {
      $(this).toggleClass("active");
      $(box).find("li.source_" + v).toggleClass("hidden_source");
    });
  });

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests data from the ConfigurationProvider.
   */
  this.onopen = function() {
    // make it using the full height
    onResize = (function() {
      $("body.logs div#logBox div.scroll").css("height", ($(window).height() - 280) + "px");
    });
    $(document).ready(onResize);
    $(window).resize(onResize);

    scc.order({"provider": "log"});
  }

  /**
   * Function that is called by the main module when a message is received from
   * the WebSocket. It adds the new messages, scrolls the container to the
   * bottom if needed, deletes old debug messages if needed and retrieves another
   * set of log messages.
   * @param {object} msg - The message object received from the server.
   */
  this.onmessage = function(msg) {
    var scrollDown = (Math.abs(boxInner.offset().top) + box.height() + box.offset().top >= boxInner.outerHeight());
    msg["messages"].forEach(function(l) {
      var json = $.parseJSON(l);
      var filterLevelButton = $(filterLevel).find("> span:eq(" + (logLevelIndex[json.level.toLowerCase()]-1) + ")");
      var filterSourceButton = $(filterSource).find("> span:eq(" + (logSourceIndex[json.source.toLowerCase()]-1) + ")");
      if (latest != null &&
          latest.level == json.level && 
          latest.cause == json.cause &&
          latest.logSource == json.logSource &&
          latest.logClass == json.logClass &&
          latest.message == json.message
         ) {
        identicalLogMessages += 1;
        $(container).find("small.numberOfOccurences").last()
                    .text(" (" + (identicalLogMessages+1) + " times)");
      } else {
        var log = json.date + " " + json.level + ": " + json.message;
        if (json.cause != null && json.cause.length > 0) {
          log += " (" + json.cause + ")";
        }
        log += " &lt;" + json.logSource + ", " + json.logClass + "&gt;";
        var cls = "level_" + json.level.toLowerCase() + " source_" + json.source;
        // check whether this message should be hidden
        if (!$(filterLevelButton).hasClass("active"))  { cls += " hidden_level"; }
        if (!$(filterSourceButton).hasClass("active")) { cls += " hidden_source"; }
        $(box).find("ul")
              .append("<li class=\"" + cls + "\">" + log + "<small class=\"numberOfOccurences\"></small></li>");
        latest = json;
        identicalLogMessages = 0;
      }
      // number of entries
      var nr = $(filterLevelButton).find("span");
      $(nr).text(parseInt($(nr).text()) + 1);
    });
    // scroll
    if (scrollDown && msg.messages.length > 0) {
      $(box).animate({ scrollTop: $(box)[0].scrollHeight }, 200);
    }

    // remove old debug messages
    var debugMessages = $(box).find('li.level_debug')
    var numDebugMessages = $(debugMessages).length;
    if (numDebugMessages > maxDebugMessages) {
      $(debugMessages).slice(0, numDebugMessages-maxDebugMessages).remove();
    }

    scc.order({"provider": "log"}, scc.conf.resources.intervalLogs);
  }
  
}



/**
 * The Resources module handles the chart drawing and all actions which can be
 * done on charts (e.g. zooming and shifting). It also handles the calculation
 * of the data set size estimation, and some statistics about the computation.
 * @constructor
 */
scc.modules.Resources = function() {
  this.requires = ["resources"];
  show_section(scc.settings.get().resources.section);
  
  // fill detailed view with all the charts we have
  setTimeout(function() {
    $("#resourceBoxes div[id$='Chart']").each(function() {
      scc.conf.resources.resourceBoxes.detailed.push($(this).attr("id"));
    });
  }, 500);
  
  // show the number of seconds after which the statistics will be updated
  $("#resStatInterval").html(scc.conf.resources.intervalStatistics / 1000);
  
  /**
   * The statistics are only updated every x seconds; this variable stores the
   * Date of the last update.
   * @type {Date}
   */
  var statisticsLastUpdated = new Date(0);

  /**
   * On the first message, we check whether there are any new objects which we
   * did not know about before and for which we should draw a chart. These
   * objects will be added to the DOM and stored in the necessary variables
   * @type {boolean}
   */
  var hasAddedNewCharts = false;
  
  /**
   * Stores the date of the last update of the data set size estimation.
   * @type {Date}
   */
  var estimationsLastUpdated = new Date(0);
  
  /**
   * Object container that encapsulates all chart objects.
   * @type {Object}
   */
  var lineCharts = {};
  
  /**
   * Helper function to create a chart and store it to the chart container.
   * @param {Object} config - The configuration to use to create the chart.
   */
  var ChartsCreate = function(config) {
    var lineChart = new LineChart();
    lineChart.container = "#resourceBoxes";
    lineChart.setup(config);
    lineCharts[lineChart.config.jsonName] = lineChart;
  }
  
  scc.conf.resources.chartConfig.forEach(function(config) {
    if (!config.skip) {
      ChartsCreate(config);
    }
  });
  
  // update boxes (needed to show the charts on reload)
  setTimeout(function() {
    show_boxes(scc.settings.get().resources.section);
  }, 600);
  

  // zooming buttons
  $("#chartZoomer .zoomIn").click(function() {
    console.log("Zoom: In");
    zooming(1.2);
  });
  $("#chartZoomer .zoomOut").click(function() {
    console.log("Zoom: Out");
    zooming(0.8);
  });
  
  /**
   * Helper function to zoom all charts in or out.
   * @param {number} scale - The scale to which to zoom in or out.
   */
  var zooming = function(scale) {
    $.each(lineCharts, function(key, chart) {
      chart.setZoom(scale);
    });
  }
  

  // moving buttons
  $("#chartZoomer .moveLeft").click(function() {
    console.log("Move: Left");
    moving(-1);
  });
  $("#chartZoomer .moveRight").click(function() {
    console.log("Move: Right");
    moving(1);
  });
  $("#chartZoomer .moveOrigin").click(function() {
    console.log("Move: Origin");
    moving(0);
  });

  /**
   * Helper function to shift all charts left, right, or to the origin.
   * @param {number} scale - The scale to which to shift to.
   */
  var moving = function(scale) {
    $.each(lineCharts, function(key, chart) {
      chart.setMove(scale);
    });
  }
  
  
  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests data from the ResourceProvider.
   */
  this.onopen = function () {
    scc.order({"provider": "resources"})
  }

  /**
   * Function that is called by the main module when a WebSocket error is
   * encountered. Does nothing.
   * @param {Event} e - The event that triggered the call.
   */
  this.onerror = function(e) { }

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Does nothing.
   */
  this.notready = function() { }

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. Does nothing.
   */
  this.onclose = function() { }

  /**
   * Helper function to check whether {@code chartConfig} contains a chart with
   * the given key.
   * @param {object} key - The key name to check whether there is such a chart
   *     stored already.
   */
  var ChartsContains = function(key) {
    var found = false;
    scc.conf.resources.chartConfig.forEach(function(c) {
      if (c.jsonName == key) {
        found = true;
        return found;
      }
    });
    return found;
  }

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. A new message is distributed to the several charts
   * which then update their visualization. This function also updates the
   * statistics and recalculates the data set size estimation from time to time.
   * @param {object} msg - The message object received from the server.
   */
  this.onmessage = function(msg) {
    
    // check if there is more data in the websockets
    if (!hasAddedNewCharts) {
      for (var key in msg.workerStatistics) {
        if (msg.workerStatistics.hasOwnProperty(key) && !ChartsContains(key)) {
          ChartsCreate({jsonName : key});
        }
      }
      hasAddedNewCharts = true;
    }
    
    // update all graphs
    $.each(lineCharts, function(k,v) { v.update(msg); });
    
    // update statistics
    if (statisticsLastUpdated.addMilliseconds(scc.conf.resources.intervalStatistics) <= msg.timestamp) {
      var resStatStartTime = $("#resStatStartTime");
      if (resStatStartTime.html() == "?") {
        resStatStartTime.html(new Date(msg.timestamp).dateTime());
      }
      $("#resStatRunTime").html(new Date(msg.timestamp-(new Date(resStatStartTime.html()))).durationPretty());
      $("#resStatWorkers").html(msg.workerStatistics.workerId.length);
      statisticsLastUpdated = new Date(msg.timestamp);
    }
    
    // update estimations
    if (estimationsLastUpdated.addMilliseconds(scc.conf.resources.intervalStatistics) <= msg.timestamp) {
      if (lineCharts.runtime_mem_total.dataLength() >= 10) {
        var maxMemory = lineCharts.runtime_mem_max.dataLatest();
        var avgMemory = lineCharts.runtime_mem_total.dataAvg();
        var edges     = Array.sum(msg.workerStatistics.numberOfOutgoingEdges);
        var vertices  = Array.sum(msg.workerStatistics.numberOfVertices);
        var fraction  = maxMemory / avgMemory;
        var message   = "The current graph has got " + vertices + " vertices and " + edges + " edges."
          + "<br><br>Based on the current memory consumption, your infrastructure could "
          + "handle a similar graph which is approximately " + Math.round(fraction) + " times bigger (i.e. ≈"
          + Math.floor(fraction * vertices) + " vertices and ≈" + Math.floor(fraction * edges) + " edges).";
        $("#estimationStatBox p").html(message);
        estimationsLastUpdated = new Date(msg.timestamp);
      }
    }
    
    scc.order({"provider": "resources"}, scc.conf.resources.intervalCharts);
  }

}

