$(document).ready(function() {

  // check whether or not to show the introduction
  var cookieFound = (document.cookie.indexOf("introduction=1") !== -1);
  try {
    var jsonHash = JSON.parse(top.location.hash.slice(1));
  } catch (e) {}
  var forceIntro = (jsonHash != null && jsonHash.intro != null);
  if (cookieFound && !forceIntro) {
    // we do not have to show the introduction
    return false;
  }
  
  // define the introduction selectors and texts
  var properties = {
    "#title":
      "<strong>Welcome! This is a guided tour through the user interface of the Console Server for Signal/Collect." + 
      "</strong><br />You can easily navigate through these steps by using the arrow keys or your mouse.",
    "#modes":
      "The Console Server consists of two main views: The 'Graph View' and the 'Resource View'.",
    "#cNodeSelection":
      "In the Graph view, you can navigate the graph on which the current computation is running. In order to" +
      " display only certain vertices of the graph, you can choose them in this section by either showing 'top" +
      " vertices' or a specific vertice by ID.",
    "#cGraphDesign":
      "In this section, you can choose how the vertices in the graph should look like. You can define node size," +
      " color and outline color based on certain node properties. You can also define the node vicinity and other" +
      " characteristics.",
    "#cGraphControl":
      "In this section you can add different breakpoints to stop the computation when certain events come in.",
    "#controls":
      "When the Console is used, the computation usually does not start automatically. Instead the user can step" +
      " through the computation, run it until it is paused again, or terminate completely.",
    "#mode_resources":
      "To change to the 'Resources', just click on it.",
    "#crs_statistics h1":
      "First you will see statistics about your infrastructure, the computation that is running, the graph parameters," +
      " and estimations about the graph size.",
    "#cResourceComputation":
      "Besides statistics, you can also look at log messages and several different charts.",
    "#crs_detailed h1":
      "A chart draws three different lines over time. The blue line visualizes the lowest value over all workers, the" +
      " red line is the highest value, and the black line is the average value.",
    "#cResourceProblems":
      "Should you ever have a problem with a computation, we try to help in this section. For every problem or" + 
      " question, we try to explain why this could have happened and show information that might help in solving" +
      " a problem or answer a question.",
    "#title img":
      "This is the end of the tour. We hope you enjoy using this tool."
  };
  
  // add the introduction information to the DOM
  var i = 1;
  $.each(properties, function(selector, val) {
    $(selector).attr("data-step", i++).attr("data-intro", val);
  });

  // show the introduction
  var intro = introJs();
  var view = "";
  intro.onchange(function(targetElement) {
    var currentDataStep = parseInt($(targetElement).attr("data-step"));
    if (currentDataStep >= parseInt($("#cResourceComputation").attr("data-step"))) {
      show_section("detailed");
      view = ',"main":{"view":"resources"},"resources":{"section":"detailed"}';
    }
    else if (currentDataStep >= parseInt($("#mode_resources").attr("data-step"))) {
      $("#mode_resources").click();
      view = "";
    } else {
      view = "";
    }
    if (targetElement.id == "cResourceComputation") {
      $("#mode_resources").click();
    }
    if (targetElement.id == "controls") {
      $("#mode_graph").click();
      $(".introjs-tooltip").addClass("tooltipOnTop");
    } else {
      $(".introjs-tooltip").removeClass("tooltipOnTop");
    }
    top.location.hash = '{"intro":' + $(targetElement).attr('data-step') + view + "}";
  });
  if (forceIntro) {
    intro.goToStep(jsonHash.intro);
  } else {
    top.location.hash = "";
    $("#mode_graph").click();
  }
  var finishIntro = function() {
    $("#mode_graph").click();
    top.location.hash = "";
  };
  intro.onexit(finishIntro);
  intro.oncomplete(finishIntro);
  intro.start();
  
  // set a cookie for one year
  document.cookie = "introduction=1;max-age=31536000"  
});
