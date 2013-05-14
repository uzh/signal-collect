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
           "Vertex with ID having this substring"
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
      info: ""
    },
    "messagesSentToWorkers": {
      name: "Messages Sent to Workers (#)",
      info: ""
    },
    "messagesSentToCoordinator": {
      name: "Messages Sent to Coordinator (#)",
      info: ""
    },
    "messagesSentToOthers": {
      name: "Messages Sent to Others (#)",
      info: ""
    },
    "messagesReceived": { 
      name: "Messages Received in Total (#)",
      info: ""
    },
    "signalMessagesReceived": { 
      name: "Signal Messages Received (#)",
      info: ""
    },
    "otherMessagesReceived": { 
      name: "Other Messages Received (#)",
      info: ""
    },
    "requestMessagesReceived": { 
      name: "Request Messages Received (#)",
      info: ""
    },
    "continueMessagesReceived": { 
      name: "Continue Messages Received (#)",
      info: ""
    },
    "bulkSignalMessagesReceived": { 
      name: "Bulk Signal Messages Received (#)",
      info: ""
    },
    "heartbeatMessagesReceived": { 
      name: "Heartbeat Messages Received (#)",
      info: ""
    },
    "receiveTimeoutMessagesReceived": { 
      name: "Timeout Messages Received (#)",
      info: ""
    },
    "outgoingEdgesAdded": { 
      name: "Outgoing Edges Added (#)",
      info: ""
    },
    "outgoingEdgesRemoved": { 
      name: "Outgoing Edges Removed (#)",
      info: ""
    },
    "numberOfOutgoingEdges": { 
      name: "Number of Outgoing Edges (#)",
      info: ""
    },
    "verticesRemoved": { 
      name: "Vertices Removed (#)",
      info: ""
    },
    "verticesAdded": { 
      name: "Vertices Added (#)",
      info: ""
    },
    "numberOfVertices": { 
      name: "Number of Vertices (#)",
      info: ""
    },
    "signalOperationsExecuted": { 
      name: "Signal Operations Executed (#)",
      info: ""
    },
    "collectOperationsExecuted": { 
      name: "Collect Operations Executed (#)",
      info: ""
    },
    "toCollectSize": { 
      name: "To Collect Size (#)",
      info: ""
    },
    "toSignalSize": { 
      name: "To Signal Size (#)",
      info: ""
    },
    "workerId": { 
      name: "Worker ID",
      info: ""
    },
    "runtime_cores": { 
      name: "Available Processors (#)",
      info: ""
    },
    "jmx_system_load": { 
      name: "System CPU Load (%)",
      info: ""
    },
    "jmx_process_time": { 
      name: "Process CPU Time (NS)",
      info: ""
    },
    "jmx_process_load": { 
      name: "Process CPU Load (%)",
      info: ""
    },
    "jmx_swap_free": { 
      name: "Free Swap Space Size (B)",
      info: ""
    },
    "jmx_swap_total": { 
      name: "Total Swap Space Size (B)",
      info: ""
    },
    "jmx_mem_total": { 
      name: "Total Physical Memory Size (B)",
      info: ""
    },
    "jmx_mem_free": { 
      name: "Free Physical Memory Size (B)",
      info: ""
    },
    "jmx_committed_vms": { 
      name: "Committed Virtual Memory Size (B)",
      info: ""
    },
    "runtime_mem_max": { 
      name: "Max Memory (B)",
      info: ""
    },
    "runtime_mem_free": { 
      name: "Free Memory (B)",
      info: ""
    },
    "runtime_mem_total": { 
      name: "Used Memory (B)",
      info: ""
    }
};
