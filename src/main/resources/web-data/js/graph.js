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

/**
 * The default settings for the graph module.
 */
scc.defaults.graph = {"layout": {
                        "cVertexSelection": "show",
                        "cGraphAdvanced": "hide",
                        "cGraphControl": "show",
                        "expositionWidth": "350"
                      },
                      "options": {
                        "gs_addBySubstring": "",
                        "gs_autoAddVicinities": "No",
                        "gs_topCriterium": "Highest degree",
                        "gd_vertexSize": "Vertex state",
                        "gd_vertexColor": "Outgoing degree",
                        "gd_vertexBorder": "Latest query",
                        "gp_vicinityIncoming": "Yes",
                        "gp_exposeVertices": "Yes",
                        "gp_vicinityRadius": "1",
                        "gp_maxVertexCount": "250",
                        "gp_targetCount": "100",
                        "gp_refreshRate": "1",
                        "gp_drawEdges": "When graph is still"
                      }
};

/**
 * The Graph module uses the GraphD3 class to draw the graph and
 * handles the panel UI functionality for the graph.
 * @constructor
 */
scc.modules.Graph = function() {
  /**
   * Messages from the specified provider are routed to this module by the
   * main module.
   * @type {Array.<string>}
   */
  this.requires = ["graph", "configuration"];

  /**
   * If this is true, the graph will reload itself periodically. This is the
   * case when the computation is running.
   * @type {boolean}
   */
  this.autoRefresh = false;

  // Object-scope variables
  var graphModule = this;
  var graphD3 = new scc.lib.graph.GraphD3(this);
  this.graphD3 = graphD3;
  var GSTR = scc.STR["Graph"];
  var vertexCountIntervals = []; 
  for (var i = 5; i<=25; i+=5) { vertexCountIntervals.push(i) };
  for (var i = 50; i<=250; i+=50) { vertexCountIntervals.push(i) };
  for (var i = 400; i<=1000; i+=100) { vertexCountIntervals.push(i) };
  var resizingExposition = false;
  var mouseClickCoords = undefined;
  var selectedVertices;
  var pickAction;

  /**
   * Clears and then populates the select box that allows the user to choose
   * the number of exceptional vertices with numbers ranging up to the
   * specified maximum.
   * @param {int} maximum - Maximum option to show
   */
  var populateTargetCountSelector = function (maximum) {
    var selector = $("#gp_targetCount");
    var currentChoice = selector.val()
    selector.empty();
    for (var i in  vertexCountIntervals) {
      if (vertexCountIntervals[i] > maximum) { break; }
      selector.append('<option value="' + vertexCountIntervals[i] + '">' + 
                      vertexCountIntervals[i] + '</option>');
    }
    // re-select the value selected before emptying or the maximum value
    if (currentChoice != null) {
      currentChoice = parseInt(currentChoice)
      if (currentChoice > maximum) { selector.val(maximum); }
      else if (currentChoice <= maximum) { selector.val(currentChoice); }
    }
  }

  /**
   * Wrapper function to do things that always come with a graph order.
   */
  this.order = function (o, delay) {
    if (o == undefined) { o = {}; }
    o["requestor"] = "Graph";
    o["provider"] = "graph";
    o["exposeVertices"] = (scc.settings.get().graph.options["gp_exposeVertices"] == "Yes")
    scc.order(o, delay)
  };

  /**
   * Order graph data using the options set in the GUI. This function may be
   * called by other modules as well, in case they require a graph refresh.
   */
  this.update = function(delay) {
    if (graphD3.vertexStorage.get().length > 0) {
      graphModule.order({"query": "vertexIds",
                         "vertexIds": graphD3.vertexStorage.get()
      }, delay);
    }
    else {
      graphD3.resetDefaultStatus();
    }
  };

  /**
   * Take a JSON document and transform it into a tree compatible with d3's
   * tree layout (with name and children attributes).
   * @param {object} json to transform
   * @return {object} tree for d3's tree layout
   */
  var jsonIntoTree = function (json) {
    if(typeof json == "object") {
      var arr = []
      $.each(json, function(k,v) {
        var child = jsonIntoTree(v)
        if (Object.prototype.toString.call(child) === '[object Array]') {
          arr.push({"name": k, "children": child});
        }
        else {
          arr.push({"name": k, "val": child});
        }
      });
      return arr;
    }
    else {
      return json;
    }
  };

  /**
   * Loads the information into the exposition panel using the provided id and
   * JSON data.
   * @param {string} id the id of the vertex
   * @param {object} id the arbitrary JSON data to display
   */
  this.expose = function (data) {
    // Transform the JSON into a tree for d3's tree layout
    var tree = { "name": "root", "children": jsonIntoTree(data.info)}

    // Clicking on the Id shall load the node
    $("#exposition_title").html(
        'ID: <span class="tid">' + data.id + '</span><br/>' +
        '<span>Vertex type: ' + data.t + '</span><br/>' +
        '<span>State: ' + data.state + '</span><br/>' +
        '<span>Signal score: ' + data.ss + '</span><br/>' +
        '<span>Collect score: ' + data.cs + '</span><br/>')
    $("#exposition_data_title").html('Information exposed by Vertex:')
    $(".tid").click(function () {
      graphD3.addBySubstring($(this).text());
    });

    // Create the layout and populate it
    var treeLayout = d3.layout.tree();
    treeLayout.value(function(d) { return d.name; });

    var expo = d3.select("#exposition_data")
    expo.data([tree]);
    expo.data([tree]);
    var t = expo.selectAll("div")
        .data(treeLayout.nodes);
    
    t.enter().append("div")
      .attr("class", function (d) {
        if (d.depth == 0) {
          return "hidden";
        }
        if (d.children != undefined) {
          return "tobject_key";
        }
        return "tvalue_container";
      })
      .style("margin-left", function(d) {
        return (10*(d.depth-1)) + "px";
      })
    t.exit().remove();
    t.html(function (d) {
      var s = d.name;
      s = '<span class="tvalue_key">' + s + '</span>';
      if (d.val == undefined) { 
        s = s + ": ";
      }
      else { 
        s = s + '<br/><span class="tvalue">' + d.val + '</span>';
      }
      return s;
    })
  }

  /**
   * Handler for when the user presses the mouse when resizing the exposition panel.
   * @param {Event} e - The event that triggered the call
   */
  d3.select('#dragbar').on("mousedown.resizeExposition", function () {
    d3.event.preventDefault();
    resizingExposition = true;
    var main = $('#exposition');
    var ghostbar = $('<div>', {id:'ghostbar',
                               css: { height: main.outerHeight(),
                                      top: main.offset().top,
                                      left: main.offset().left
                    }}).appendTo('body');
    d3.select(document).on("mousemove.resizeExposition", function () {
      d3.event.preventDefault();
      ghostbar.css("left", d3.event.clientX-2);
    });
  });
      
  /**
   * Handler for when the user releases the mouse when resizing the exposition panel.
   * @param {Event} e - The event that triggered the call
   */
  d3.select(document).on("mouseup.resizeExposition", function () {
    d3.event.preventDefault();
    if (resizingExposition) {
      var newWidth = $(document).width()-d3.event.clientX+2;
      scc.settings.set({"graph":{"layout": {"expositionWidth": newWidth}}});
      $('#exposition').css("width", newWidth);
      $('#graph_canvas').css("right", newWidth);
      $('#ghostbar').remove();
      d3.select(document).on("mousemove.resizeExposition", null);
      resizingExposition = false;
    }
  });

  /**
   * Modifies the panel HTML, sets options and adds dynamic fields.
   */
  this.layout = function() {
    for (var i = 1; i<=4; i++) {
      $("#gp_vicinityRadius").append('<option value="' + i + '">' + i + '</option>');
    }
    for (var i = 1; i<=10; i+=1) {
      $("#gp_refreshRate").append('<option value="' + i + '">' + i + '</option>');
    }
    for (var i = 15; i<=60; i+=5) {
      $("#gp_refreshRate").append('<option value="' + i + '">' + i + '</option>');
    }
    $("#gp_refreshRate").append('<option value="Never">Never</option>');
    for (var i in vertexCountIntervals) {
      $("#gp_maxVertexCount").append('<option value="' + vertexCountIntervals[i] + '">' + 
                                     vertexCountIntervals[i] + '</option>');
    }
    populateTargetCountSelector(vertexCountIntervals[vertexCountIntervals.length-1]);
    $('#gs_addBySubstring').click(function(e) { 
      if ($(this).val() == GSTR["addBySubstring"]) {
        $(this).select();
      }
    });
    $('#gs_addBySubstring').keypress(function(e) {
      if ( e.which == 13 ) { 
        e.preventDefault();
        $("#gs_addBySubstringAdd").addClass("active")
        setTimeout(function () { $("#gs_addBySubstringAdd").removeClass("active"); }, 100);
        graphD3.addBySubstring($("#gs_addBySubstring").val());
      }
    });
    $.each(scc.settings.get().graph.layout, function (key, value) {
      if (value == "show") { $("#" + key).show(); }
    });
    $.each(scc.settings.get().graph.options, function (key, value) {
      $("#" + key).val(value);
    });
    $("#gs_addBySubstring").val(GSTR.addBySubstring);
    if (scc.settings.get().graph.options["gp_exposeVertices"] == "Yes") {
      $('#exposition').css("width", scc.settings.get().graph.layout.expositionWidth);
      graphD3.exposedVertexId = localStorage["exposedVertexId"];
      $("#exposition").fadeIn(100, function () {
        $('#graph_canvas').css("right", $("#exposition").width());
      });
    }
    $("#exposition_background").text(GSTR.expositionEmpty);
    var val = parseInt($("#gp_maxVertexCount").val());
    populateTargetCountSelector(val);
    $.each(GSTR.menuhelp, function (k, v) {
      var subject = $('label[for="' + k + '"],' +
                      '.panel_span[data-title="' + k + '"]')
      if (subject.length == 0) {
        subject = $('#' + k)
      }
      subject.attr("title", v);
    });
  }
  this.layout();

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Creates the SVG element and prepares the graph drawing
   * functionality.
   * @param {Event} e - The event that triggered the call
   */
  this.onopen = function(e) {
    graphD3.onopen(e);
  }

  /**
   * Function that is called by the main module when a message is received
   * from the WebSocket. Reads the graph data received and updates the graph
   * accordingly.
   * @param {object} j - The message object received from the server
   */
  this.onmessage = function(j) {
    graphD3.onmessage(j)
  }

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Shows a message on the graph canvas.
   */
  this.notready = function() {
    $("#graph_background").text("Data Provider not ready, retrying...");
  };

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. 
   * @param {Event} e - The event that triggered the call
   */
  this.onclose = function(e) {
    graphD3.onclose(e);
  };

  /**
   * Take two coordinates and use them as opposite corner of a rectangle.
   * Calculate the position and size of the resulting rectangle.
   * @param {array<int>} cornerA - The [x,y] position of the first corner
   * @param {array<int>} cornerB - The [x,y] position of the second corner
   * @return {array<int>} dimensions - The [x, y, w, h] dimensions
   */
  var cornersToDimensions = function (cornerA, cornerB) {
    var left = Math.min(cornerA[0],cornerB[0]);
    var top = Math.min(cornerA[1],cornerB[1]);
    var width = Math.max(cornerA[0],cornerB[0]) - left;
    var height = Math.max(cornerA[1],cornerB[1]) - top;
    return [left, top, width, height];
  };


  /**
   * d3 handler that initiates a selection using the mouse to draw a rectangle.
   */
  d3.select("#graph_canvas_overlay").on("mousedown.selectVertices", function (e) {
    // Store the positions of where the mouse button was pressed
    mouseClickCoords = d3.mouse(this);
    // Place the selection rectangle at the right spot and show it
    $("#graph_canvas_selection").css({"height": 0, "width": 0});
    $("#graph_canvas_selection").show();
  });

  /**
   * d3 handler that adjusts the selection rectangle when moving the mouse
   */
  d3.select("#graph_canvas_overlay").on("mousemove.selectVertices", function (e) {
    if (mouseClickCoords) {
      // update the width
      var coords = d3.mouse(this);
      var dimensions = cornersToDimensions(coords, mouseClickCoords);
      $("#graph_canvas_selection").css({"left": dimensions[0], 
                                        "top": dimensions[1],
                                        "width": dimensions[2], 
                                        "height": dimensions[3]
      });
    }
  });

  /**
   * d3 handler that finally calculates which vertices fall into the selection
   * and then adjusts the panel buttons depending on the action that follows
   * this selection.
   */
  d3.select("#graph_canvas_overlay").on("mouseup.selectVertices", function (e) {
    // Filter vertices to find the ones inside the selection rectangle
    var coords = d3.mouse(this);
    selectedVertices = $("circle").filter(function (i) {
      var boundingRect = this.getBoundingClientRect()
      if (boundingRect.left-301 > Math.min(coords[0], mouseClickCoords[0]) && 
          boundingRect.bottom > Math.min(coords[1], mouseClickCoords[1]) &&
          boundingRect.right-301 < Math.max(coords[0], mouseClickCoords[0]) && 
          boundingRect.bottom < Math.max(coords[1], mouseClickCoords[1])) { 
        return true;
      }
      return false;
    });

    // Highlight the selected vertices
    selectedVertices.css({"fill": "#00ff00", "stroke": "#fff"});

    // Modify the panel buttons depending on the current action
    switch (pickAction) {
      case "remove":
        $("#gd_removeBySelect").addClass("snd");
        $("#gd_removeBySelectRemove").removeClass("hidden");
        break;
      case "addVicinities":
        $("#gs_addVicinitiesBySelect").addClass("snd");
        $("#gs_addVicinitiesBySelectAdd").removeClass("hidden");
        break;
    }

    // Hide the selection rectangle and overlay
    $("#graph_canvas_selection").hide();
    $("#graph_canvas_overlay").fadeOut(100);
    $("#graph_canvas_overlay").removeClass("pickingVertices");
  });

  /**
   * Function used as handler for selecting vertices whose vicinities shall be 
   * loaded.
   */
  var processSelection = function (e, action, primaryButton, confirmButton) { 
    e.preventDefault();
    // restore styling on all vertices
    graphD3.resetVertexStyle();
    selectedVertices = undefined;
    $("button").attr("disabled", false);
    primaryButton.removeClass("active");
    pickAction = action;
    // modify panel buttons
    if (primaryButton.text() == "Cancel") {
      primaryButton.text("Selected vertices");
      primaryButton.removeClass("snd");
      confirmButton.addClass("hidden");
      $("#graph_canvas_selection").hide();
      $("#graph_canvas_overlay").fadeOut(100);
      $("#graph_canvas_overlay").removeClass("pickingVertices");
    }
    else {
      $("button").removeClass("snd");
      primaryButton.text("Cancel");
      primaryButton.addClass("active");
      confirmButton.addClass("hidden");
      $("#graph_canvas_overlay").addClass("pickingVertices");
      $("#graph_canvas_overlay").fadeIn(100);
      $("button:not(.active)").attr("disabled", true);
    }
  };

  /**
   * Bind the function to the vertex selection buttons
   */
  $("#gs_addVicinitiesBySelect").click(function (e) {
    processSelection(e, "addVicinities", $("#gs_addVicinitiesBySelect"), 
                     $("#gs_addVicinitiesBySelectAdd"));
  });
  $("#gd_removeBySelect").click(function (e) {
    processSelection(e, "remove", $("#gd_removeBySelect"), 
                     $("#gd_removeBySelectRemove"));
  });

  /**
   * Function used as handler for finally executing the action 
   */
  var executeAction = function (e, primaryButton, confirmButton, callback) {
    e.preventDefault();
    $("button").attr("disabled", false);
    primaryButton.text("Selected vertices");
    primaryButton.removeClass("snd");
    primaryButton.removeClass("active");
    confirmButton.addClass("hidden");
    callback();
  }

  /**
   * Bind the function to the vertex selection buttons.
   */
  $("#gs_addVicinitiesBySelectAdd").click(function (e) { 
    executeAction(e, $("#gs_addVicinitiesBySelect"), 
                     $("#gs_addVicinitiesBySelectAdd"), function () {
      // extract list of Ids from vertex list and order data
      var selectedVertexIds = $.map(selectedVertices, function (vertex, key) { 
        return vertex.__data__.id;
      });
      graphModule.order({
             "query": "vertexIds",
             "vertexIds": selectedVertexIds,
             "vicinityIncoming": ($("#gp_vicinityIncoming").val() == "Yes"),
             "vicinityRadius": parseInt($("#gp_vicinityRadius").val()) 
      });
    });
  });

  $("#gd_removeBySelectRemove").click(function (e) { 
    executeAction(e, $("#gd_removeBySelect"), 
                     $("#gd_removeBySelectRemove"), function () {
      graphD3.removeVerticesFromCanvas(selectedVertices);
    });
  });

  /**
   * Handler called when the user clicks on the button to remove all vertices.
   * @param {Event} e - The event that triggered the call
   */
  $("#gd_removeOrphans").click(function (e) { 
    e.preventDefault();
    graphD3.removeOrphans();
  });

  /**
   * Handler called when the user clicks on the button to remove all vertices that
   * do not belong to the latest query.
   * @param {Event} e - The event that triggered the call
   */
  $("#gd_removeNonLatest").click(function (e) { 
    e.preventDefault();
    graphD3.removeNonLatestVertices();
  });

  /**
   * Handler that fires when pressing a key. It supports some
   * actions using the keyboard.
   */
  $(document).keydown(function(e) {
    // If the user has selected vertices to be removed, DEL will do the same as
    // clicking on "Remove". ESC will cancel the selection.
    if ($("#gd_removeBySelect").hasClass("snd")) {
      if ( e.which == 46 ) { 
        e.preventDefault();
        $("#gd_removeBySelectRemove").trigger('click');
      }
    }
    if ($("#gd_removeBySelect").text() == "Cancel") {
      if ( e.which == 27 ) { 
        e.preventDefault();
        $("#gd_removeBySelect").trigger('click');
      }
    }
    // If the user has selected vertices to load their vicinities, ENTER will do
    // the same as clicking on "Add". ESC will cancel the selection.
    if ($("#gs_addVicinitiesBySelect").hasClass("snd")) {
      if ( e.which == 13 ) { 
        e.preventDefault();
        $("#gs_addVicinitiesBySelectAdd").trigger('click');
      }
    }
    if ($("#gs_addVicinitiesBySelect").text() == "Cancel") {
      if ( e.which == 27 ) { 
        e.preventDefault();
        $("#gs_addVicinitiesBySelect").trigger('click');
      }
    }
  });

  $("#gs_addBySubstringAdd").click(function (e) {
    e.preventDefault();
    var s = $("#gs_addBySubstring").val();
    graphD3.addBySubstring(s);
  });
  $("#gs_addByTop").click(graphD3.addTop);
  $("#gs_addRecentVicinitiesAdd").click(graphD3.addRecentVicinities);
  $("#gs_addAllVicinitiesAdd").click(graphD3.addAllVicinities);

  /**
   * Handler called when a 'select' HTML element is changed. The choice is
   * persisted to the settings hash.
   * @param {Event} e - The event that triggered the call
   */
  $("#cGraphDesign select").change(function (e) {
    var property = $(this);
    graphD3.setGraphDesign(property.attr("id"), property.val());
  });
  /**
   * Handler called when the user clicks on button to remove all vertices
   * @param {Event} e - The event that triggered the call
   */
  $("#gd_removeAll").click(function (e) { 
    e.preventDefault();
    graphD3.removeAllVertices();
  });

  /**
   * Handler called when the maximum vertex count option changes. Adjusts the
   * possible range to choose from in the target count option.
   */
  $("#gp_maxVertexCount").change(function (e) { 
    graphD3.removeOverflowingVertices();
    var val = parseInt($(this).val());
    populateTargetCountSelector(val);
  });

  /**
   * Handler called when the exposeVertices option changes. Adjusts the
   * possible range to choose from in the target count option.
   */
  $("#gp_exposeVertices").change(function (e) { 
    var val = $(this).val();
    if (val == "Yes") {
      scc.consumers.Graph.update();
      $('#exposition').css("width", scc.settings.get().graph.layout.expositionWidth);
      graphD3.exposedVertexId = localStorage["exposedVertexId"];
      $("#exposition").fadeIn(100, function () {
        $('#graph_canvas').css("right", $("#exposition").width());
      });
      $("#exposition").fadeIn(100)
    }
    else {
      $("#exposition").fadeOut(100)
      $('#graph_canvas').css("right", 0);
    }
  });

  /**
   * Handler called when the 'draw edges' option changes. Persists the choice 
   * to the settings hash and updates the representation (shows or hides the
   * edges)
   * @param {Event} e - The event that triggered the call
   */
  $("#gp_drawEdges").change(function (e) { 
    var val = $(this).val();
    graphD3.setEdgeDrawing(val);
  });

  /**
   * Handler called when the user unfocuses the id-substring field. Reverts
   * value to default if it's left empty.
   */
  $("#gs_addBySubstring").change(function () {
    if ($("#gs_addBySubstring").val().length == 0) {
      $("#gs_addBySubstring").val(GSTR.addBySubstring); 
    }
  });
};

