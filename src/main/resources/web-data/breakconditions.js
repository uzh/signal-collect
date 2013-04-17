scc.defaults.breakconditions = {};

CNAME = {"changesState": "changes state",
         "goesAboveState": "goes above state",
         "goesBelowState": "goes below state",
         "goesAboveSignalThreshold": "goes above signal threshold",
         "goesBelowSignalThreshold": "goes below signal threshold",
         "goesAboveCollectThreshold": "goes above collect threshold",
         "goesBelowCollectThreshold": "goes below collect threshold"
};

scc.modules.breakconditions = function () {
  this.requires = ["breakconditions"];

  this.onopen = function() {
    scc.order({"provider": "breakconditions"});
  }

  this.onerror = function(e) { }

  this.onclose = function() { }

  $("#gc_state").val(STR.enterState); 
  $("#gc_nodeId").val(STR.pickNode); 

  this.onmessage = function(j) {
    $("#gc_conditionList").empty();
    if (j["status"] == "noExecution" ) {
      $("#gc_conditionList").append('<div class="condition none">' + STR.noExecution + '</div>');
      scc.order({"provider": "breakconditions"}, 1000);
      return;
    }
    if (j.active.length == 0) {
      $("#gc_conditionList").append('<div class="condition none">' + STR.noConditions + '</div>');
    }
    $.each(j.active, function (k, c) {
      var s = c.props.nodeId;
      if (s.length > 23) {
        s = s.substring(s.length - 25, s.length);
      }
      var item = '<div class="condition';
      if (j.reached[c.id] != undefined) { item += ' reached' }
      item += ('">When Node with id: <span class="node_link" title="' + 
               c.props.nodeId + '">...' + s + '</span><br/> ' + c.name)
      switch(c.name) {
        case CNAME.goesAboveState:
        case CNAME.goesBelowState:
          item += (" " + c.props.expectedState); break;
        case CNAME.changesState:
          item += " from " + c.props.currentState; break;
        case CNAME.goesAboveSignalThreshold:
        case CNAME.goesBelowSignalThreshold:
          item += " " + c.props.signalThreshold; break;
        case CNAME.goesAboveCollectThreshold:
        case CNAME.goesBelowCollectThreshold:
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
      $("#gc_conditionList").append(item);
    });
    $("#gc_conditionList .delete").click(function (e) { 
      scc.order({
          "provider": "breakconditions",
          "action": "remove",
          "id": $(this).attr("data-id")
      });
    });
    $(".node_link").click(function (e) {
      var id = $(this).attr("title");
      scc.consumers.graph.searchById(id);
    });
    $("#gc_conditionList li:last-child").addClass("last_child");
  }

  $("#gc_useMouse").click(function (e) { 
    e.preventDefault();
    if ($("#graph_canvas").hasClass("picking")) {
      $("#graph_canvas").removeClass("picking");
      $("#gc_useMouse").removeClass("active");
    }
    else {
      $("#graph_canvas").addClass("picking");
      $("#gc_useMouse").addClass("active");
    }
  });

  d3.select("#graph_canvas").on("click", function (e) {
    if (!$("#graph_canvas").hasClass("picking")) { return; }
    $("#graph_canvas").removeClass("picking");
    $("#gc_useMouse").removeClass("active");
    var target = d3.event.target;
    var data = target.__data__;
    var node = $(target);
    if (data == undefined) {
      $("#gc_nodeId").val(STR.pickNode);
      $("#gc_addCondition").attr("disabled", true);
    }
    else {
      $("#gc_nodeId").val(data.id);
      $("#gc_nodeId").focus();
      $("#gc_nodeId").val($("#gc_nodeId").val());
      $("#gc_addCondition").removeAttr("disabled");
    }
  });

  $("#gc_nodeId").keyup(function(e) {
    if ($(this).val().length == 0 || $(this).val() == STR.pickNode) {
      $(this).val(STR.pickNode);
      $("#gc_addCondition").attr("disabled", true);
    }
    else {
      $("#gc_addCondition").removeAttr("disabled");
    }
  });

  $("#gc_state").keyup(function(e) {
    if ($(this).val().length == 0 || $(this).val() == STR.enterState) {
      $(this).val(STR.enterState); 
      $("#gc_addCondition").attr("disabled", true);
    }
    else {
      $("#gc_addCondition").removeAttr("disabled");
    }
  });

  $("#gc_addCondition").click(function (e) { 
    e.preventDefault();
    var name = $("#gc_condition").val().replace(/:/g,"");
    var props = {}
    switch (name) {
      case CNAME.changesState:
        props["nodeId"] = $("#gc_nodeId").val();
        break;
      case CNAME.goesAboveState:
      case CNAME.goesBelowState:
        props["nodeId"] = $("#gc_nodeId").val();
        props["expectedState"] = $("#gc_state").val();
        break;
      case CNAME.goesAboveSignalThreshold:
      case CNAME.goesBelowSignalThreshold:
      case CNAME.goesAboveCollectThreshold:
      case CNAME.goesBelowCollectThreshold:
        props["nodeId"] = $("#gc_nodeId").val();
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
    $("#gc_addCondition").attr("disabled", true);
    $("#gc_state").val(STR.enterState); 
    $("#gc_nodeId").val(STR.pickNode); 
  });

  $("#gc_condition").change(function (e) {
    conditionChoice = $("#gc_condition option:selected").val().replace(/:/g,"");
    switch(conditionChoice) {
      case CNAME.goesAboveState:
      case CNAME.goesBelowState:
        $("#gc_state").val(STR.enterState); 
        $("#gc_stateContainer").show(); 
        $("#gc_addCondition").attr("disabled", true);
        break;
      case CNAME.changesState:
      case CNAME.goesAboveSignalThreshold:
      case CNAME.goesBelowSignalThreshold:
      case CNAME.goesAboveCollectThreshold:
      case CNAME.goesBelowCollectThreshold:
        $("#gc_stateContainer").hide(); break;
    }
  });



}
