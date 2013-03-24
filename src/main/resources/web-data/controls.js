scc.defaults.controls = { }

scc.modules.controls = function() {
  this.requires = ["controls"]

  var controls = ["step", "continue", "pause", "reset", "terminate"]
  controls.forEach(function (control) {
    $("#" + control).click(function (e) { 
      if ($(this).hasClass("blocked")) { return; }
      if ($(this).hasClass("hidden")) { return; }
      scc.order({"provider": "api", "control": control}) 
      $("#controls").find(".icon").addClass("blocked");
    });
  });

  this.onopen = function() {
    $("#controls").find(".icon").removeClass("blocked");
  }

  this.onmessage = function(j) {
    var newState = j.state
    $('#resStatStatus').text(newState);
    switch (newState) {
      case "stepping":
        $("#controls").find(".icon").removeClass("blocked");
        break;
      case "pausing":
        scc.consumers.graph.autoRefresh = false;
        $("#controls").find(".icon").removeClass("blocked");
        $("#controls").find("#pause").addClass("hidden");
        $("#controls").find("#continue").removeClass("hidden");
        break;
      case "continuing":
        scc.consumers.graph.autoRefresh = true;
        $("#controls").find(".icon").addClass("blocked");
        $("#controls").find("#pause").removeClass("blocked");
        $("#controls").find("#pause").removeClass("hidden");
        $("#controls").find("#terminate").removeClass("blocked");
        $("#controls").find("#terminate").removeClass("hidden");
        $("#controls").find("#continue").addClass("hidden");
        break;
      case "resetting":
        this.destroy()
        this.onopen()
        break;
      case "terminating":
        scc.terminate("#success", "Terminating...")
        break;
    }
    scc.consumers.graph.order()
  }

  this.onerror = function(e) { }
  this.notready = function() { }

  this.onclose = function() {
    this.destroy()
  }

  this.destroy = function() {
    $("#controls").find(".icon").addClass("blocked");
    $("#controls").find(".icon").removeClass("hidden");
    $("#controls").find("#pause").addClass("hidden");
    $('#resStatStatus').text('ended');
  }
  
}
