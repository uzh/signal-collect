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
import com.esotericsoftware.kryo.Serializer
import com.signalcollect.interfaces.Request
import com.signalcollect.messaging.Command
import com.signalcollect.messaging.AkkaProxy
import com.signalcollect.messaging.Incrementor
import com.signalcollect.factory.messagebus._
import com.signalcollect.factory.mapper.DefaultMapperFactory
import com.signalcollect.worker.AkkaWorker
import com.signalcollect.WorkerCreator
import com.signalcollect.factory.worker.DefaultAkkaWorker

class KryoInit {

  def customize(kryo: Kryo): Unit = {
    kryo.setReferences(true) // Required for cycle between edges and vertices.
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
      register("com.signalcollect.interfaces.SignalMessage")
      register("com.signalcollect.interfaces.BulkSignal")
      register("com.signalcollect.interfaces.BulkSignalNoSourceIds")
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
      register("com.signalcollect.factory.scheduler.Throughput$")
      register("java.lang.Class")
      register("java.lang.Object")
      register("akka.actor.RepointableActorRef")
      register("scala.collection.convert.Wrappers$JMapWrapper")
      register("com.signalcollect.interfaces.EdgeId")
      register("com.signalcollect.interfaces.WorkerStatus")
      register("com.signalcollect.interfaces.NodeStatus")
      register("com.signalcollect.interfaces.Heartbeat")
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
      register("scala.collection.mutable.WrappedArray$ofRef")
      /*register("akka.dispatch.NullMessage$")*/ // Removed this class in akka 2.3.2
      register("akka.actor.SystemGuardian$RegisterTerminationHook$")
      register("akka.actor.ReceiveTimeout$")
      register("akka.remote.EndpointWriter$AckIdleCheckTimer$")
      register("akka.remote.ReliableDeliverySupervisor$GotUid")
      register("akka.remote.EndpointWriter$StoppedReading")
      register("akka.remote.ReliableDeliverySupervisor$Ungate$")
      register("akka.remote.EndpointWriter$FlushAndStop$")
      register("akka.remote.EndpointWriter$OutboundAck")
      register("akka.remote.EndpointWriter$StopReading")
      register("akka.remote.Ack")
      register("akka.actor.Address")
      register("akka.remote.SeqNo")
      register("com.signalcollect.WorkerCreator$$anonfun$create$1")
      register("scala.reflect.ManifestFactory$$anon$1")
      register("com.signalcollect.factory.storage.MemoryEfficientStorage$")
      register("com.signalcollect.factory.worker.DefaultAkkaWorker$")
      register("com.signalcollect.coordinator.OnIdle")
      register("com.signalcollect.worker.ScheduleOperations$")
      register("akka.actor.Terminated")
      register("akka.actor.LocalActorRef")
      register("akka.actor.SystemGuardian$TerminationHookDone$")
      register("com.signalcollect.interfaces.Request")
      register("com.signalcollect.deployment.EfficientPageRankVertex")
      register("com.signalcollect.messaging.Command")
      register("com.signalcollect.messaging.Incrementor$$anonfun$1")
      register("com.signalcollect.coordinator.DefaultCoordinator$$anonfun$1")
      register("com.signalcollect.DefaultGraph$$anonfun$10")
      register("com.signalcollect.factory.messagebus.AkkaMessageBusFactory$")
      register("com.signalcollect.factory.mapper.DefaultMapperFactory$")
      register("com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory")
      register("com.signalcollect.messaging.AbstractMessageBus$$anonfun$1")
      register("com.signalcollect.messaging.AbstractMessageBus$$anonfun$3")
      register("com.signalcollect.messaging.AbstractMessageBus$$anonfun$4")
      register("com.signalcollect.worker.AkkaWorker$$anonfun$1")
      register("com.signalcollect.deployment.SplitLoader")
      register("com.signalcollect.interfaces.AddVertex")
      register("scala.reflect.ManifestFactory$$anon$9")
      register("scala.reflect.ManifestFactory$$anon$12")
      register("com.signalcollect.examples.SplitLoader")
      register("com.signalcollect.worker.IncrementorForWorker")
      register("akka.remote.RemoteActorRef")
      register("akka.remote.RemoteWatcher$HeartbeatTick$")
      register("akka.remote.RemoteWatcher$ReapUnreachableTick$")
      //register("akka.actor.Identify")
      register("com.signalcollect.examples.EfficientPageRankVertex")
      register("com.signalcollect.TopKFinder")
      register("com.signalcollect.TopKFinder$$anonfun$1")
      register("scala.math.Ordering$$anon$9")
      register("scala.math.Ordering$$anonfun$by$1")
      register("scala.math.Ordering$Double$")
      registerClass(classOf[Array[Byte]])
      register("akka.remote.RemoteWatcher$HeartbeatTick$")
      register("akka.remote.RemoteWatcher$ReapUnreachableTick$")
      //register("akka.actor.Identify")
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
