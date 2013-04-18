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
 */
scc.defaults.resources = {"layout":{
                            "cResourceComputation": "show",
                            "cResourceProblems": "show"
                          },
                          "section": "statistics"
};


// configure which content box to show in which section
var resourceBoxes = {
    "statistics": [
      "infrastructureStatBox",
      "computationStatBox",
      "graphStatBox",
      "estimationStatBox"
      ],
    "logs"      : [ "logBox" ],
    "detailed" : [
      "chartZoomer",
      // all charts will be added automatically
     ],
    
    "nostart"   : [ 
      "chartZoomer",
      "logTitle",
      "logBox",
      "signalCollectTitle",
      "heartbeatMessagesReceivedChart",
      "messagesSentChart",
      "messagesReceivedChart",
      "signalMessagesReceivedChart",
      "signalOperationsExecuted",
      "collectOperationsExecuted",
      "toCollectSizeChart",
      "toSignalSizeChart",
      "infrastructureTitle",
      "jmx_system_loadChart",
      "jmx_process_timeChart",
      "jmx_process_loadChart",
      "jmx_swap_totalChart",
      "jmx_swap_freeChart",
      "jmx_mem_totalChart",
      "jmx_mem_freeChart",
      "jmx_commited_vmsChart",
      "runtime_mem_freeChart",
      "runtime_mem_maxChart",
      "runtime_mem_totalChart"
    ],
    "noconvergence" : [
      "chartZoomer",
      "signalCollectTitle",
      "messagesSentChart",
      "messagesReceivedChart",
    ], 
    "estimation" : [
      "chartZoomer",
      "estimationStatBox",
      "infrastructureTitle",
      "runtime_mem_freeChart",
      "runtime_mem_maxChart",
      "runtime_mem_totalChart"
    ],
    "crash" : [
      "chartZoomer",
      "logTitle",
      "logBox",
      "infrastructureTitle",
      "jmx_mem_freeChart",
      "runtime_mem_freeChart",
      "runtime_mem_maxChart",
      "runtime_mem_totalChart"
    ],
    "slow" : [
      "chartZoomer",
      "infrastructureTitle",
      "jmx_system_loadChart",
      "jmx_process_timeChart",
      "jmx_process_loadChart",
      "jmx_swap_totalChart",
      "jmx_swap_freeChart",
      "jmx_mem_totalChart",
      "jmx_mem_freeChart",
      "jmx_commited_vmsChart",
      "runtime_mem_freeChart",
      "runtime_mem_maxChart",
      "runtime_mem_totalChart"
    ],
};





/**
 * Panel functionality
 */
$(window).on('hashchange', function() {
  scc.settings.reload();
  var settings = scc.settings.get();
  if (settings.resources.section != null) {
    show_section(settings.resources.section);
  }
});
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
function show_boxes(s) {
  var boxes = "#resourceBoxes";
  // first, hide all of them
  $(boxes + " > div").attr("class", "hidden");
  if (resourceBoxes[s] == null) { return; }
  // then only show the ones that are needed
  resourceBoxes[s].forEach(function(v) {
    var resourceBox = boxes + " > #" + v;
    $(resourceBox).removeClass("hidden");
    $(resourceBox).appendTo(boxes); // change order
  });
}
$(document).ready(function() {
  $("#resources_panel_container label").click(function() {
    section = $(this).attr("for").split("_")[1];
    scc.settings.set({'resources':{'section':section}});
  });
});

/**
 * The Configuration module ...
 * TODO: elaborate (see graph.js or breakconditions.js for example intro text)
 * @constructor
 */
scc.modules.Configuration = function() {
  this.requires = ["configuration"];

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests data from the ConfigurationProvider.
   * @param e {Event} - The event that triggered the call.
   */
  this.onopen = function () {
    scc.order({"provider": "configuration"});
  }
    
  /**
   * Function that is called by the main module when a WebSocket error is
   * encountered. Does nothing.
   * @param e {Event} - The event that triggered the call.
   */
  this.onerror = function(e) { }

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Does nothing.
   * @param e {Event} - The event that triggered the call.
   */
  this.notready = function() { }

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. Does nothing.
   * @param e {Event} - The event that triggered the call.
   */
  this.onclose = function() { }

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. TODO: elaborate
   * @param j {object} - The message object received from the server.
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
 * The Log module ...
 * TODO: elaborate (see graph.js or breakconditions.js for example intro text)
 * @constructor
 */
scc.modules.Log = function() {
  this.requires = ["log"];
  
  var latest, identicalLogMessages = 0;
  var container = $("#resourceBoxes #logBox");
  var box = $(container).find("div.scroll");
  var boxInner = $(box).find("div");
  var maxDebugMessages = 200;
  
  var logLevelIndex  = { "error":1, "warning":2, "info":3, "debug":4 };
  var filterLevel    = $(container).find("p.filter.level");
  var logSourceIndex = { "akka":1, "sc":2, "user":3 };
  var filterSource   = $(container).find("p.filter.source");
  
  // hide and show log messages based on their level
  $.each(logLevelIndex, function(v, k) {
    $(filterLevel).find("> span:eq(" + (k-1) + ")").on("click", function() {
      $(this).toggleClass("active");
      $(box).find("li.level_" + v).toggleClass("hidden_level");
    });
  });
  
  // hide and show log messages based on their level
  $.each(logSourceIndex, function(v, k) {
    $(filterSource).find("> span:eq(" + (k-1) + ")").on("click", function() {
      $(this).toggleClass("active");
      $(box).find("li.source_" + v).toggleClass("hidden_source");
    });
  });

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests data from the ConfigurationProvider.
   * @param e {Event} - The event that triggered the call.
   */
  this.onopen = function () {
    // make it using the full height
    onResize = (function() {
      $("body.logs div#logBox div.scroll").css("height", ($(window).height() - 280) + "px");
    });
    $(document).ready(onResize);
    $(window).resize(onResize);

    scc.order({"provider": "log"});
  }

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. TODO: elaborate
   * @param j {object} - The message object received from the server.
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
 * Show statistics and draw charts
 */
scc.modules.Resources = function() {
  this.requires = ["resources"];
  show_section(scc.settings.get().resources.section);
  
  // fill detailed view with all the charts we have
  setTimeout(function() {
    $("#resourceBoxes div[id$='Chart']").each(function() {
      resourceBoxes.detailed.push($(this).attr("id"));
    });
  }, 500);
  
    
  // statistics
  $("#resStatInterval").html(scc.conf.resources.intervalStatistics / 1000);
  var statisticsLastUpdated = new Date(0);

  var hasAddedNewCharts = false;
  var estimationsLastUpdated = new Date(0);
  
  
  var sumSubArray = function(data) {
    return data.workerStatistics.messagesSent.map(function(array) {
      return Array.sum(array);
    });
  };
  
  var chartConfig = [
                     {jsonName : "messagesSent", dataCallback: sumSubArray },
                     {jsonName : "messagesReceived"},
                     {jsonName : "signalMessagesReceived"},
                     {jsonName : "otherMessagesReceived"},
                     {jsonName : "requestMessagesReceived"},
                     {jsonName : "continueMessagesReceived"},
                     {jsonName : "bulkSignalMessagesReceived"},
                     {jsonName : "heartbeatMessagesReceived"},
                     {jsonName : "receiveTimeoutMessagesReceived"},
                     {jsonName : "outgoingEdgesAdded"},
                     {jsonName : "outgoingEdgesRemoved"},
                     {jsonName : "numberOfOutgoingEdges"},
                     {jsonName : "verticesRemoved"},
                     {jsonName : "verticesAdded"},
                     {jsonName : "numberOfVertices"},
                     {jsonName : "signalOperationsExecuted"},
                     {jsonName : "collectOperationsExecuted"},
                     {jsonName : "toCollectSize"},
                     {jsonName : "toSignalSize"},
                     {jsonName : "workerId"},
                     {jsonName : "runtime_cores"},
                     {jsonName : "jmx_system_load"},
                     {jsonName : "jmx_process_time"},
                     {jsonName : "jmx_process_load"},
                     {jsonName : "jmx_swap_free"},
                     {jsonName : "jmx_swap_total"},
                     {jsonName : "jmx_mem_total"},
                     {jsonName : "jmx_mem_free"},
                     {jsonName : "jmx_committed_vms"},
                     {jsonName : "runtime_mem_max"},
                     {jsonName : "runtime_mem_free"},
                     {jsonName : "runtime_mem_total"},
                     {jsonName : "os", skip: true },
                    ];
  var lineCharts = {};
  
  var ChartsCreate = function(config) {
    var lineChart = new LineChart();
    lineChart.container = "#resourceBoxes";
    lineChart.setup(config);
    lineCharts[lineChart.config.jsonName] = lineChart;
  }
  
  chartConfig.forEach(function(config) {
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
  var moving = function(scale) {
    $.each(lineCharts, function(key, chart) {
      chart.setMove(scale);
    });
  }
  
  
  // event handler
  
  this.onopen = function () {
    scc.order({"provider": "resources"})
  }

  /**
   * Function that is called by the main module when a WebSocket error is
   * encountered. Does nothing.
   * @param e {Event} - The event that triggered the call.
   */
  this.onerror = function(e) { }

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Does nothing.
   * @param e {Event} - The event that triggered the call.
   */
  this.notready = function() { }

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. Does nothing.
   * @param e {Event} - The event that triggered the call.
   */
  this.onclose = function() { }

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. TODO: elaborate
   * @param j {object} - The message object received from the server.
   */

  var ChartsContains = function(key) {
    var found = false;
    chartConfig.forEach(function(c) {
      if (c.jsonName == key) {
        found = true;
        return found;
      }
    });
    return found;
  }

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. TODO: elaborate
   * @param j {object} - The message object received from the server.
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

