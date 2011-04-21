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

package signalcollect.interfaces

import java.util.concurrent.LinkedBlockingQueue
import signalcollect.implementations.messaging.MultiQueue
import scala.concurrent.forkjoin.LinkedTransferQueue
import java.util.concurrent.BlockingQueue

object Queue {
	type QueueFactory = () => BlockingQueue[Any]
	
	lazy val defaultFactory = linkedTransferQueueFactory
	
	lazy val linkedTransferQueueFactory = () => new LinkedTransferQueue[Any]
	lazy val linkedBlockingQueueFactory = () => new LinkedBlockingQueue[Any]
	lazy val multiQueueFactory = new MultiQueue[Any](_, _)
}