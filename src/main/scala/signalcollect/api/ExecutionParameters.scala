/*
 *  @author Philip Stutz
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

package signalcollect.api

import signalcollect.interfaces._

case class ExecutionParameters(
  signalThreshold: Double = 0.01,
  collectThreshold: Double = 0.0,
  timeLimit: Option[Long] = None,
  stepsLimit: Option[Long] = None) {

  override def toString: String = {
    "signal threshold" + "\t" + signalThreshold + "\n" +
      "collect threshold" + "\t" + collectThreshold + "\n" +
      "time limit" + "\t" + "\t" + timeLimit + "\n" +
      "steps limit" + "\t" + "\t" + stepsLimit
  }
}