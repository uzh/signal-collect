/*
 *  @author Carol Alexandru
 *  //TODO: renew comments in this file!
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

scc.defaults.state = {};

/**
 * The State module provides insight into the state of the server during a 
 * computation (paused, signaling, collecting, ...). It's also responsible
 * for the graph execution controls in the UI.
 * @constructor
 */
scc.modules.State = function() {
  /**
   * Messages from the specified provider are routed to this module by the
   * main module.
   * @type {Array.<string>}
   */
  this.requires = ["state", "controls"];

  // Variables
  this.stateCount = 0;
  this.state = "uninizialized";
  var pendingCommand = false;
  var controls = ["step", "collect", "continue", "pause", "reset", "terminate"];

  /**
   * Add an event handler to each of the control buttons. Clicking a button
   * usually invokes an order to the server, if the button isn't decactivated.
   */
  controls.forEach(function (control) {
    $("#" + control).click(function (e) { 
      if ($(this).hasClass("blocked")) { return; }
      pendingCommand = true;
      $("#pending_command").show();
      scc.order({"provider": "controls", "control": control}); 
      $("#controls").find(".icon").addClass("blocked");
    });
  });

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Orders state information for the first time.
   * @param {Event} e - The event that triggered the call
   */
  this.onopen = function() {
    scc.order({"provider": "state"}); 
  };

  /**
   * Disable all buttons and then re-enable the ones supplied. The buttons will
   * only be activated if there is no command pending on the server.
   * @param {Array<string>} buttons - The buttons to activate
   */
  var enabledButtons = function (buttons) {
    if (pendingCommand) { return; }
    $("#controls").find(".icon").addClass("blocked")
    $.each(buttons, function (k, button) {
      $("#" + button).removeClass("blocked")
    });
  };

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. Depending on the incoming state, activate or 
   * de-activate the different buttons. If the computation is running
   * continuously, enable the graph autorefresh.
   * @param {object} j - The message object received from the server
   */
  this.onmessage = function(j) {
    // Receiving a message from controls means that the command was received.
    if (j.provider == "controls") { 
      pendingCommand = false;
      return;
    }
    // If there are no pending commands, remove the loading gif.
    if (!pendingCommand) {
      $("#pending_command").hide();
    }
    // Update the state information in the GUI
    $('#iteration').text(j.iteration);
    $('#resStatStatus').text(STR.State[j.state][0]);
    $('#state').text(STR.State[j.state][0])
    $('#state').attr("title", STR.State[j.state][1])
    switch (j.state) {
      case "pausedBeforeChecksBeforeSignal":
      case "pausedBeforeSignal":
      case "pausedBeforeChecksBeforeCollect":
      case "pausedBeforeCollect":
      case "pausedBeforeGlobalChecks":
        enabledButtons(["reset", "step", "collect", "continue", "terminate"])
        if (scc.consumers.Graph != undefined) {
          scc.consumers.Graph.autoRefresh = false;
          scc.consumers.Graph.order();
        }
        break;
    }
    // If the computation is continuing, it's only possible to pause or terminate
    if (j.steps != 0) {
      if (scc.consumers.Graph != undefined && scc.consumers.Graph.autoRefresh == false) {
        scc.consumers.Graph.autoRefresh = true;
        scc.consumers.Graph.order();
      }
      enabledButtons(["pause", "terminate"])
    }
    // Else there are more choices:
    else {
      if (scc.consumers.Graph != undefined) {
        scc.consumers.Graph.order();
      }
      switch (j.state) {
        case "signalling":
        case "collecting":
          enabledButtons(["terminate"])
          break;
        case "resetting":
          this.destroy();
          this.onopen();
          break;
        case "terminating":
          this.terminate("#success", "Terminating...");
          break;
      }
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

