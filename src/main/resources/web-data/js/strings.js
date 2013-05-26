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
 * Objects which contains Strings needed in the UI of BreakConditions, Graph, and State.
 * @type {Object}
 */
scc.STR = {
       "BreakConditions": {
         "invalidId": "A vertex with the ID you provided does not exist",
         "invalidState": "The state you provided is invalid. It must be something that can be parsed as a double",
         "unknownError": "Unknown validation error: ",
         "noConditions": "No conditions specified",
         "noExecution": "The interactive execution mode is unavailable, retrying...",
         "pickVertex": "Enter full ID or select using mouse",
         "enterState": "Enter state",
         "stateChanges": "state changes",
         "stateAbove": "state above",
         "stateBelow": "state below",
         "signalScoreAboveThreshold": "signal score above threshold",
         "signalScoreBelowThreshold": "signal score below threshold",
         "collectScoreAboveThreshold": "collect score above threshold",
         "collectScoreBelowThreshold": "collect score below threshold"
       },
       "Graph": {
         "expositionEmpty":
           "Click on a vertex to expose its details. Click and drag the border of this box to make it larger or smaller.",
         "canvasEmpty":
           "Canvas is empty: use the tools on the left to add and remove vertices. "  +
           "Older vertices will automatically be removed once the maximum vertex count is reached.",
         "addBySubstring": 
           "Vertex with ID having this substring",
         "menuhelp": {
           "gs_autoAddVicinities": "When enabled, causes the vicinity of a vertex to be loaded automatically when the mouse is hovering over it.",
           "gp_targetCount": "Add at most the specified number of vertices sorted by the given property.",
           "gs_vicinities": "Add the vicinity of vertices that are already on the canvas",
           "gs_addRecentVicinitiesAdd": "Add only the vicinity of vertices from the latest query",
           "gs_addAllVicinitiesAdd": "Add the vicinity of all vertices on the canvas. You may exceed the maximum vertex count quickly doing this.",
           "gs_addVicinitiesBySelect": "Click here and then draw a rectangle around the vertices whose vicinity you want to add.",
           "gd_removeNonLatest": "Remove all vertices that are not from the most recent query.",
           "gd_removeOrphans": "Remove vertices that are not connected to any other vertices on the canvas. Note that they may have edges with vertices not added onto the canvas.",
           "gd_removeAll": "Remove all vertices from the canvas.",
           "gd_removeBySelect": "Click here and then draw a rectangle around the vertices to be removed.",
           "gp_vicinityRadius": "This determines how many hops the query travels from the source when traversing the vicinity of a vertex.",
           "gp_vicinityIncoming": "Traversing outgoing vicinities is much cheaper than calculating incoming ones. This may affect the performance of your queries",
           "gp_refreshRate": "When running the computation continously, this determines the minimum time to wait between refreshing the graph.",
           "gp_maxVertexCount": "The maximum number of vertices to request from the server when doing queries or adding vicinities.",
           "gp_exposeVertices": "When enabled, calls the 'expose' function of each vertex being loaded and shows the results in the side bar.",
           "gp_drawEdges": "Wether or not to draw the edges between vertices. 'Only on hover' may improve the performance for large graphs.",
           "gd_vertexSize": "Wether or not to map the state of a vertex to its size.",
           "gd_vertexColor": "Determines the source data to use for the vertex color. Where it is a gradient, blue shades indicate low values, red shades high values.",
           "gd_vertexBorder": "Determines the source data to use for the vertex border color. Where it is a gradient, blue shades indicate low values, red shades high values.",
           "gc_vertexId": "Click on 'Select' and then pick a vertex on the canvas. Then, choose a condition and click 'Add' to create the new break condition."
         }
       },
       "State": {
         "undetermined": [
           "State undetermined...",
           "The execution mode and state have not been determined, yet"],
         "initExecution": [
           "Loading interactive execution...",
           "The interactive execution is being initilized"],
         "pausedBeforeChecksAfterCollect": [
           "Paused bef. checks after collect",
           "Paused - The next partial step triggers a break condition check"],
         "checksAfterCollect": [
           "Check conditions after collect...",
           "Checking conditions before the next signal step..."],
         "pausedBeforeSignal": [
           "Paused before signal",
           "Paused - The next partial step will perform the signalling step of the computation"],
         "signalling": [
           "Signalling...",
           "Performing the signalling step of the computation..."],
         "pausedBeforeChecksAfterSignal": [
           "Paused before checks after signal",
           "Paused - The next partial step triggers a break condition check"],
         "checksAfterSignal": [
           "Check conditions after signal...",
           "Checking conditions before the next collect step..."],
         "pausedBeforeCollect": [
           "Paused before collect",
           "Paused - The next partial step will perform the collecting step of the computation"],
         "collecting": [
           "Collecting...",
           "Performing the collecting step of the computation..."],
         "pausedBeforeGlobalChecks": [
           "Paused before global checks",
           "Paused - The next partial step triggers the global break condition check"],
         "globalChecks": [
           "Checking global conditions...",
           "Checking global termination conditions..."],
         "globalConditionReached": [
           "Global term. condition reached!",
           "The global termination condition has been reached. You may reset the graph to start over."],
         "resetting": [
           "Resetting graph...",
           "Resetting the graph to its initial state"],
         "terminating": [
           "Terminating Signal/Collect...",
           "Terminating the Signal/Collect application"],
         "pausing": [
           "Pausing...",
           "Pausing the computation, waiting for tasks to finish"],
         "converged": [
           "Computation converged!",
           "The computation has converged. You may reset the graph to start over."]
       }
       
};

/**
 * Configure the pretty name of the charts based on their name from the JSON object.
 * @type {Object}
 */
scc.lib.resources.chartInfo = {
    "messagesSent": {
      name: "Messages Sent in Total (#)",
      info: "The number of messages that were sent in total (including messages to nodes, workers, others, or the coordinator) per worker."
    },
    "messagesSentToNodes": {
      name: "Messages Sent to Nodes (#)",
      info: "The number of messages that were sent to nodes per worker."
    },
    "messagesSentToWorkers": {
      name: "Messages Sent to Workers (#)",
      info: "The number of messages that were sent to workers per worker."
    },
    "messagesSentToCoordinator": {
      name: "Messages Sent to Coordinator (#)",
      info: "The number of messages that were sent to the coordinator per worker."
    },
    "messagesSentToOthers": {
      name: "Messages Sent to Others (#)",
      info: "The number of messages that were sent to others per worker."
    },
    "messagesReceived": { 
      name: "Messages Received in Total (#)",
      info: "The number of messages that were received in total per worker."
    },
    "signalMessagesReceived": { 
      name: "Signal Messages Received (#)",
      info: "The number of signal messages that were received per worker."
    },
    "otherMessagesReceived": { 
      name: "Other Messages Received (#)",
      info: "The number of other messages that were received per worker."
    },
    "requestMessagesReceived": { 
      name: "Request Messages Received (#)",
      info: "The number of request messages that were received per worker."
    },
    "continueMessagesReceived": { 
      name: "Continue Messages Received (#)",
      info: "The number of continue messages that were received per worker."
    },
    "bulkSignalMessagesReceived": { 
      name: "Bulk Signal Messages Received (#)",
      info: "The number of bulk signal messages that were received per worker."
    },
    "heartbeatMessagesReceived": { 
      name: "Heartbeat Messages Received (#)",
      info: "The number of heartbeat messages that were received per worker."
    },
    "receiveTimeoutMessagesReceived": { 
      name: "Timeout Messages Received (#)",
      info: "The number of timeout messages that were received per worker."
    },
    "outgoingEdgesAdded": { 
      name: "Outgoing Edges Added (#)",
      info: "The number of outgoing edges that were added after the initial graph loading per worker."
    },
    "outgoingEdgesRemoved": { 
      name: "Outgoing Edges Removed (#)",
      info: "The number of outgoing edges that were removed after the initial graph loading per worker."
    },
    "numberOfOutgoingEdges": { 
      name: "Number of Outgoing Edges (#)",
      info: "The number of outgoing edges per worker."
    },
    "verticesRemoved": { 
      name: "Vertices Removed (#)",
      info: "The number of vertices that were removed after the initial graph loading per worker."
    },
    "verticesAdded": { 
      name: "Vertices Added (#)",
      info: "The number of vertices that were added after the initial graph loading per worker."
    },
    "numberOfVertices": { 
      name: "Number of Vertices (#)",
      info: "The number of vertices per worker."
    },
    "signalOperationsExecuted": { 
      name: "Signal Operations Executed (#)",
      info: "The number of signal operations executed per worker."
    },
    "collectOperationsExecuted": { 
      name: "Collect Operations Executed (#)",
      info: "The number of collect operations executed per worker."
    },
    "toCollectSize": { 
      name: "To Collect Size (#)",
      info: "The size of the To-Collect inbox per worker."
    },
    "toSignalSize": { 
      name: "To Signal Size (#)",
      info: "The size of the To-Signal inbox per worker."
    },
    "workerId": { 
      name: "Worker ID",
      info: "The ID of the individual workers."
    },
    "runtime_cores": { 
      name: "Available Processor Cores (#)",
      info: "The number of available processor cores per node."
    },
    "jmx_system_load": { 
      name: "System CPU Load per Core",
      info: "CPU load of the whole system per processor core (most computers have more than one core). Value is between 0.0 and 1.0."
    },
    "jmx_system_load_node": { 
      name: "System CPU Load",
      info: "CPU load of the whole node. Value is between 0.0 and the number of cores."
    },
    "jmx_process_time": { 
      name: "Process CPU Time per Core",
      info: "CPU time used by the process on which the JVM is running in nanoseconds per core."
    },
    "jmx_process_time_node": { 
      name: "Process CPU Time",
      info: "CPU time used by the process on which the JVM is running in nanoseconds per node."
    },
    "jmx_process_load": { 
      name: "Process CPU Load per Core",
      info: "Recent CPU usage for the JVM process per core."
    },
    "jmx_process_load_node": { 
      name: "Process CPU Load",
      info: "Recent CPU usage for the JVM process per node."
    },
    "jmx_swap_free": { 
      name: "Free Swap Space Size",
      info: "Amount of free swap space in byte per node."
    },
    "jmx_swap_total": { 
      name: "Total Swap Space Size",
      info: "Total amount of swap space in byte per node."
    },
    "jmx_mem_total": { 
      name: "Total Physical Memory Size",
      info: "Total physical memory (RAM) size in byte per node, not all of this may be used for the process."
    },
    "jmx_mem_free": { 
      name: "Free Physical Memory Size",
      info: "Amount of free physical memory (RAM) size in byte per node, not all of this may be used for the process."
    },
    "jmx_committed_vms": { 
      name: "Committed Virtual Memory Size",
      info: "Amount of virtual memory in bytes that is guaranteed to be available to the running process per node."
    },
    "runtime_mem_max": { 
      name: "Max Memory",
      info: "Maximum memory in bytes that can be used by the JVM per node. This value is configurable in the JVM."
    },
    "runtime_mem_free": { 
      name: "Free Memory",
      info: "Approximation of the free memory in bytes per node."
    },
    "runtime_mem_total": { 
      name: "Used Memory",
      info: "Total amount of allocated memory in bytes per node. This value may increase up to 'Max Memory'."
    }
};
