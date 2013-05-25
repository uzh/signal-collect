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
  };
  
  
  
  /**
   * Handles state changes of the interactive execution (e.g. resetting, converged, ...).
   * @param {Object} msg - The message including the state
   */
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
