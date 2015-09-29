/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.DeflateSerializer
import com.signalcollect.interfaces._
import com.signalcollect.util.BitSet
import com.signalcollect.nodeprovisioning.local._
import com.signalcollect.factory.scheduler._
import com.signalcollect.WorkerCreator
import scala.collection._
import scala.collection.convert.Wrappers
import scala.collection.mutable.{ HashMap => MutableHashMap }
import scala.collection.immutable.{ HashMap => ImmutableHashMap }
import scala.collection.immutable.{ Map => ImmutableMap }
import scala.collection.immutable.{ HashSet => ImmutableHashSet }
import scala.collection.immutable.{ Set => ImmutableSet }
import scala.collection.immutable.{ Vector => ImmutableVector }
import scala.collection.immutable.{ TreeSet => ImmutableTreeSet }
import akka.actor.ReceiveTimeout
import akka.remote.transport.AkkaProtocolException
import scala.reflect.ManifestFactory

class KryoInit {

  def customize(kryo: Kryo): Unit = {
    kryo.setReferences(false)
    kryo.setCopyReferences(false)
    register(kryo)
  }

  def register(kryo: Kryo): Unit = {
    try {
      def register(s: String) {
        kryo.register(Class.forName(s))
      }
      def registerClass(c: Class[_]) {
        kryo.register(c)
      }
      registerClass(classOf[Some[_]])
      registerClass(None.getClass)
      registerClass(classOf[SignalMessageWithSourceId[_, _]])
      registerClass(classOf[SignalMessageWithoutSourceId[_, _]])
      registerClass(classOf[BulkStatus])
      registerClass(classOf[BulkSignal[_, _]])
      registerClass(classOf[BulkSignal[Int, _]])
      registerClass(classOf[BulkSignal[Long, _]])
      registerClass(classOf[BulkSignalNoSourceIds[_, _]])
      registerClass(classOf[AddVertex[_, _, _, _]])
      registerClass(classOf[AddEdge[_, _]])
      registerClass(classOf[WorkerCreator[_, _]])
      registerClass(classOf[Tuple2[_, _]])
      registerClass(classOf[Tuple3[_, _, _]])
      registerClass(classOf[Tuple4[_, _, _, _]])
      registerClass(classOf[Tuple5[_, _, _, _, _]])
      registerClass(classOf[Tuple6[_, _, _, _, _, _]])
      registerClass(classOf[Tuple7[_, _, _, _, _, _, _]])
      registerClass(classOf[Tuple8[_, _, _, _, _, _, _, _]])
      registerClass(classOf[Tuple9[_, _, _, _, _, _, _, _, _]])
      registerClass(classOf[Tuple10[_, _, _, _, _, _, _, _, _, _]])
      registerClass(classOf[BitSet])
      registerClass(classOf[SortedSet[_]])
      registerClass(classOf[SortedMap[_, _]])
      registerClass(classOf[Left[_, _]])
      registerClass(classOf[Right[_, _]])
      registerClass(classOf[GraphConfiguration[_, _]])
      registerClass(classOf[LocalNodeProvisioner[_, _]])
      registerClass(classOf[Throughput[_, _]])
      registerClass(classOf[LowLatency[_, _]])
      registerClass(classOf[Class[_]])
      registerClass(classOf[Object])
      registerClass(classOf[Wrappers.JMapWrapper[_, _]])
      registerClass(classOf[EdgeId[_]])
      registerClass(classOf[EdgeId[Int]])
      registerClass(classOf[EdgeId[Long]])
      registerClass(classOf[WorkerStatus])
      registerClass(classOf[NodeStatus])
      registerClass(classOf[WorkerStatistics])
      registerClass(classOf[NodeStatistics])
      registerClass(classOf[SentMessagesStats])
      registerClass(classOf[MutableHashMap[_, _]])
      registerClass(classOf[ImmutableHashMap.HashTrieMap[_, _]])
      registerClass(ImmutableMap.empty[AnyRef, AnyRef].getClass)
      registerClass(classOf[ImmutableMap.Map1[_, _]])
      registerClass(classOf[ImmutableMap.Map2[_, _]])
      registerClass(classOf[ImmutableMap.Map3[_, _]])
      registerClass(classOf[ImmutableMap.Map4[_, _]])
      registerClass(classOf[ImmutableMap.Map4[_, _]])
      registerClass(classOf[ImmutableHashSet.HashTrieSet[_]])
      registerClass(ImmutableSet.empty[AnyRef].getClass)
      registerClass(classOf[ImmutableSet.Set1[_]])
      registerClass(classOf[ImmutableSet.Set2[_]])
      registerClass(classOf[ImmutableSet.Set3[_]])
      registerClass(classOf[ImmutableSet.Set4[_]])
      registerClass(classOf[ImmutableTreeSet[_]])
      registerClass(Nil.getClass)
      registerClass(classOf[ImmutableVector[_]])
      registerClass(ReceiveTimeout.getClass)
      // TODO: Convert to safe notation.
      register("com.signalcollect.interfaces.SignalMessageWithoutSourceId$mcJ$sp")
      register("com.signalcollect.interfaces.BulkSignalNoSourceIds$mcI$sp")
      register("scala.collection.immutable.$colon$colon")
      register("com.signalcollect.WorkerCreator$$anonfun$create$1")
      register("scala.reflect.ManifestFactory$$anon$1")
      register("scala.reflect.ManifestFactory$$anon$9")
      register("scala.reflect.ManifestFactory$$anon$12")
      register("com.signalcollect.factory.storage.MemoryEfficientStorage")
      register("com.signalcollect.factory.worker.AkkaWorkerFactory")
      register("com.signalcollect.coordinator.OnIdle")
      register("com.signalcollect.worker.ScheduleOperations$")
      register("akka.actor.Terminated")
      register("akka.actor.LocalActorRef")
      register("akka.actor.SystemGuardian$TerminationHookDone$")
      register("akka.actor.StopChild")
      register("com.signalcollect.interfaces.Request")
      register("com.signalcollect.messaging.Command")
      register("com.signalcollect.messaging.Incrementor$$anonfun$1")
      register("com.signalcollect.coordinator.DefaultCoordinator$$anonfun$1")
      register("com.signalcollect.DefaultGraph$$anonfun$10")
      register("com.signalcollect.factory.messagebus.AkkaMessageBusFactory")
      register("com.signalcollect.factory.messagebus.IntIdDoubleSignalMessageBusFactory")
      register("com.signalcollect.factory.mapper.DefaultMapperFactory")
      register("com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory")
      register("com.signalcollect.messaging.AbstractMessageBus$$anonfun$1")
      register("com.signalcollect.messaging.AbstractMessageBus$$anonfun$3")
      register("com.signalcollect.messaging.AbstractMessageBus$$anonfun$4")
      register("com.signalcollect.worker.AkkaWorker$$anonfun$1")
      register("com.signalcollect.worker.IncrementorForWorker")
      register("akka.remote.RemoteActorRef")
      register("akka.remote.RemoteWatcher$HeartbeatTick$")
      register("akka.remote.RemoteWatcher$ReapUnreachableTick$")
      register("com.signalcollect.examples.PlaceholderEdge")
      register("com.signalcollect.examples.EfficientPageRankVertex")
      register("com.signalcollect.TopKFinder")
      register("com.signalcollect.TopKFinder$$anonfun$1")
      register("com.signalcollect.Vertex")
      register("com.signalcollect.util.MemoryEfficientSplayIntSet")
      register("scala.math.Ordering$$anon$9")
      register("scala.math.Ordering$$anonfun$by$1")
      register("scala.math.Ordering$Double$")
      register("com.signalcollect.util.SplayNode")
      register("com.signalcollect.util.IntHashMap")
      register("com.signalcollect.SumOfStates")
      register("scala.math.Numeric$DoubleIsFractional$")
      register("com.signalcollect.worker.StatsDue$")
      register("com.signalcollect.worker.Ping")
      register("com.signalcollect.worker.Pong")
      register("com.signalcollect.worker.StartPingPongExchange")
      register("com.signalcollect.factory.handler.DefaultEdgeAddedToNonExistentVertexHandlerFactory")
      register("com.signalcollect.factory.handler.DefaultExistingVertexHandlerFactory")
      register("com.signalcollect.factory.handler.DefaultUndeliverableSignalHandlerFactory")
      register("com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory$mcI$sp")
      register("com.signalcollect.factory.mapper.DefaultMapperFactory$mcI$sp")
      register("com.signalcollect.factory.handler.DefaultEdgeAddedToNonExistentVertexHandlerFactory$mcI$sp")
      register("com.signalcollect.factory.scheduler.Throughput$mcI$sp")
      register("com.signalcollect.factory.scheduler.Throughput$mcJ$sp")
      register("com.signalcollect.factory.storage.MemoryEfficientStorage$mcI$sp")
      register("com.signalcollect.factory.handler.DefaultUndeliverableSignalHandlerFactory$mcI$sp")
      register("com.signalcollect.node.IdleReportRequested")
      register("scala.reflect.ManifestFactory$$anon$11")
      register("com.signalcollect.examples.Location")
      register("com.signalcollect.examples.Path")
      registerClass(classOf[Array[com.signalcollect.interfaces.WorkerStatus]])
      registerClass(classOf[Array[com.signalcollect.interfaces.NodeStatus]])
      registerClass(classOf[Array[Byte]])
      registerClass(classOf[Array[Byte]])
      registerClass(classOf[Array[Int]])
      registerClass(classOf[Array[Long]])
      registerClass(classOf[Array[Short]])
      registerClass(classOf[Array[Float]])
      registerClass(classOf[Array[Double]])
      registerClass(classOf[Array[Boolean]])
      registerClass(classOf[Array[String]])
      registerClass(classOf[Array[Object]])
      registerClass(classOf[Array[AnyRef]])
      registerClass(classOf[Array[akka.actor.ActorRef]])
      registerClass(classOf[Array[Array[Byte]]])
      registerClass(classOf[Array[Array[Int]]])
      registerClass(classOf[Array[Array[Long]]])
      registerClass(classOf[Array[Array[Short]]])
      registerClass(classOf[Array[Array[Float]]])
      registerClass(classOf[Array[Array[Double]]])
      registerClass(classOf[Array[Array[Boolean]]])
      registerClass(classOf[Array[Array[String]]])
      registerClass(classOf[Array[Array[Object]]])
      registerClass(classOf[Array[Array[AnyRef]]])
      // Cluster
      register("akka.cluster.InternalClusterAction$GetClusterCoreRef$")
      register("akka.cluster.InternalClusterAction$PublisherCreated")
      register("akka.cluster.InternalClusterAction$Subscribe")
      register("akka.cluster.InternalClusterAction$JoinSeedNodes")
      register("akka.cluster.InternalClusterAction$Unsubscribe")
      register("akka.cluster.ClusterEvent$CurrentClusterState")
      register("akka.cluster.InternalClusterAction$JoinSeedNode$")
      register("akka.cluster.ClusterEvent$ClusterShuttingDown$")
      register("akka.actor.Status$Failure")
      register("akka.remote.EndpointWriter$FlushAndStop$")
      register("akka.remote.EndpointWriter$FlushAndStop$")
      register("akka.remote.EndpointWriter$AckIdleCheckTimer$")
      register("akka.cluster.InternalClusterAction$ReapUnreachableTick$")
      register("akka.cluster.InternalClusterAction$GossipTick$")
      register("akka.cluster.InternalClusterAction$LeaderActionsTick$")
      register("akka.remote.ReliableDeliverySupervisor$AttemptSysMsgRedelivery$")
      register("akka.remote.EndpointWriter$AckIdleCheckTimer$")
      register("akka.cluster.InternalClusterAction$LeaderActionsTick$")
      register("akka.remote.EndpointWriter$StoppedReading")
      register("akka.remote.EndpointWriter$StoppedReading")
      register("akka.remote.ReliableDeliverySupervisor$Ungate$")
      register("akka.remote.ReliableDeliverySupervisor$Ungate$")
      register("akka.dispatch.sysmsg.Watch")
      register("akka.remote.RemoteScope")
      register("akka.dispatch.sysmsg.DeathWatchNotification")
      register("akka.actor.Scope")
      register("akka.dispatch.sysmsg.Supervise")
      register("akka.actor.Address")
      register("akka.remote.RemoteSystemDaemon")
      registerClass(ManifestFactory.Long.getClass)
      registerClass(classOf[AkkaProtocolException])
      registerClass(classOf[akka.remote.transport.netty.NettyTransport$$anonfun$associate$1$$anon$2])
      register("akka.cluster.ClusterEvent$InitialStateAsSnapshot$")
      register("akka.cluster.ClusterEvent$ClusterDomainEvent")
      register("akka.cluster.ClusterEvent$MemberEvent")
      registerClass(classOf[Array[StackTraceElement]])
      register("akka.cluster.ClusterEvent$ReachabilityEvent")
      register("java.lang.StackTraceElement")
      register("java.util.Collections$UnmodifiableRandomAccessList")
      register("akka.cluster.Member$$anon$1")
      register("akka.remote.ReliableDeliverySupervisor$GotUid")
      register("akka.cluster.ClusterUserAction$JoinTo")
      register("akka.cluster.InternalClusterAction$MetricsTick$")
      register("akka.cluster.InternalClusterAction$PublishChanges")
      register("akka.cluster.Gossip")
      register("akka.cluster.Member")
      register("akka.remote.Ack")
      register("akka.remote.EndpointWriter$OutboundAck")
      register("akka.cluster.MemberStatus$Joining$")
      register("akka.cluster.ClusterHeartbeatSender$HeartbeatTick$")
      register("akka.cluster.InternalClusterAction$PublishEvent")
      register("akka.cluster.ClusterHeartbeatSender$HeartbeatTick$")
      register("akka.remote.SeqNo")
      register("akka.remote.EndpointWriter$StopReading")
      register("akka.cluster.UniqueAddress")
      register("akka.cluster.MemberStatus$Up$")
      register("akka.cluster.GossipOverview")
      register("akka.cluster.Reachability")
      register("akka.cluster.ClusterEvent$ClusterMetricsChanged")
      register("akka.cluster.VectorClock")
      register("scala.collection.immutable.TreeMap")
      register("scala.math.Ordering$String$")
      register("akka.cluster.ClusterEvent$MemberUp")
      register("akka.cluster.ClusterEvent$ReachabilityChanged")
      register("akka.cluster.ClusterEvent$SeenChanged")
      register("akka.cluster.ClusterEvent$LeaderChanged")
      register("akka.cluster.NodeMetrics")
      register("akka.cluster.ClusterHeartbeatSender$ExpectedFirstHeartbeat")
      register("akka.cluster.Metric")
      register("akka.cluster.EWMA")
      // private
      register("akka.actor.RepointableActorRef")
      register("akka.actor.SystemGuardian$RegisterTerminationHook$")
    } catch {
      case t: Throwable => t.printStackTrace
    }
    //registerWithCompression(kryo, classOf[Array[Array[Int]]])
  }

  protected def registerWithCompression(kryo: Kryo, c: Class[_]) {
    kryo.register(c)
    val s = kryo.getSerializer(c)
    kryo.register(c, new DeflateSerializer(s))
  }

}
