package com.signalcollect.storage

import com.signalcollect.interfaces._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.IndexedSeq
import com.signalcollect._

/**
 * Buffer for incoming signals that keeps only the relevant information by reducing the signals at arrival
 * 
 * @param reducer 	Combines the aggregated state with the incoming state and provides a new value for the aggregated state.
 * 					If no aggregated state is found the first parameter will be None and needs to be handled.
 * 
 * @param mapper	Maps the aggregated value back to a signal that is later handed to the vertex for collecting
 * 
 * @param 			neutral value for the buffer if an empty vertexId is added.
 */
class ReducingVertexSignalBuffer[AggregatedValueType](reducer: (Option[AggregatedValueType], SignalMessage[_]) => AggregatedValueType,
    mapper: (AggregatedValueType, Any) => SignalMessage[_],
    neutralValue: AggregatedValueType) extends VertexSignalBuffer {

  val undeliveredSignals = new ConcurrentHashMap[Any, AggregatedValueType](16, 0.75f, 1) //key: recipients id, value: signals for that recipient
  var iterator = undeliveredSignals.keySet.iterator
  
  /**
   * Adds a new signal for a specific recipient to the buffer
   * The reducer checks the content field of the signal and determines if it can aggregate the new signals with the one already stored or discard the new signal.
   *
   * Notice: Signals are not checked for valid id when inserted to the buffer.
   *
   * @param signal the signal that should be buffered for further collecting
   */
  def addSignal(signal: SignalMessage[_]) {
    
    
    if (undeliveredSignals.containsKey(signal.edgeId.targetId)) {
      val aggregatedValue = reducer(Some(undeliveredSignals.get(signal.edgeId.targetId)), signal)
      undeliveredSignals.put(signal.edgeId.targetId, aggregatedValue)
    } else {
      undeliveredSignals.put(signal.edgeId.targetId, reducer(None, signal))
    }
  }

  /**
   * If the map contains no entry for that id a new entry is created with no signals buffered
   * This can be useful when a vertex still needs to collect even though no new signals are available
   *
   * @ vertexId the ID of a vertex that should collect regardless of the existence of signals for it
   */
  def addVertex(vertexId: Any) {
    if (!undeliveredSignals.containsKey(vertexId)) {
      undeliveredSignals.put(vertexId, neutralValue)
    }
  }

  /**
   * Manually removes the vertexId and its associated signals from the map
   * Should only be used when a vertex is removed from the map to remove the vertex after successfully collecting use the parameter in the foreach function
   *
   * @param vertexId the ID of the vertex that needs to be removed from the map
   */
  def remove(vertexId: Any) {
    undeliveredSignals.remove(vertexId)
  }

  def isEmpty: Boolean = undeliveredSignals.isEmpty

  /**
   * Returns the number of vertices for which the buffer has signals stored.
   */
  def size = undeliveredSignals.size

  /**
   * Iterates through all signals in the buffer and applies the specified function to each entry
   * Allows the the loop to be escaped and to resume work at the same position
   *
   * @param f 				the function to apply to each entry in the map
   * @param clearWhenDone	determines if the map should be cleared when all entries are processed
   * @param breakCondition 	determines if the loop should be escaped before it is done
   */
  def foreach[U](f: (Any, IndexedSeq[SignalMessage[_]]) => U,
    removeAfterProcessing: Boolean,
    breakCondition: () => Boolean = () => false): Boolean = {

    if (!iterator.hasNext) {
      iterator = undeliveredSignals.keySet.iterator
    }

    while (iterator.hasNext && !breakCondition()) {
      val currentId = iterator.next
      val aggregatedSignal = mapper(undeliveredSignals.get(currentId), currentId)
      f(currentId, IndexedSeq(aggregatedSignal))
      if (removeAfterProcessing) {
        remove(currentId)
      }
    }
    !iterator.hasNext
  }

  def cleanUp = undeliveredSignals.clear
}