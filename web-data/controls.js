scc.defaults.controls = { }

scc.modules.controls = function() {
  this.requires = ["controls"]

  var controls = ["step", "continue", "pause", "reset", "terminate"]
  controls.forEach(function (control) {
    $("#" + control).click(function (e) { 
      scc.order({"provider": "api", "control": control}) 
      $("#controls").find(".icon").addClass("blocked");
    });
  });

  this.onopen = function() {
    $("#controls").find(".icon").removeClass("blocked");
  }

  this.onmessage = function(j) {
    var newState = j.state
    switch (newState) {
      case "stepping":
        $("#controls").find(".icon").removeClass("blocked");
        break;
      case "pausing":
        $("#controls").find(".icon").removeClass("blocked");
        $("#controls").find("#pause").addClass("hidden");
        $("#controls").find("#continue").removeClass("hidden");
        break;
      case "continuing":
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
        break;
    }

  }

  this.onerror = function(e) { }

  this.onclose = function() {
    this.destroy()
  }

  this.destroy = function() {
    $("#controls").find(".icon").addClass("blocked");
    $("#controls").find(".icon").removeClass("hidden");
    $("#controls").find("#pause").addClass("hidden");
  }


  
}
