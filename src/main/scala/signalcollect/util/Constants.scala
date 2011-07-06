/*
 *  @author Francisco de Freitas
 *  
 *  Copyright 2011 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */

package signalcollect.util

/**
 * Ports and service names used by the distributed system
 */
object Constants {

  def MANAGER_SERVICE_PORT = 2552

  def MASTER_MANAGER_SERVICE_NAME = "master-service"
  def ZOMBIE_MANAGER_SERVICE_NAME = "zombie-service"

  // used by only one machine
  def COORDINATOR_SERVICE_PORT = 2553
  def COORDINATOR_SERVICE_NAME = "coordinator-service"

/*  def AKKA_MESSAGEBUS_SERVICE_PORT = 2554
  def AKKA_MESSAGEBUS_SERVICE_NAME = "message-bus"*/

  def LOGGER_SERVICE_NAME = "logger-service"
  def LOGGER_SERVICE_PORT = 2555
    
  // start of range for workers to listen to
  def WORKER_PORT_RANGE_START = 2556

}