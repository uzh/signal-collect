/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package signalcollect.implementations.coordinator

import signalcollect.api.Factory._
import signalcollect.api.Factory
import signalcollect.interfaces._
import signalcollect.interfaces.ComputationStatistics
import java.util.concurrent.BlockingQueue

trait SynchronousExecution {

  protected def stepsLimit: Int
  
  protected def workerApi: WorkerApi
  
  protected def performComputation {
    var done = false
    do {
      workerApi.signalStep
      done = workerApi.collectStep
    } while ((workerApi.collectSteps < stepsLimit) && !done)
  }
  
}