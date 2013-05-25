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
 * The Configuration module retrieves data about different configuration and
 * parameter statistics from the console server. The module shows the JVM
 * parameters, the computation statistics as well as the graph statistics.
 * @constructor
 */
scc.modules.Configuration = function() {
  this.requires = ["configuration"];

  /**
   * Function that is called by the main module when a new WebSocket connection
   * is established. Requests data from the ConfigurationProvider.
   */
  this.onopen = function () {
    scc.order({"requestor": "Resources", "provider": "configuration"});
  }
    
  /**
   * Function that is called by the main module when a WebSocket error is
   * encountered. Does nothing.
   * @param {Event} e - The event that triggered the call.
   */
  this.onerror = function(e) { }

  /**
   * Function that is called by the main module when a requested piece of data
   * is not (yet) available from the server. Does nothing.
   */
  this.notready = function() { }

  /**
   * Function that is called by the main module when a new WebSocket connection
   * breaks down. Does nothing.
   */
  this.onclose = function() { }

  /**
   * Function that is called by the main module when a message is received from
   * the WebSocket. It populates the given information about the JVM,
   * computation and the graph in the proper elements.
   * @param {object} msg - The message object received from the server.
   */
  this.onmessage = function(msg) {
    if (msg.executionConfiguration != "unknown") {
      $.each(msg.executionConfiguration, function(k,v) {
        $("#resStat" + k).html(v);
      });
    }
    var ul = $("#infrastructureStatBox ul").html('');
    $.each(msg.systemProperties, function(index) {
      $.each(msg.systemProperties[index], function(k, v) {
        if (scc.conf.resources.hideInfrastructureItems.indexOf(k) == -1) {
          ul.append('<li>' + k + ': ' + v + '</li>');
        }
      });
    });
    ul = $("#graphStatBox ul").html('');
    $.each(msg.graphConfiguration, function(k, v) {
      if (k == "consoleHttpPort" && v[0] == -1) {
        v[0] = location.port;
      }
      ul.append('<li>' + k + ': ' + v[0] + '</li>');
    });
  }
}
