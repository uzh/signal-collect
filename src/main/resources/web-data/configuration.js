/*
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
 *  
 */

/**
 * The project container variable scc which appears in the global namespace
 * @namespace
 * @property {object} modules - The module classes that can be instantiated.
 * @property {object} consumers - The instantiated modules
 * @property {object} defaults - The default settings of each module
 * @property {object} orders - The pending orders, zero or one for each consumer
 * @property {object} callbacks - Zero or one callback for each consumer to be
 *     called once a reply has been received from the server
 * @property {object} conf - Container for the configurable parameters
 */
var scc = {"modules": {}, "consumers": {}, "defaults": {}, "orders": {}, 
           "callbacks": {}, "conf": {}};

/**
 * Container for the configurable parameters
 * @property {object} graph - Container for the configurable graph parameters
 * @property {object} resources - Container for the configurable resources parameters
 */
scc.conf = {"graph": {}, "resources": {}};

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
    "detailed" : [
      "chartZoomer",
      // all charts will be added automatically to this section
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
 * Configures the main chart types. This is not mandatory, but they load faster
 * if you add them here. You can also add different data callbacks or skip
 * entire objects.
 * @type {Array.<Objects>}
 */
scc.conf.resources.chartConfig = [
                   {jsonName : "messagesSent", dataCallback: sumMessageSent },
                   {jsonName : "messagesSentToNodes"},
                   {jsonName : "messagesSentToWorkers"},
                   {jsonName : "messagesSentToCoordinator"},
                   {jsonName : "messagesSentToOthers"},
                   {jsonName : "messagesReceived", dataCallback: sumMessageReceived },
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
