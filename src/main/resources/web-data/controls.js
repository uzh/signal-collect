/*
 *  @author Carol Alexandru
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

scc.defaults.controls = {};

scc.modules.Controls = function() {
  /**
   * Messages from the specified provider are routed to this module by the
   * main module.
   * @type {Array.<string>}
   */
  this.requires = ["controls"];

  // Object-scope variables
  var controls = ["step", "continue", "pause", "reset", "terminate"];

  /**
   * Add an event handler to each of the control buttons. Clicking a button
   * usually invokes an order to the server, if the button isn't decactivated.
   */
  controls.forEach(function (control) {
    $("#" + control).click(function (e) { 
      if ($(this).hasClass("blocked")) { return; }
      if ($(this).hasClass("hidden")) { return; }
      scc.order({"provider": "controls", "control": control}); 
      $("#controls").find(".icon").addClass("blocked");
    });
  });

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Activate the buttons.
   * @param {Event} e - The event that triggered the call
   */
  this.onopen = function() {
    $("#controls").find(".icon").removeClass("blocked");
  };

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. Depending on the incoming state, activate or 
   * de-activate the different buttons. If the computation is running
   * continuously, enable the graph autorefresh.
   * @param {object} j - The message object received from the server
   */
  this.onmessage = function(j) {
    var newState = j.state;
    $('#resStatStatus').text(newState);
    switch (newState) {
      case "stepping":
        $("#controls").find(".icon").removeClass("blocked");
        break;
      case "pausing":
        scc.consumers.Breakconditions.onopen();
        scc.consumers.Graph.autoRefresh = false;
        $("#controls").find(".icon").removeClass("blocked");
        $("#controls").find("#pause").addClass("hidden");
        $("#controls").find("#continue").removeClass("hidden");
        break;
      case "continuing":
        scc.consumers.Graph.autoRefresh = true;
        $("#controls").find(".icon").addClass("blocked");
        $("#controls").find("#pause").removeClass("blocked");
        $("#controls").find("#pause").removeClass("hidden");
        $("#controls").find("#terminate").removeClass("blocked");
        $("#controls").find("#terminate").removeClass("hidden");
        $("#controls").find("#continue").addClass("hidden");
        break;
      case "resetting":
        this.destroy();
        this.onopen();
        break;
      case "terminating":
        this.terminate("#success", "Terminating...");
        break;
    }
    if (scc.consumers.Graph != null) {
      scc.consumers.Graph.order();
    }
  };

  /**
   * Function that is called by the main module when a WebSocket error is
   * encountered. Does nothing.
   * @param {Event} e - The event that triggered the call
   */
  this.onerror = function(e) { };

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Does nothing.
   * @param {Event} e - The event that triggered the call
   */
  this.notready = function() { };

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. Resets all the buttons.
   * @param {Event} e - The event that triggered the call
   */
  this.onclose = function() {
    this.destroy();
  };

  /**
   * Deactivates all buttons.
   */
  this.destroy = function() {
    $("#controls").find(".icon").addClass("blocked");
    $("#controls").find(".icon").removeClass("hidden");
    $("#controls").find("#pause").addClass("hidden");
    $('#resStatStatus').text('ended');
  };

  /**
   * Orders the server to shut down. A callback is installed that will 
   * re-enable the automatic WebSocket reconnect after a timeout, giving
   * the server the chance to shut down before re-connection is attempted.
   * The function accepts two parameters that will be used to display an
   * accompaning message in the UI. This means that other modules may call
   * this terminate function and supply a reason for why they did it (e.g.
   * a fatal error message).
   * @param {string} type - The message type to show
   * @param {string} msg - The message to show
   */
  this.terminate = function(type, msg) {
    scc.webSocket.close();
    showMsg(type, msg);
    setTimeout(scc.createWebSocket, 10000);
  };
 
}

