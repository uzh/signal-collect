/**
 *  @author Carol Alexandru
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
 * Interval in milliseconds between two consecutive chart updates.
 * @type {number}
 */
scc.conf.resources.intervalCharts = 3000;

/**
 * Interval in milliseconds between two consecutive statistic updates.
 * @type {number}
 */
scc.conf.resources.intervalStatistics = 6000;

/**
 * Interval in milliseconds between two consecutive estimation updates.
 * @type {number}
 */
scc.conf.resources.intervalEstimation = 20*1000;

/**
 * Interval in milliseconds between two consecutive log message updates.
 * @type {number}
 */
scc.conf.resources.intervalLogs = 2000;

/**
 * Configures which resource box to show in which section.
 * @type {Object}
 */
scc.conf.resources.resourceBoxes = {
    "statistics": [
      "infrastructureStatBox",
      "computationStatBox",
      "graphStatBox",
      "estimationStatBox"
      ],
    "logs" : [
      "logBox"
    ],
    "nodecharts" : [
      "chartZoomer",
      // charts will be added according to the chart configuration
    ],
    "workercharts" : [
      "chartZoomer",
      // charts will be added according to the chart configuration
    ],
    
    "nostart" : [ 
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
      "infrastructureTitle",
      "jmx_system_loadChart",
      "jmx_process_timeChart",
      "jmx_process_loadChart",
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
 * Configures the main chart types for workers. This is not mandatory, but they
 * load faster if you add them here. You can also add different data callbacks
 * or skip entire objects.
 * @type {Array.<Objects>}
 */
scc.conf.resources.chartConfigWorkers = [
                   {jsonName : "messagesSent", dataCallback: scc.lib.resources.sumMessageSent },
                   {jsonName : "messagesSentToNodes"},
                   {jsonName : "messagesSentToWorkers"},
                   {jsonName : "messagesSentToCoordinator"},
                   {jsonName : "messagesSentToOthers"},
                   {jsonName : "messagesReceived", dataCallback: scc.lib.resources.sumMessageReceived },
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
                   {jsonName : "workerId", skip: true },
                  ];

/**
 * Configures the main chart types for nodes. This is not mandatory, but they
 * load faster if you add them here. You can also add different data callbacks
 * or skip entire objects.
 * @type {Array.<Objects>}
 */
scc.conf.resources.chartConfigNodes = [
                   {jsonName : "runtime_cores",
                     unit:"#" },
                   {jsonName : "jmx_system_load",
                     format: d3.format("p"),
                     formatTT: d3.format(".5p") },
                   {jsonName : "jmx_system_load_node",
                     dataCallback: scc.lib.resources.getSystemLoad,
                     format: d3.format("p"),
                     formatTT: d3.format(".5p") },
                   {jsonName : "jmx_process_time",
                     format: scc.lib.resources.tickDuration },
                   {jsonName : "jmx_process_time_node",
                     dataCallback: scc.lib.resources.getProcessTime,
                     format: scc.lib.resources.tickDuration },
                   {jsonName : "jmx_process_load", 
                     format: d3.format("p"),
                     formatTT: d3.format(".5p") },
                   {jsonName : "jmx_process_load_node",
                     dataCallback: scc.lib.resources.getProcessLoad, 
                     format: d3.format("p"),
                     formatTT: d3.format(".5p") },
                   {jsonName : "jmx_swap_free",
                     format: scc.lib.resources.formatBytes },
                   {jsonName : "jmx_swap_total",
                     format: scc.lib.resources.formatBytes },
                   {jsonName : "jmx_mem_total",
                     format: scc.lib.resources.formatBytes },
                   {jsonName : "jmx_mem_free",
                     format: scc.lib.resources.formatBytes },
                   {jsonName : "jmx_committed_vms",
                     format: scc.lib.resources.formatBytes },
                   {jsonName : "runtime_mem_max",
                     format: scc.lib.resources.formatBytes },
                   {jsonName : "runtime_mem_free",
                     format: scc.lib.resources.formatBytes,
                     dataCallback: scc.lib.resources.getFreeMemory },
                   {jsonName : "runtime_mem_total",
                     format: scc.lib.resources.formatBytes,
                     dataCallback: scc.lib.resources.getUsedMemory },
                   {jsonName : "os", skip: true },
                   {jsonName : "nodeId", skip: true },
                  ];

/**
 * Configures the infrastructure configuration items which should NOT be shown
 * in the console server as they are irrelevant.
 * @type {Array.<String>}
 */
scc.conf.resources.hideInfrastructureItems = [
  "sun.os.patch.level",
  "java.vendor.url.bug",
  "sun.cpu.isalist",
  "gopherProxySet",
  "java.awt.graphicsenv",
  "java.awt.printerjob",
  "awt.toolkit"
];