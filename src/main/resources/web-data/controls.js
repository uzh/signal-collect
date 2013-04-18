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
  this.requires = ["controls"];

  var controls = ["step", "continue", "pause", "reset", "terminate"];
  controls.forEach(function (control) {
    $("#" + control).click(function (e) { 
      if ($(this).hasClass("blocked")) { return; }
      if ($(this).hasClass("hidden")) { return; }
      scc.order({"provider": "controls", "control": control}); 
      $("#controls").find(".icon").addClass("blocked");
    });
  });

  this.onopen = function() {
    $("#controls").find(".icon").removeClass("blocked");
  };

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

  this.onerror = function(e) {};
  this.notready = function() {};

  this.onclose = function() {
    this.destroy();
  };

  this.destroy = function() {
    $("#controls").find(".icon").addClass("blocked");
    $("#controls").find(".icon").removeClass("hidden");
    $("#controls").find("#pause").addClass("hidden");
    $('#resStatStatus').text('ended');
  };

  this.terminate = function(type, msg) {
    scc.webSocket.close();
    showMsg(type, msg);
    setTimeout(scc.createWebSocket, 10000);
  };
 
}
