scc.defaults.CoordinatorIdle = {};

scc.modules.CoordinatorIdle = function() {
  this.requires = ["coordinatoridle"];
  this.onopen = function(e) {
    $("#coordinatoridle").show(); 
    scc.order({"provider": "coordinatoridle"})
  }
  this.onmessage = function(j) {
    $("#coordinatoridle").removeClass("notready"); 
    if (j.idle == true) { $("#coordinatoridle").addClass("idle"); }
    else { $("#coordinatoridle").removeClass("idle"); }
    scc.order({"provider": "coordinatoridle"}, 200)
  }
  this.onclose = function(e) {
    $("#coordinatoridle").hide();
  }
  this.notready = function(e) {
    $("#coordinatoridle").addClass("notready"); 
  }
};

