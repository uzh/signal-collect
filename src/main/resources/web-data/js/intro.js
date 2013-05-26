/**
 *  @author Carol Alexandru
 *  @author Silvan Troxler
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
  var url = document.URL.split("/");
  var loc = url[0] + "//" + url[2];
  var locGraph = '<a target="_blank" href="' + loc + '/graph">' + loc + '/graph</a>';
  var locResources = '<a target="_blank" href="' + loc + '/resources">' + loc + '/resources</a>';
  
  // define the introduction selectors and texts
  var properties = {
    "#title":
      "<strong>Welcome! This is a guided tour through the user interface of the Signal/Collect Console.</strong><br/>" + 
      "Navigate these steps by using the arrow keys or your mouse and hit ESC or click 'Skip' to cancel the tour. " +
      "You can revisit the tour at any time by clicking on <strong>Tour</strong> in the lower right corner of the screen.",
    "#modes":
      "The Console Server consists of two main views: The <strong>Graph View</strong> and the <strong>Resource View</strong>. " +
      "You can switch between the views using these buttons, or you may access a single view under " + locGraph + 
      " or " + locResources + " without loading the other.",
    "#cVertexSelection":
      "In the Graph view you can view and navigate the graph of your current computation. You start out with an empty " +
      "canvas onto which you add the vertices you're interested in. When the view becomes cluttered, you can remove vertices you don't " +
      "need anymore.",
    "#gs_container":
      "Add vertices..." +
      "<ul><li>by enabling an option to expand vertices on hover</li>" +
      "    <li>by their exceptional properties</li>" +
      "    <li>by searching for vertices with an ID containing the given string</li>" + 
      "    <li>by <strong>double-clicking</strong> on a vertex to expand its vicinity</li>" +
      "    <li>by clicking on 'Selected vertices' and then using the mouse to draw a rectangle around several vertices to expand</li></ul>" +
      "Signal/Collect Console remembers which vertices have been previously added to the canvas even beyond a restart of the server or your browser.",
    "#gd_container":
      "Clear the entire canvas or remove vertices by drawing a rectangle around them. You can also remove all vertices except the ones most recently added to the canvas, or any vertices that don't have any edges (orphans).",
    "#cGraphAdvanced":
      "Click on a panel section to expand it. The advanced options may impact your performanced and affect the look and feel of the graph. Hover over the options to find out more about them.",
    "#cGraphControl":
      "Select a vertex and add break conditions to halt the computation when certain events occur.",
    "#controls":
      "If your graph computation is using the <strong>interactive execution mode</strong>, then you can use these buttons to control it. ",
    "#reset":
      "<strong>Reset</strong> the computation to restore all vertices to their initial state",
    "#pause":
      " <strong>Pause</strong> the computation if it is running",
    "#step":
      " Performing a <strong>partial step</strong> will walk you through all the states of a single iteration, pausing before each of them: " +
               "signalling, condition checks after signalling, collecting, condition checks after collecting, and global termination check.",
    "#collect":
      " Performing a <strong>full step</strong> continues the iteration and stops before the next signal step.",
    "#continue":
      " When clicking <strong>continue</strong>, the computation will run until you pause it or until a break condition fires.",
    "#terminate":
      " Click <strong>terminate</strong> to end the computation and quit Signal/Collect.",
    "#stateContainer":
      " The current state and the current iteration are displayed here",
    "#mode_resources":
      "To change to the 'Resources', just click on it.",
    "#crs_statistics h1":
      "First you will see statistics about your infrastructure, the computation that<br /> is running, the graph parameters," +
      " and estimations about the graph size.",
    "#cResourceComputation":
      "Besides statistics, you can also look at log messages and several different charts.",
    "#crs_nodecharts h1":
      "<ul><li>A chart draws three different lines over time. The <span style=\"color:blue;\">blue</span> line" +
      " visualizes the lowest value over<br /> all nodes or workers, the <span style=\"color:red;\">red</span>" +
      " line is the highest value, and the black line is the average value.</li><li>The controls in the upper" +
      " right can be used to collectively <strong>zoom, shift, and reset all charts</strong>.</li></ul>",
    "#cResourceProblems":
      "Should you ever have a problem with a computation, we try to help in this section. For every problem or" + 
      " question, we try to explain why this could have happened and show information that might help in solving" +
      " a problem or answer a question.",
    "#title img":
      "This is the end of the tour. We hope you enjoy using this tool. " +
      "You can revisit the tour at any time by clicking on <strong>Tour</strong> in the lower right corner."
  };
  
  // add the introduction information to the DOM
  var i = 1;
  $.each(properties, function(selector, val) {
    $(selector).attr("data-step", i++).attr("data-intro", val);
  });
  $("#controls,#reset,#pause,#step,#collect,#continue,#terminate,#stateContainer, " +
    "#cGraphDesign,#cGraphControl").attr("data-position", "top");

  // show the introduction
  var intro = introJs();
  var view = "";
  intro.onchange(function(targetElement) {
    var currentDataStep = parseInt($(targetElement).attr("data-step"));
    if (currentDataStep >= parseInt($("#cResourceComputation").attr("data-step"))) {
      scc.lib.resources.show_section("nodecharts");
      view = ',"main":{"view":"resources"},"resources":{"section":"nodecharts"}';
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
    if (targetElement.id == "stateContainer") {
      $("#mode_graph").click();
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
