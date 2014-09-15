package com.signalcollect

import com.signalcollect.interfaces.AggregationOperation
import scala.collection.immutable.SortedMap

/** Container for the deliver and collect duration measurements */
case class ActivityTime(signal: Int, deliver: Int, collect: Int) extends Ordered[ActivityTime] {
  override def toString: String =
    f"signal: ${signal}ns, deliver: ${deliver}ns, collect: ${collect}ns"
  def compare(that: ActivityTime) = {
    (that.signal + that.deliver + that.collect) - (this.signal + this.deliver + this.collect)
  }
}

/**
 * Finds the vertices in the graph which were active for the longest duration
 *
 * @param n the number of top vertices to find
 */
class TopActivityAggregator[Id](n: Int)
  extends AggregationOperation[SortedMap[ActivityTime, Id]] {
  type ActivityMap = SortedMap[ActivityTime, Id]
  def extract(v: Vertex[_, _, _, _]): ActivityMap = v match {
    case t: Timeable[Id, _, _, _] =>
      SortedMap((ActivityTime(t.signalTime, t.deliverTime, t.collectTime) -> t.id))
    case _ =>
      SortedMap[ActivityTime, Id]()
  }
  def reduce(activities: Stream[ActivityMap]): ActivityMap = {
    activities.foldLeft(SortedMap[ActivityTime, Id]()) { (acc, m) => acc ++ m }.take(n)
  }
}

/** Allows measuring how long a vertex stays in deliverSignal and collect*/
trait Timeable[Id, State, GraphIdUpperBound, GraphSignalUpperBound] extends Vertex[Id, State, GraphIdUpperBound, GraphSignalUpperBound] {
  var signalTime: Int = 0
  var deliverTime: Int = 0
  var collectTime: Int = 0
  def time[R](block: => R): (R, Int) = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    (result, (t1 - t0).toInt)
  }
  abstract override def executeSignalOperation(graphEditor: GraphEditor[GraphIdUpperBound, GraphSignalUpperBound]): Unit = {
    val (_, t) = time(super.executeSignalOperation(graphEditor))
    signalTime += t
  }
  abstract override def deliverSignalWithSourceId(signal: GraphSignalUpperBound, sourceId: GraphIdUpperBound,
    graphEditor: GraphEditor[GraphIdUpperBound, GraphSignalUpperBound]): Boolean = {
    val (result, t) = time(super.deliverSignalWithSourceId(signal, sourceId, graphEditor))
    deliverTime += t
    result
  }
  abstract override def deliverSignalWithoutSourceId(signal: GraphSignalUpperBound,
    graphEditor: GraphEditor[GraphIdUpperBound, GraphSignalUpperBound]): Boolean = {
    val (result, t) = time(super.deliverSignalWithoutSourceId(signal, graphEditor))
    deliverTime += t
    result
  }
  abstract override def executeCollectOperation(graphEditor: GraphEditor[GraphIdUpperBound, GraphSignalUpperBound]): Unit = {
    val (_, t) = time(super.executeCollectOperation(graphEditor))
    collectTime += t
  }
}

