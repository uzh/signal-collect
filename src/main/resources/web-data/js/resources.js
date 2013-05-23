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
 * Object container that encapsulates all chart objects.
 * @type {Object}
 */
scc.lib.resources.lineCharts = {};


/**
 * Event handler to catch onHashChange events to update the current section.
 */
$(window).on('hashchange', function() {
  scc.settings.reload();
  var settings = scc.settings.get();
  if (settings.resources.section != null) {
    scc.lib.resources.show_section(settings.resources.section);
  }
});

/**
 * Hides all sections and shows only the given section.
 * @param {string} s - The name of the section to show.
 */
scc.lib.resources.show_section = function(s) {
  if (s == "") { return; }
  // hide all sections
  $("#resources .structured > div[id^=\"crs\"]").hide();
  // show the appropriate section
  $("#crs_" + s).show();
  scc.lib.resources.show_boxes(s);
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
scc.lib.resources.show_boxes = function(s) {
  var boxes = "#resourceBoxes";
  // first, hide all of them
  $(boxes + " > div").attr("class", "hidden");
  if (scc.conf.resources.resourceBoxes[s] == null) { return; }
  // then only show the ones that are needed
  scc.conf.resources.resourceBoxes[s].forEach(function(v) {
    var resourceBox = boxes + " > #" + v;
    $(resourceBox).removeClass("hidden");
    $(resourceBox).appendTo(boxes); // change order
    if (v.endsWith("Chart")) {
      scc.lib.resources.update_chart(v.slice(0, -5));
    }
  });
}

/**
 * Updates a chart when it is in viewport.
 * @param {string} chart - The name of the chart to update.
 */
scc.lib.resources.update_chart = function(chart) {
  if (!scc.lib.resources.lineCharts.hasOwnProperty(chart)) {
    return;
  }
  if (scc.lib.resources.lineCharts[chart].isOverlappingViewport()) {
    scc.lib.resources.lineCharts[chart].updateChart();
  }
}

/**
 * Event handler that gets called when the DOM is ready.
 */
$(document).ready(function() {
  $("#resources_panel_container label").click(function() {
    var section = $(this).attr("for").split("_")[1];
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
    scc.order({"requestor": "Resources", "provider": "configuration"});
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
    if (msg.executionConfiguration != "unknown") {
      $.each(msg.executionConfiguration, function(k,v) {
        $("#resStat" + k).html(v);
      });
    }
    var ul = $("#infrastructureStatBox ul").html('');
    $.each(msg.systemProperties, function(index) {
      $.each(msg.systemProperties[index], function(k, v) {
        ul.append('<li>' + k + ': ' + v + '</li>');
      });
    });
    ul = $("#graphStatBox ul").html('');
    $.each(msg.graphConfiguration, function(k, v) {
      if (k == "consoleHttpPort" && v[0] == -1) {
        v[0] = location.port;
      }
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
    box.css("height", ($(window).height() - 240) + "px");
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



/**
 * The Resources module handles the chart drawing and all actions which can be
 * done on charts (e.g. zooming and shifting). It also handles the calculation
 * of the data set size estimation, and some statistics about the computation.
 * @constructor
 */
scc.modules.Resources = function() {
  this.requires = ["resources", "state"];
  scc.lib.resources.show_section(scc.settings.get().resources.section);
  
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
   * Helper function to create a chart and store it to the chart container.
   * @param {Object} config - The configuration to use to create the chart.
   */
  var ChartsCreate = function(config) {
    var lineChart = new scc.lib.resources.LineChart();
    lineChart.container = "#resourceBoxes";
    lineChart.setup(config);
    scc.lib.resources.lineCharts[lineChart.config.jsonName] = lineChart;
  }
  
  scc.conf.resources.chartConfigWorkers.forEach(function(config) {
    if (!config.skip) {
      ChartsCreate(config);
      if (scc.conf.resources.resourceBoxes.workercharts.indexOf(config.jsonName + "Chart") === -1) {
        scc.conf.resources.resourceBoxes.workercharts.push(config.jsonName + "Chart");
      }
    }
  });
  scc.conf.resources.chartConfigNodes.forEach(function(config) {
    if (!config.skip) {
      ChartsCreate(config);
      if (scc.conf.resources.resourceBoxes.nodecharts.indexOf(config.jsonName + "Chart") === -1) {
        scc.conf.resources.resourceBoxes.nodecharts.push(config.jsonName + "Chart");
      }
    }
  });
  
  // update boxes (needed to show the charts on reload)
  setTimeout(function() {
    scc.lib.resources.show_boxes(scc.settings.get().resources.section);
  }, 600);
  

  // zooming buttons
  $("#chartZoomer .zoomIn").click(function() {
    console.debug("Zoom: In");
    zooming(1.2);
  });
  $("#chartZoomer .zoomOut").click(function() {
    console.debug("Zoom: Out");
    zooming(0.8);
  });
  
  /**
   * Helper function to zoom all charts in or out.
   * @param {number} scale - The scale to which to zoom in or out.
   */
  var zooming = function(scale) {
    $.each(scc.lib.resources.lineCharts, function(key, chart) {
      chart.setZoom(scale);
    });
  }
  

  // moving buttons
  $("#chartZoomer .moveLeft").click(function() {
    console.debug("Move: Left");
    moving(-1);
  });
  $("#chartZoomer .moveRight").click(function() {
    console.debug("Move: Right");
    moving(1);
  });
  $("#chartZoomer .moveOrigin").click(function() {
    console.debug("Move: Origin");
    moving(0);
  });

  /**
   * Helper function to shift all charts left, right, or to the origin.
   * @param {number} scale - The scale to which to shift to.
   */
  var moving = function(scale) {
    $.each(scc.lib.resources.lineCharts, function(key, chart) {
      chart.setMove(scale);
    });
  }
  
  
  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests data from the ResourceProvider.
   */
  this.onopen = function () {
    scc.order({"requestor": "Resources", "provider": "resources"})
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
   * breaks down. Redraws all charts so that they show current data.
   */
  this.onclose = function() {
    $.each(scc.lib.resources.lineCharts, function(chartKey) {
      scc.lib.resources.lineCharts[chartKey].updateChart();
    });
  }

  /**
   * Helper function to check whether {@code chartConfig} contains a chart with
   * the given key.
   * @param {object} key - The key name to check whether there is such a chart
   *     stored already.
   */
  var ChartsContains = function(key) {
    var found = false;
    scc.conf.resources.chartConfigWorkers.forEach(function(c) {
      if (c.jsonName == key) {
        found = true;
        return found;
      }
    });
    if (!found) {
      scc.conf.resources.chartConfigNodes.forEach(function(c) {
        if (c.jsonName == key) {
          found = true;
          return found;
        }
      });
    }
    return found;
  }

  
  /**
   * Selector of the computation start time.
   * @type {Object}
   */
  var resStatStartTime = $("#resStatStartTime");

  /**
   * Selector of the computation run time.
   * @type {Object}
   */
  var resStatRunTime = $("#resStatRunTime");

  /**
   * Selector of the number of workers.
   * @type {Object}
   */
  var resStatWorkers = $("#resStatWorkers");

  /**
   * The current state of the computation.
   * @type {string}
   */
  var computationState = "";
  
  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. A new message is distributed to the several charts
   * which then update their visualization. This function also updates the
   * statistics and recalculates the data set size estimation from time to time.
   * @param {object} msg - The message object received from the server.
   */
  this.onmessage = function(msg) {
    
    // check whether it is a state change message
    if (msg.provider == "state") {
      this.handleStateChange(msg);
      return;
    }
    
    // check if there is more data in the websocket response
    if (!hasAddedNewCharts) {
      var chartSkipped = function(resource, chart) {
        if (scc.conf.resources[resource].hasOwnProperty(chart)) {
          console.dir(scc.conf.resources[resource][chart].skip);
          if (scc.conf.resources[resource][chart].skip) {
            return true;
          }
        }
        return false;
      };
      for (var key in msg.workerStatistics) {
        if (!ChartsContains(key) && !chartSkipped("chartConfigWorkers", key)) {
          ChartsCreate({jsonName : key});
          scc.conf.resources.resourceBoxes.workercharts.push(key + "Chart");
        }
      }
      for (var key in msg.nodeStatistics) {
        if (!ChartsContains(key) && !chartSkipped("chartConfigNodes", key)) {
          ChartsCreate({jsonName : key});
          scc.conf.resources.resourceBoxes.nodecharts.push(key + "Chart");
        }
      }
      hasAddedNewCharts = true;
    }
    
    // update all graphs
    $.each(scc.lib.resources.lineCharts, function(k,v) { v.update(msg); });
    

    if (computationState != "converged") {
    
      // update statistics
      if (statisticsLastUpdated.addMilliseconds(scc.conf.resources.intervalStatistics) <= msg.timestamp) {
        if (resStatStartTime.html() == "?") {
          resStatStartTime.html(new Date(msg.timestamp).dateTime());
        }
        resStatRunTime.html(new Date(msg.timestamp-(new Date(resStatStartTime.html()))).durationPretty());
        resStatWorkers.html(msg.workerStatistics.workerId.length);
        statisticsLastUpdated = new Date(msg.timestamp);
      }
      
      // update estimations
      if (estimationsLastUpdated.addMilliseconds(scc.conf.resources.intervalEstimation) <= msg.timestamp) {
        if (scc.lib.resources.lineCharts.runtime_mem_total.dataLength() >= 10) {
          var maxMemory = scc.lib.resources.lineCharts.runtime_mem_max.dataLatest();
          var avgMemory = scc.lib.resources.lineCharts.runtime_mem_total.dataAvg();
          var edges     = Array.sum(msg.workerStatistics.numberOfOutgoingEdges);
          var vertices  = Array.sum(msg.workerStatistics.numberOfVertices);
          var fraction  = maxMemory / avgMemory;
          var message   = "The current graph has got " + vertices + " vertices and " + edges + " edges."
            + "<br><br>Based on the current memory consumption, your infrastructure could handle a similar "
            + "graph which has approximately " + Math.round(fraction*100)/100 + " times that size (i.e. ≈"
            + Math.floor(fraction * vertices) + " vertices and ≈" + Math.floor(fraction * edges) + " edges).";
          $("#estimationStatBox p").html(message);
          estimationsLastUpdated = new Date(msg.timestamp);
        }
      }
    
    }
    
    scc.order({"requestor": "Resources", "provider": "resources"}, scc.conf.resources.intervalCharts);
  }
  
  
  
  this.handleStateChange = function(msg) {
    
    if (computationState == "" || msg.state == "resetting") {
      $.each(scc.lib.resources.lineCharts, function(key, chart) {
        chart.addComputationState("reset");
      });
    }
    
    if (msg.state == "resetting") {
      // re-initialize statistics
      resStatStartTime.html(new Date().dateTime());
      resStatRunTime.html("0"); 
    }
    
    if (msg.state == "converged") {
      $.each(scc.lib.resources.lineCharts, function(key, chart) {
        chart.addComputationState("converge");
      });
    }

    computationState = msg.state;
  };

}

