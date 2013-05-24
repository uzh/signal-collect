/**
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

/**
 * The default settings for the graph module.
 * @constant
 * @default
 * @type {Object}
 */
scc.defaults.resources = {"layout":{
                            "cResourceComputation": "show",
                            "cResourceProblems": "show"
                          },
                          "section": "statistics"
};

/**
 * Object container that encapsulates all chart objects.
 * @type {Object}
 */
scc.lib.resources.lineCharts = {};


/**
 * Event handler to catch onHashChange events to update the current section.
 */
$(window).on('hashchange', function() {
  scc.settings.reload();
  var settings = scc.settings.get();
  if (settings.resources.section != null) {
    scc.lib.resources.show_section(settings.resources.section);
  }
});

/**
 * Hides all sections and shows only the given section.
 * @param {string} s - The name of the section to show.
 */
scc.lib.resources.show_section = function(s) {
  if (s == "") { return; }
  // hide all sections
  $("#resources .structured > div[id^=\"crs\"]").hide();
  // show the appropriate section
  $("#crs_" + s).show();
  scc.lib.resources.show_boxes(s);
  // show change in the panel
  $("#resources_panel_container input").prop("checked", false);
  $("#resources_panel_container input#rs_" + s + "").prop("checked", true);
  // change body class
  $("body").attr("class", s);
  // set section to the hash tag
  var mod = {"resources": {"section": s }}
}

/**
 * Hides all resource boxes and only show the ones that are needed for the
 * given section.
 * @param {string} s - The name of the section to show the resource boxes for.
 */
scc.lib.resources.show_boxes = function(s) {
  var boxes = "#resourceBoxes";
  // first, hide all of them
  $(boxes + " > div").attr("class", "hidden");
  if (scc.conf.resources.resourceBoxes[s] == null) { return; }
  // then only show the ones that are needed
  scc.conf.resources.resourceBoxes[s].forEach(function(v) {
    var resourceBox = boxes + " > #" + v;
    $(resourceBox).removeClass("hidden");
    $(resourceBox).appendTo(boxes); // change order
    if (v.endsWith("Chart")) {
      scc.lib.resources.update_chart(v.slice(0, -5));
    }
  });
}

/**
 * Updates a chart when it is in viewport.
 * @param {string} chart - The name of the chart to update.
 */
scc.lib.resources.update_chart = function(chart) {
  if (!scc.lib.resources.lineCharts.hasOwnProperty(chart)) {
    return;
  }
  if (scc.lib.resources.lineCharts[chart].isOverlappingViewport()) {
    scc.lib.resources.lineCharts[chart].updateChart();
  }
}

/**
 * Event handler that gets called when the DOM is ready.
 */
$(document).ready(function() {
  $("#resources_panel_container label").click(function() {
    var section = $(this).attr("for").split("_")[1];
    scc.settings.set({'resources':{'section':section}});
  });
});
