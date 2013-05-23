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
 * The project container variable scc which appears in the global namespace.
 * @namespace
 * @property {object} modules - The module classes that can be instantiated.
 * @property {object} consumers - The instantiated modules
 * @property {object} defaults - The default settings of each module
 * @property {object} orders - The pending orders, zero or one for each consumer
 * @property {object} callbacks - Zero or one callback for each consumer to be
 *     called once a reply has been received from the server
 */
var scc = {"modules": {}, "consumers": {}, "defaults": {}, "orders": {}, 
           "callbacks": {}};

/**
 * Container for needed libraries and variables.
 * @property {object} graph - Container for the graph libraries
 * @property {object} resources - Container for the resources libraries
 */
scc.lib = {"graph": {}, "resources": {}};

/**
 * Container for the configurable parameters.
 * @property {object} graph - Container for the configurable graph parameters
 * @property {object} resources - Container for the configurable resources parameters
 */
scc.conf = {"graph": {}, "resources": {}};
