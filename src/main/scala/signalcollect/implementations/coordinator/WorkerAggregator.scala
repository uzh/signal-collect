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

class Counter(workers: Int) extends WorkerAggregator[Int](workers, 0, (_ + _)) {
  def increment = aggregate(1)
  def decrement = {
    aggregateValue -= 1
    aggregationCounter -= 1
  }
}

class LongAggregator(workers: Int) extends WorkerAggregator[Long](workers: Int, 0l, (_ + _))

class WorkerAggregator[G](workers: Int, initialValue: G, aggregationFunction: (G, G) => G) {

  def isDone = aggregationCounter == workers

  var aggregationCounter = 0

  var aggregateValue = initialValue

  def aggregate(item: G) {
    if (!isDone) {
      aggregateValue = aggregationFunction(aggregateValue, item)
      aggregationCounter += 1
    } else {
      throw new Exception("Aggregation error: too many results to aggregate")
    }
  }

  def tryAggregate(item: Any) {
    if (!isDone) {
      val castItem = try { Some(item.asInstanceOf[G]) } catch { case _ => None }  // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
      if (castItem.isDefined) {
        aggregateValue = aggregationFunction(aggregateValue, castItem.get)
        aggregationCounter += 1
      }
    } else {
      throw new Exception("Aggregation error: too many results to aggregate")
    }
  }

  def apply(): Option[G] = {
    if (isDone) {
      Some(aggregateValue)
    } else {
      None
    }
  }

  def reset = {
    aggregateValue = initialValue
    aggregationCounter = 0
  }
}