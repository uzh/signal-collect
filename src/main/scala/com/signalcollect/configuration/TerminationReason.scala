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

package com.signalcollect.configuration

/**
 *  Enumeration used to communicate why a computation was terminated.
 */
object TerminationReason extends Enumeration with Serializable {

  /**
   *  Computation terminated because the specified time limit was reached.
   */
  val TimeLimitReached = Value

  /**
   *  Computation terminated because all the signalScores and collectScores
   *  of the vertices were below the respective thresholds.
   */
  val Converged = Value

  /**
   *  Computation terminated because the global constraint was met.
   */
  val GlobalConstraintMet = Value

  /**
   *  Computation terminated because the step limit was reached.
   *  This is only relevant for the synchronous execution mode.
   */
  val ComputationStepLimitReached = Value

  /**
   *  Computation has not termination. This is used for the continuous
   *  asynchronous execution mode, which returns immediately, but keeps on processing.
   */
  val Ongoing = Value

}