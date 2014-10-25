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

class KryoInit {

  def customize(kryo: Kryo): Unit = {
    kryo.setReferences(false)
    kryo.setCopyReferences(false)
    register(kryo)
  }

  protected def register(kryo: Kryo): Unit = {
    try {
      def register(s: String) {
        kryo.register(Class.forName(s))
      }
      def registerClass(c: Class[_]) {
        kryo.register(c)
      }
      register("scala.Some")
      register("scala.None$")
      register("com.signalcollect.interfaces.SignalMessageWithSourceId")
      register("com.signalcollect.interfaces.SignalMessageWithoutSourceId")
      register("com.signalcollect.interfaces.BulkStatus")
      register("com.signalcollect.interfaces.BulkSignal")
      register("com.signalcollect.interfaces.BulkSignalNoSourceIds")
      register("com.signalcollect.interfaces.BulkSignalNoSourceIds$mcI$sp")
      register("com.signalcollect.interfaces.AddVertex")
      register("com.signalcollect.interfaces.AddEdge")
      register("com.signalcollect.WorkerCreator")
      register("scala.Tuple2")
      register("scala.Tuple3")
      register("scala.Tuple4")
      register("scala.Tuple5")
      register("scala.Tuple6")
      register("scala.Tuple7")
      register("scala.Tuple8")
      register("scala.Tuple9")
      register("scala.Tuple10")
      register("scala.Tuple11")
      register("scala.Tuple12")
      register("scala.collection.BitSet")
      register("scala.collection.SortedSet")
      register("scala.util.Left")
      register("scala.util.Right")
      register("scala.collection.SortedMap")
      register("com.signalcollect.configuration.GraphConfiguration")
      register("com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner")
      register("com.signalcollect.factory.scheduler.Throughput")
      register("com.signalcollect.factory.scheduler.LowLatency")
      register("java.lang.Class")
      register("java.lang.Object")
      register("akka.actor.RepointableActorRef")
      register("scala.collection.convert.Wrappers$JMapWrapper")
      register("com.signalcollect.interfaces.EdgeId")
      register("com.signalcollect.interfaces.WorkerStatus")
      register("com.signalcollect.interfaces.NodeStatus")
      register("com.signalcollect.interfaces.WorkerStatistics")
      register("com.signalcollect.interfaces.NodeStatistics")
      register("com.signalcollect.interfaces.SentMessagesStats")
      register("scala.collection.mutable.HashMap")
      register("scala.collection.immutable.HashMap$HashTrieMap")
      register("scala.collection.immutable.Map$EmptyMap$")
      register("scala.collection.immutable.Map$Map1")
      register("scala.collection.immutable.Map$Map2")
      register("scala.collection.immutable.Map$Map3")
      register("scala.collection.immutable.Map$Map4")
      register("scala.collection.immutable.HashSet$HashTrieSet")
      register("scala.collection.immutable.Set$EmptySet$")
      register("scala.collection.immutable.Set$Set1")
      register("scala.collection.immutable.Set$Set2")
      register("scala.collection.immutable.Set$Set3")
      register("scala.collection.immutable.Set$Set4")
      register("scala.collection.immutable.Nil$")
      register("scala.collection.immutable.$colon$colon")
      register("scala.collection.immutable.Vector")
      register("akka.actor.SystemGuardian$RegisterTerminationHook$")
      register("akka.actor.ReceiveTimeout$")
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
