/**
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

  // Object scope variables
  this.stateCount = 0;
  this.state = "uninizialized";
  var pendingCommand = false;
  var controls = ["step", "collect", "continue", "pause", "reset", "terminate"];
  var retryMillis = 500;
  var retryMillisMultiplier = 1.2;
  var firstTry = true;

  /**
   * Add an event handler to each of the control buttons. Clicking a button
   * usually invokes an order to the server, if the button isn't deactivated.
   */
  controls.forEach(function (control) {
    $("#" + control).click(function (e) { 
      if ($(this).hasClass("blocked")) { return; }
      pendingCommand = true;
      $("#pending_command").show();
      scc.order({"requestor": "State", "provider": "controls", "control": control}); 
      $("#controls").find(".icon").addClass("blocked");
    });
  });

  /**
   * Order state data.
   */
  var order = function(o, delay) {
    if (o == undefined) { o = {}; }
    o["requestor"] = "State";
    o["provider"] = "state";
    scc.order(o, delay);
  };

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Orders state information for the first time.
   */
  this.onopen = function() {
    firstTry = true;
    order(); 
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
   * continuously, enable the graph auto refresh.
   * @param {object} j - The message object received from the server
   */
  this.onmessage = function(j) {
    // If this is the first time a state message is received, update the graph.
    if (firstTry == true) {
      try {
        scc.consumers.Graph.update();
        firstTry = false;
      } catch (e) {}
    }
    // Receiving a message from controls means that the command was received.
    if (j.provider == "controls") { 
      pendingCommand = false;
      return;
    }
    // If there are no pending commands, remove the loading GIF.
    if (!pendingCommand) {
      $("#pending_command").hide();
    }
    // Update the state information in the GUI
    var stateStrings = scc.STR.State[j.state];
    if (j.mode != undefined) {
      stateStrings = [j.mode + ": " + j.state, ""];
    }
    else if (stateStrings == undefined) {
      stateStrings = ["Mode: " + j.mode + ", state: " + j.state];
    }
    $('#resStatStatus').text(stateStrings[0]);
    $('#state').text(stateStrings[0])
    $('#state').attr("title", stateStrings[1])
    // Retry at increasing intervals if the mode is undetermined
    if (j.state == "undetermined") {
      setTimeout(function () {
        order(); 
      }, retryMillis);
      retryMillis *= retryMillisMultiplier;
    }
    // If the state is determined, reset retryMillis
    if (j.state != "undetermined") {
      retryMillis = 200;
    }
    // Adjust UI if not in interactive execution mode
    if (j.state == "undetermined" || !scc.STR.State.hasOwnProperty(j.state)) {
      $("#cGraphControlEnabled").addClass("hidden");
      $("#iteration_container").addClass("hidden");
      $("#cGraphControlDisabled").removeClass("hidden");
    }
    // If neither undetermined, nor interactive, then some other mode is being
    // used. We simply enable/disable auto-refreshing as long as the computation
    // is running.
    if (!scc.STR.State.hasOwnProperty(j.state)) {
      if (j.state != undefined && 
          j.state.toLowerCase().indexOf("converged") != -1) {
        if (scc.consumers.Graph.autoRefresh == false) {
          scc.consumers.Graph.update();
        }
        scc.consumers.Graph.autoRefresh = false;
      }
      else if (scc.consumers.Graph.autoRefresh == false) {
        scc.consumers.Graph.autoRefresh = true;
        scc.consumers.Graph.update();
      }
      if (scc.consumers.Graph.autoRefresh == true) {
        setTimeout(function () {
          order(); 
        }, 1000);
      }
      return
    }
    if (j.state == "initExecution") { return; }
    // What follows is only available in interactive mode
    $("#cGraphControlEnabled").removeClass("hidden");
    $("#iteration_container").removeClass("hidden");
    $("#cGraphControlDisabled").addClass("hidden");
    $('#iteration').text(j.iteration);
    switch (j.state) {
      case "pausedBeforeChecksAfterCollect":
      case "pausedBeforeSignal":
      case "pausedBeforeChecksAfterSignal":
      case "pausedBeforeCollect":
      case "pausedBeforeGlobalChecks":
        enabledButtons(["reset", "step", "collect", "continue", "terminate"])
        if (scc.consumers.Graph != undefined) {
          scc.consumers.Graph.autoRefresh = false;
          scc.consumers.BreakConditions.order();
        }
        break;
    }
    // If the computation is continuing, it's only possible to pause or
    // terminate. If only a few steps remain, don't do anything.
    if ((j.steps > 5 || j.steps == -1) && ["converged", "globalConditionReached"].indexOf(j.state) == -1) {
      if (scc.consumers.Graph != undefined && scc.consumers.Graph.autoRefresh == false) {
        scc.consumers.Graph.autoRefresh = true;
        scc.consumers.Graph.update();
        scc.consumers.BreakConditions.order();
      }
      enabledButtons(["pause", "terminate"])
    }
    // Else there are more choices:
    else if (j.steps == 0 || ["converged", "globalConditionReached"].indexOf(j.state) != -1) {
      if (scc.consumers.Graph != undefined) {
        scc.consumers.Graph.autoRefresh = false;
        scc.consumers.Graph.update();
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
        case "converged":
        case "globalConditionReached":
          enabledButtons(["reset", "terminate"])
          break;
        case "terminating":
          this.terminate("#success", "Terminating...");
          break;
      }
    }
  };

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
   * accompanying message in the UI. This means that other modules may call
   * this terminate function and supply a reason for why they did it (e.g.
   * a fatal error message).
   * @param {string} type - The message type to show
   * @param {string} msg - The message to show
   */
  this.terminate = function(type, msg) {
    scc.webSocket.close();
    scc.layout.showMsg(type, msg);
    setTimeout(scc.createWebSocket, 10000);
  };
 
}

