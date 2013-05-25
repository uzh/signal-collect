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
 * Helper function to sum up the individual elements of two arrays.
 * @param {Array.<number>} sum - The array to increase and return.
 * @param {Array.<number>} array - The array to get the numbers to sum up from.
 * @return {Array.<number>} - An array with summed up individual array elements.
 */
Array.sumElements = function(sum, array) {
  array.forEach(function(num, index) {
    if (sum[index] == undefined) {
      sum[index] = 0;
    }
    sum[index] = sum[index] + num;
  });
  return sum;
};

/**
 * Specific dataCallback function to sum up the individual messageSent
 * statistics.
 * @param {Object} data - The data object that will be looked at.
 * @return {Array.<number>} - The summed array values.
 */
scc.lib.resources.sumMessageSent = function(data) {
  var statistics = [ "messagesSentToNodes",
                     "messagesSentToWorkers",
                     "messagesSentToCoordinator",
                     "messagesSentToOthers" ];
  var numValues = data.workerStatistics[statistics[0]].length;
  var sum = new Array(numValues);
  statistics.forEach(function(sentTo){
    sum = Array.sumElements(sum, data.workerStatistics[sentTo]);
  });
  return sum;
};

/**
 * Specific dataCallback function to sum up the individual messageReceived
 * statistics.
 * @param {Object} data - The data object that will be looked at.
 * @return {Array.<number>} - The summed array values.
 */
scc.lib.resources.sumMessageReceived = function(data) {
  var statistics = [ "otherMessagesReceived",
                     "requestMessagesReceived",
                     "signalMessagesReceived",
                     "receiveTimeoutMessagesReceived",
                     "bulkSignalMessagesReceived",
                     "continueMessagesReceived",
                     "heartbeatMessagesReceived" ];
  var numValues = data.workerStatistics[statistics[0]].length;
  var sum = new Array(numValues);
  statistics.forEach(function(receivedFrom){
    sum = Array.sumElements(sum, data.workerStatistics[receivedFrom]);
  });
  return sum;
};

/**
 * Specific dataCallback function to calculate the used memory.
 * @param {Object} data - The data object that will be looked at.
 * @return {Array.<number>} - The summed array values.
 */
scc.lib.resources.getUsedMemory = function(data) {
  var usedMemory = [];
  for (var i=0; i<data.nodeStatistics.runtime_mem_total.length; i++) {
    usedMemory[i] = data.nodeStatistics.runtime_mem_total[i] - data.nodeStatistics.runtime_mem_free[i];
  }
  return usedMemory;
};

/**
 * Specific dataCallback function to calculate the free memory.
 * @param {Object} data - The data object that will be looked at.
 * @return {Array.<number>} - The summed array values.
 */
scc.lib.resources.getFreeMemory = function(data) {
  var usedMemory = scc.lib.resources.getUsedMemory(data);
  var freeMemory = [];
  for (var i=0; i<data.nodeStatistics.runtime_mem_free.length; i++) {
    freeMemory[i] = data.nodeStatistics.runtime_mem_max[i] - usedMemory[i];
  }
  return freeMemory;
};

/**
 * Specific dataCallback function to the CPU usage per node.
 * @param {Object} data - The data object that will be looked at.
 * @return {Array.<number>} - The summed array values.
 */
scc.lib.resources.getSystemLoad = function(data) {
  var systemLoad = [];
  for (var i=0; i < data.nodeStatistics.jmx_system_load.length; i++) {
    systemLoad[i] = scc.lib.resources.getSum(data.nodeStatistics.jmx_system_load);
  }
  return systemLoad;
};

/**
 * Specific dataCallback function to the CPU Time per node.
 * @param {Object} data - The data object that will be looked at.
 * @return {Array.<number>} - The summed array values.
 */
scc.lib.resources.getProcessTime = function(data) {
  var cpuTime = [];
  for (var i=0; i < data.nodeStatistics.jmx_process_time.length; i++) {
    cpuTime[i] = scc.lib.resources.getSum(data.nodeStatistics.jmx_process_time);
  }
  return cpuTime;
};

/**
 * Specific dataCallback function to the Process Load per node.
 * @param {Object} data - The data object that will be looked at.
 * @return {Array.<number>} - The summed array values.
 */
scc.lib.resources.getProcessLoad = function(data) {
  var processLoad = [];
  for (var i=0; i < data.nodeStatistics.jmx_process_load.length; i++) {
    processLoad[i] = scc.lib.resources.getSum(data.nodeStatistics.jmx_process_load);
  }
  return processLoad;
};

/**
 * dataCallback helper function to sum up array elements.
 * @param {Object} data - The data object that will be looked at.
 * @return {number} - The summed array values.
 */
scc.lib.resources.getSum = function(data) {
  var sum = 0;
  for (var i=0; i < data.length; i++) {
    sum += data[i];
  }
  return sum;
};

/**
 * Function to create a fake date based on a duration.
 * @param {number} d - The duration to format.
 * @return {callback} - The format of the duration.
 */
scc.lib.resources.tickDuration = function(d) {
  return new Date(d/(1000*1000)).durationRounded();
}

/**
 * Function to create a number format with a trailing "B" for byte.
 * @param {number} d - The number to format.
 * @return {callback} - The format of the byte size.
 */
scc.lib.resources.formatBytes = function(d) {
  var format = d3.formatPrefix(d)
  return format.scale(d) + format.symbol + "B";
}

/**
 * Calculates the duration in the format "1h 2m" given a {@code Date}
 * object whereas it rounds the values and only shows to units.
 * @return {String} - Pretty printed duration in the format "1h 2m". 
 */
Date.prototype.durationRounded = function() {
  var ms = +this;
  var units = 0; // number of units added to the duration
  var duration  = "";
  var durations = {h:60*60*1000, m:60*1000, s:1000, ms:1};
  $.each(durations, function(k, v) {
    if (ms / v >= 1 && units <= 2) {
      duration += " " + Math.floor(ms / v) + k;
      ms = ms % v; // store the rest
      units += 1;
    }
  });
  return (duration.length > 0 ? duration : "0");
};

/**
 * Function that returns true when the passed DOM element is in the viewport,
 * false otherwise.
 * @param {Object} el - The element to check whether it is in the viewport.
 * @return {boolean} - Whether or not the passed DOM element is in the viewport.
 */ 
function isElementOverlappingViewport(el) {
  var rect = el.getBoundingClientRect();
  return (
      rect.top >= -600 &&
      rect.left >= 0 &&
      rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) + 600 && /*or $(window).height() */
      rect.right <= (window.innerWidth || document.documentElement.clientWidth) /*or $(window).width() */
      );
}

/**
 * Returns whether or not a string ends with the passed suffix.
 * @param {string} suffix - The suffix to check the end of the string for.
 * @return {boolean} - Whether or not the string ends with suffix.
 */
String.prototype.endsWith = function(suffix) {
  return this.indexOf(suffix, this.length - suffix.length) !== -1;
};