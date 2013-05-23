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

/**
 * The default settings for the main module.
 * @constant
 * @default
 * @type {object}
 */
scc.defaults.main = {"view": "graph"};

/**
 * The Settings class can be used to store and retrieve settings. The settings
 * are stored in the hash tag of the URL. Each module provides a set of default
 * settings (stored in scc.defaults.*) and the Settings object automatically
 * removes default settings from the hash. This means that the settings hash is
 * basically a 'diff' between the defaults and the things the user changed.
 * @constructor
 */
scc.Settings = function() {
  /**
   * JS object representation of the settings stored in the hash. Example:
   * {"graph":
   *   {"options":
   *     {"gp_exposeVertices":"Yes",
   *      "gp_targetCount":"10",
   *      "gp_maxVertexCount":"600"},
   *    "layout":{
   *      "expositionWidth":344
   *     }
   *   }
   * }
   * @type {object}
   */
  this.settings = loadSettings();

  /**
   * Reloads the settings from the (updated) hash tag
   */
  this.reload = function() {
    this.settings = loadSettings();
  }

  /**
   * Applies a modification to the existing settings.
   * @param {function|object} modification - Either a function that acts on the
   *     existing settings object to return a modified settings object or a new
   *     settings object that will be merged with the existing settings object.
   */
  this.set = function(modification) {
    if (typeof(modification) == "function") {
      modification(this.settings);
    }
    else {
      var newSettings = {};
      $.extend(true, newSettings, this.settings);
      $.extend(true, newSettings, modification);
      this.settings = hideDefaults(scc.defaults, newSettings);
    }
    if (Object.keys(this.settings).length > 0) {
      top.location.hash = JSON.stringify(this.settings);
    }
    else {
      top.location.hash = "";
    }
  }

  /**
   * Retrieves the settings object. This is the function that should be used to
   *     retrieve settings.
   * @return {object} - The settings object (with all default values added)
   */
  this.get = function() {
    var s = {};
    $.extend(true, s, scc.defaults);
    $.extend(true, s, this.settings);
    return s;
  }

  /**
   * Reads the URL hashtag and parses it as JSON. 
   * @return {object} - The settings object (without any default values added)
   */
  function loadSettings() {
    var settings = {};
    var hash = top.location.hash.slice(1);
    if (hash) {
      try {
        hash = JSON.parse(hash);
        $.extend(true, settings, hash);
      } catch (e) {}
    }
    return settings;
  }

  /**
   * Removes any defaults from a given settings object or sub-object
   * @param {object} defaults - The object containing the properties that will
   *     be removed from the other object
   * @param {object} added - The object containing the new settings. The
   *     defaults will be removed from this object.
   * @return {object} - The settings object containing only the non-default
   *     properties
   */
  function hideDefaults(defaults, added) {
    if (typeof(added) == "object" ) {
      $.each(added, function(k, v) {
        added[k] = hideDefaults(defaults[k], v);
        if (added[k] == null || 
           (typeof(added[k]) == "object" &&  Object.keys(added[k]).length == 0)) {
          delete added[k];
        }
      });
    }
    else {
      if (defaults == added) { return null; }
    }
    return added;
  }

  /**
   * Handler for persisting settings automatically when they change
   * @param {Event} e - The event that triggered the call
   */
  $(".setting").change(function (e) { 
    var property = $(this);
    var panel = property.parents(".panel_container").attr("id").split("_")[0]
    var newSetting = {}
    newSetting[panel] = {"options":{}}
    newSetting[panel]["options"][property.attr("id")] = property.val()
    scc.settings.set(newSetting);
   });
}

$(document).ready(function() {
  scc.settings = new scc.Settings();

  /**
   * Changes the cursor globally to busy
   */
  scc.busy = function () { $("body,a,select,label").addClass("busy"); }

  /**
   * Removes the busy cursor globally
   */
  scc.notBusy = function () { $("body,a,select,label").removeClass("busy"); }

  /**
   * Instantiate a layouter.
   */
  scc.layout = new scc.lib.Layout();

  /** 
   * Instantiates scc.webSocket with a ReconnectingWebSocket and overrides
   * its onopen, onmessage, onclose and onerror functions. Much of the
   * inbound de-multiplexing is happening here, such as forwarding incoming
   * messages or processing error messages.
   */
  scc.createWebSocket = function () {
    scc.webSocket = new ReconnectingWebSocket(
                        "ws://" + document.domain + ":" + 
                        (parseInt(window.location.port) + 100));
    scc.webSocket.reconnectInterval = 500;
    scc.webSocket.timeoutInterval = 8000;

    /**
     * Function that is called when a new WebSocket connection is established.
     * It calls the onopen function on all active modules.
     * @param {Event} e - The event that triggered the call
     */
    scc.webSocket.onopen = function(e) {
      scc.layout.showMsg("#success", "WebSocket connection established", true);
      for (var m in scc.consumers) { scc.consumers[m].onopen(e) }
    }; 

    /**
     * Function that is called when a message is received from the WebSocket.
     * Depending on the provider of the message, it will be forwarded to the
     * consumers that requires messages by that provider, or the message may
     * be processed as an error.
     * @param {Event} e - The event that triggered the call. Most importantly,
     *     it contains the message from the server inside the e.data property.
     */
    scc.webSocket.onmessage = function(e) {
      var j = JSON.parse(e.data);
      var provider = j["provider"];
      // When a 'notready' message is received, the request that caused this
      // message is re-sent after waiting 500ms.
      if (provider == "notready") {
        var request = j["request"];
        scc.order(request, 500);
        var requestor = j["requestor"];
        if (scc.consumers[requestor].notready != null) {
          scc.consumers[requestor].notready(j);
        }
      }
      // Error messages are printed to the error pop-up, including the stack-
      // trace that caused the error. These errors are generally considered to
      // be fatal and the computation and console may not function properly
      // after such an occurrence.
      else if (provider == "error") {
        console.log(j["stacktrace"]);
        scc.layout.showMsg("#small_error", j["msg"] + ": " + j["stacktrace"]);
      }
      // If the server receives a request that it considers invalid, it sends
      // back this message. The error will be printed to the error pop-up
      else if (provider == "invalid") {
        scc.layout.showMsg("#small_error", j["msg"] + ", Comment: " + j["comment"]);
      }
      // Callees ordering information using scc.order can specify a callback
      // which will be called here
      if (scc.callbacks[provider]) { 
        scc.callbacks[provider]();
        delete scc.callbacks[provider];
      }
      // Any remaining messages are forwarded to their respective consumers
      for (var m in scc.consumers) { 
        var consumer = scc.consumers[m];
        if (consumer.requires.indexOf(provider) >= 0) {
          consumer.onmessage(j);
        }
      }
    };

    /**
     * Function that is called when a new WebSocket connection breaks down. All
     * existing orders are cancelled and the onclose function is called on each
     * consumer.
     * @param {Event} e - The event that triggered the call
     */
    scc.webSocket.onclose = function(e) {
      $.each(scc.orders, function(k, v) { clearTimeout(v); });
      scc.orders = {};
      scc.layout.showMsg("#error", "Connection Lost. Reconnecting to WebSocket...");
      for (var m in scc.consumers) {
        if (scc.consumers[m].onclose != null) {
          scc.consumers[m].onclose(e);
        }
      }
    };

    /**
     * Function that is called when a WebSocket error is encountered. In 
     * this case, the onerror function is called on each consumer.
     * @param {Event} e - The event that triggered the call
     */
    scc.webSocket.onerror = function(e) {
      for (var m in scc.consumers) {
        if (scc.consumers[m].onerror != null) {
          scc.consumers[m].onerror(e);
        }
      }
    };
  }

  /**
   * Delete all pending orders and callbacks for the given message provider.
   * @param {string} provider - The name of the provider
   */
  scc.resetOrders = function(provider) {
    clearTimeout(scc.orders[provider]);
    delete scc.orders[provider];
    delete scc.callbacks[provider];
  };

  /**
   * Order new data from the server. Messages need to adhere to the protocol.
   * and when an order is issued, any previous pending orders are overridden.
   * @param {string|object} msg - The message to be sent to the server. If a
   *     string is supplied, it will first be parsed as JSON. 
   * @param {int} delay - Milliseconds to wait before ordering the data
   * @param {function} cb - Callback function to be called upon receiving the
   *     reply from the server
   */
  scc.order = function(msg, delay, cb) {
    if (typeof(msg) == "string") {
      msg = JSON.parse(msg);
    }
    var id = msg.provider;
    if (!delay) { var delay = 0; }
    if (scc.orders[id]) { scc.resetOrders(id); }
    scc.callbacks[id] = cb;
    scc.orders[id] = setTimeout(function() {
      if (msg["provider"] == "graph") {
        $("#graph_background").text("Loading...").fadeIn(50);
      }
      var j = JSON.stringify(msg);
      try { 
        scc.webSocket.send(j); 
      }
      catch(err) { 
        console.log("cannot send message: " + j);
      }
    }, delay);
  };

  /**
   * Enable any number of modules. Enabling a module causes it to be
   * instantiated and to receive incoming messages of the types the module
   * specifies in its own this.requires property.
   * @param {Array.<string>} modules - The names of the modules to be enabled
   */
  var enableModules = function(modules) {
    for (var m in modules) {
      var module = modules[m];
      scc.consumers[module] = new scc.modules[module]();
    }
  };
  
  /**
   * Capitalizes the first letter of a String and returns the new String
   */
  String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
  }

  // Create the WebSocket and activate modules depending on the URL path
  scc.createWebSocket();
  switch (window.location.pathname) {
    case "/resources": 
    case "/graph": 
      var view = window.location.pathname.slice(1);
      scc.settings.set({"main": {"view": view}});
      enableModules(["State"]);
      if (view == "resources") {
        enableModules(["Resources", "Configuration", "Log"]);
      }
      if (view == "graph") {
        enableModules(["Graph", "BreakConditions"]);
      }
      scc.layout.layout([view]);
      break;
    default:
      enableModules(["Resources", "Configuration" , "Log", 
                     "Graph", "BreakConditions", "State"]);
      scc.layout.layout(["graph", "resources"]);
  }

});

/**
 * Ensure that the websocket is closed when leaving/closing the page
 */
window.onbeforeunload = function() {
  scc.webSocket.onclose = function () {}; 
  scc.webSocket.close();
};

