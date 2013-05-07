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

/**
 * The default settings for the breakconditions module.
 */
scc.defaults.breakconditions = {};


/**
 * The BreakConditions module allows setting new break conditions on the 
 * computation as well as loading the existing break conditions from the
 * server.
 * @constructor
 */
scc.modules.BreakConditions = function () {
  /**
   * Messages from the specified provider are routed to this module by the
   * main module.
   * @type {Array.<string>}
   */
  this.requires = ["breakconditions"];

  // Object-scope variables
  BSTR = STR["BreakConditions"];

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests the list of break conditions currently specified.
   * @param {Event} e - The event that triggered the call
   */
  this.onopen = function(e) {
    $("#gc_vertexId").val(BSTR.pickVertex);
  }

  /**
   * Function that is called by the main module when a WebSocket error is
   * encountered. Does nothing.
   * @param {Event} e - The event that triggered the call
   */
  this.onerror = function(e) { };

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. Does nothing.
   * @param {Event} e - The event that triggered the call
   */
  this.onclose = function(e) { };

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Does nothing.
   * @param {Event} e - The event that triggered the call
   */
  this.notready = function() { };

  /**
   * Order break condition data in order to update the condition list
   */
  this.order = function() {
    scc.order({"provider": "breakconditions"});
  };

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. Rebuilds the list of conditions, possibly
   * highlighting any number of them.
   * @param {object} j - The message object received from the server
   */
  this.onmessage = function(j) {
    $("#gc_conditionList").empty();
    // It can happen that the Console is started without the interactive
    // execution mode or that the execution mode object cannot be retrieved.
    // In this case, another order is issued after a certain delay.
    
    // If there are no conditions specified, display a placeholder
    if (j.active.length == 0) {
      $("#gc_conditionList").append('<div class="condition none">' + BSTR.noConditions + '</div>');
    }

    // Add each of the conditions to the list of conditions
    $.each(j.active, function (k, c) {
      // Shorten long vertex ids
      var s = c.props.vertexId;
      if (s.length > 20) {
        s = s.substring(s.length - 22, s.length);
      }
      // Build the div element to be added...
      var item = '<div class="condition';
      if (j.reached[c.id] != undefined) { item += ' reached' }
      item += ('">Vertex with id: <span class="vertex_link" title="' + 
               c.props.vertexId + '">...' + s + '</span><br/>when ' + c.name)
      switch(c.name) {
        case BSTR.stateAbove:
        case BSTR.stateBelow:
          item += (" " + c.props.expectedState); break;
        case BSTR.stateChanges:
          item += " from " + c.props.currentState; break;
        case BSTR.signalScoreAboveThreshold:
        case BSTR.signalScoreBelowThreshold:
          item += " " + c.props.signalThreshold; break;
        case BSTR.collectScoreAboveThreshold:
        case BSTR.collectScoreBelowThreshold:
          item += " " + c.props.collectThreshold; break;
      }
      if (j.reached[c.id] != undefined) { 
        var goal = j.reached[c.id];
        if (goal.length > 15) {
          goal = goal.substring(0, 12) + "...";
        }
        item += ': <span class="goal" title="' + j.reached[c.id] + '">' + goal + '</span>';
      }
      item += ('<div class="delete" data-id="' + c.id + '" /></div>');
      // Finally append the item to the list
      $("#gc_conditionList").append(item);
    });

    if (j["status"] == "noExecution" ) {
      $("#gc_conditionList").append('<div class="condition none">' + BSTR.noExecution + '</div>');
      scc.order({"provider": "breakconditions"}, 1000);
      return;
    }

    // Display error if any
    if (j["error"] != undefined) {
      if (j["error"].indexOf("Missing or invalid vertexId") != -1) {
        $("#gc_conditionList").append('<div class="condition none fade">' + BSTR.invalidId + '</div>');
      }
      else if (j["error"].indexOf("Invalid state") != -1) {
        $("#gc_conditionList").append('<div class="condition none fade">' + BSTR.invalidState + '</div>');
      }
      else {
        $("#gc_conditionList").append('<div class="condition none fade">' + BSTR.unknownError +
                                      j["error"] + '</div>');
      }
      setTimeout(function () {
        $(".fade").slideUp(200, function () {
          $(this).remove(); 
        });
      }, 4000);
    }

    // Delete button handler
    $("#gc_conditionList .delete").click(function (e) { 
      scc.order({
          "provider": "breakconditions",
          "action": "remove",
          "id": $(this).attr("data-id")
      });
    });

    /**
     * Handler called upon clicking on a vertex id. It highlights the appropriate
     * vertex in the graph or loads the vertex if it's not yet represented in the
     * graph. This is done simply by using the addById function provided by
     * the graph module
     */
    $(".vertex_link").click(function (e) {
      var id = $(this).attr("title");
      scc.consumers.Graph.addBySubstring(id);
    });

    // The last child in the list doesn't have a bottom border
    $("#gc_conditionList li:last-child").addClass("last_child");
  }

  /**
   * Handler that allows the user to pick a vertex from the graph to fill in the
   * id of the vertex which the break condition shall be applied to
   */
  $("#gc_useMouse").click(function (e) { 
    e.preventDefault();
    if ($("#graph_canvas").hasClass("picking")) {
      $("#graph_canvas").removeClass("picking");
      $("#gc_useMouse").removeClass("active");
      $("#gc_useMouse").text("Select");
    }
    else {
      $("#graph_canvas").addClass("picking");
      $("#gc_useMouse").addClass("active");
      $("#gc_useMouse").text("Cancel");
    }
  });

  /**
   * Handler that is added to the #graph_canvas for when the user picks a vertex
   * using the mouse. It only applies when the #graph_canas has the "picking"
   * class.
   */
  d3.select("#graph_canvas").on("click.breakconditions", function (e) {
    if (!$("#graph_canvas").hasClass("picking")) { return; }
    $("#gc_useMouse").text("Select");
    $("#graph_canvas").removeClass("picking");
    $("#gc_useMouse").removeClass("active");
    var target = d3.event.target;
    var data = target.__data__;
    var vertex = $(target);
    if (data == undefined) {
      $("#gc_vertexId").val(BSTR.pickVertex);
      $("#gc_addCondition").attr("disabled", true);
    }
    else {
      $("#gc_vertexId").val(data.id);
      $("#gc_vertexId").focus();
      $("#gc_vertexId").val($("#gc_vertexId").val());
      $("#gc_vertexId").trigger("keyup");
    }
  });

  /**
   * Checks if the choices made by the user can be considered "sane" for
   * submission to the server. It doesn't necessarily mean that the supplied
   * ID or state is valid, but it's not empty or the default and the
   * combination of choices is valid
   */
  var formCompleted = function () {
    if ($("#gc_vertexId").val() == BSTR.pickVertex ||
        $("#gc_vertexId").val().length == 0) {
      return false;
    }
    else {
      if ($("#gc_condition").val() == BSTR.stateChanges) {
        return true;
      }
      else if ($("#gc_state").val() == BSTR.enterState ||
               $("#gc_state").val().length == 0) {
        return false;
      }
    }
    return true;
  };

  /**
   * Handler that toggles the 'add' button if the content of the text fields
   * while the user types. Performed on each keyup on any of the two fields.
   */
  var checkFormCompleted = function(e) {
    if (formCompleted()) {
      $("#gc_addCondition").removeAttr("disabled");
    }
    else {
      $("#gc_addCondition").attr("disabled", true);
    }
  };
  $("#gc_vertexId").keyup(checkFormCompleted);
  $("#gc_state").keyup(checkFormCompleted);

  /**
   * Checks if a field is empty and fills in the default. Performed only on
   * change, not on keyup.
   */
  var checkEmptyField = function(e) {
    if ($("#gc_vertexId").val().length == 0) {
      $("#gc_vertexId").val(BSTR.pickVertex); 
    }
    if ($("#gc_state").val().length == 0) {
      $("#gc_state").val(BSTR.enterState); 
    }
    $("#gc_vertexId").trigger("keyup");
    $("#gc_state").trigger("keyup");
  }
  $("#gc_vertexId").change(checkEmptyField);
  $("#gc_state").change(checkEmptyField);

  /**
   * Handler that is called when clicking on 'add condition'. The input fields
   * are parsed and the data is ordered from the server.
   */
  $("#gc_addCondition").click(function (e) { 
    e.preventDefault();
    var name = $("#gc_condition").val().replace(/:/g,"");
    var props = {};
    switch (name) {
      case BSTR.stateChanges:
        props["vertexId"] = $("#gc_vertexId").val();
        break;
      case BSTR.stateAbove:
      case BSTR.stateBelow:
        props["vertexId"] = $("#gc_vertexId").val();
        props["expectedState"] = $("#gc_state").val();
        break;
      case BSTR.signalScoreAboveThreshold:
      case BSTR.signalScoreBelowThreshold:
      case BSTR.collectScoreAboveThreshold:
      case BSTR.collectScoreBelowThreshold:
        props["vertexId"] = $("#gc_vertexId").val();
        break;
    }
    scc.order({
        "provider": "breakconditions",
        "action": "add",
        "name": name,
        "props": props
    });
    $("#gc_conditionList").children(".none").remove();
    $("#gc_conditionList").append('<div class="condition new last_child"></div>');
    $("#gc_vertexId").trigger("keyup");
  });

  /**
   * Handler that determines which sub-fields are displayed when the user
   * selects the condition type.
   */
  $("#gc_condition").change(function (e) {
    conditionChoice = $("#gc_condition option:selected").val().replace(/:/g,"");
    switch(conditionChoice) {
      case BSTR.stateAbove:
      case BSTR.stateBelow:
        $("#gc_stateContainer").show(); 
        break;
      case BSTR.stateChanges:
      case BSTR.signalScoreAboveThreshold:
      case BSTR.signalScoreBelowThreshold:
      case BSTR.collectScoreAboveThreshold:
      case BSTR.collectScoreBelowThreshold:
        $("#gc_stateContainer").hide();
        break;
    }
    $("#gc_vertexId").trigger("keyup");
  });

  // set the default text on the text fields
  $("#gc_state").val(BSTR.enterState); 
  $("#gc_vertexId").val(BSTR.pickVertex); 

}
