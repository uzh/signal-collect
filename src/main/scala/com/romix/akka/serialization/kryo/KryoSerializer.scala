/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */

package com.romix.akka.serialization.kryo

import akka.serialization._
import akka.actor.ExtendedActorSystem
import akka.actor.ActorRef
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.util._
import KryoSerialization._
import com.esotericsoftware.minlog.{ Log => MiniLog }
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import scala.util.Success
import scala.util.Failure
import scala.reflect.ClassTag

class KryoSerializer(val system: ExtendedActorSystem) extends Serializer {

  import KryoSerialization._
  val log = Logging(system, getClass.getName)

  val settings = new Settings(system.settings.config)

  val mappings = settings.ClassNameMappings

  locally {
    log.debug("Got mappings: {}", mappings)
  }

  val classnames = settings.ClassNames

  locally {
    log.debug("Got classnames for incremental strategy: {}", classnames)
  }

  val bufferSize = settings.BufferSize

  locally {
    log.debug("Got buffer-size: {}", bufferSize)
  }

  val serializerPoolSize = settings.SerializerPoolSize

  val idStrategy = settings.IdStrategy

  locally {
    log.debug("Got id strategy: {}", idStrategy)
  }

  val serializerType = settings.SerializerType

  locally {
    log.debug("Got serializer type: {}", serializerType)
  }

  val implicitRegistrationLogging = settings.ImplicitRegistrationLogging
  locally {
    log.debug("Got implicit registration logging: {}", implicitRegistrationLogging)
  }

  val useManifests = settings.UseManifests
  locally {
    log.debug("Got use manifests: {}", useManifests)
  }

  val serializer = try new KryoBasedSerializer(getKryo(idStrategy, serializerType),
    bufferSize,
    serializerPoolSize,
    useManifests)
  catch {
    case e: Exception => {
      log.error("exception caught during akka-kryo-serialization startup: {}", e)
      throw e
    }
  }

  locally {
    log.debug("Got serializer: {}", serializer)
  }

  // This is whether "fromBinary" requires a "clazz" or not
  def includeManifest: Boolean = useManifests

  // A unique identifier for this Serializer
  def identifier = 123454323

  // Delegate to a real serializer
  def toBinary(obj: AnyRef): Array[Byte] = {
    val ser = getSerializer
    val bin = ser.toBinary(obj)
    releaseSerializer(ser)
    bin
  }

  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    val ser = getSerializer
    val obj = ser.fromBinary(bytes, clazz)
    releaseSerializer(ser)
    obj
  }

  val serializerPool = new ObjectPool[Serializer](serializerPoolSize, () => {
    new KryoBasedSerializer(getKryo(idStrategy, serializerType),
      bufferSize,
      serializerPoolSize,
      useManifests)
  })

  private def getSerializer = serializerPool.fetch
  private def releaseSerializer(ser: Serializer) = serializerPool.release(ser)

  private def getKryo(strategy: String, serializerType: String): Kryo = {
    val referenceResolver = if (settings.KryoReferenceMap) new MapReferenceResolver() else new ListReferenceResolver()
    val kryo = new Kryo(new KryoClassResolver(implicitRegistrationLogging), referenceResolver)
    // Support deserialization of classes without no-arg constructors
    kryo.setInstantiatorStrategy(new StdInstantiatorStrategy())
    // Support serialization of some standard or often used Scala classes
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    system.dynamicAccess.getClassFor[AnyRef]("scala.Enumeration$Val") match {
      case Success(clazz) => kryo.register(clazz)
      case Failure(e) => {
        log.error("Class could not be loaded and/or registered: {} ", "scala.Enumeration$Val")
        throw e
      }
    }
    kryo.addDefaultSerializer(classOf[scala.collection.Map[_, _]], classOf[ScalaMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Set[_]], classOf[ScalaSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.MapFactory[scala.collection.Map]], classOf[ScalaMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
    kryo.addDefaultSerializer(classOf[ActorRef], new ActorRefSerializer(system))
    kryo.addDefaultSerializer(None.getClass, new NoneSerializer)
    kryo.register(classOf[scala.Enumeration#Value], 10)
    kryo.register(None.getClass, 19)
    kryo.register(classOf[Array[Int]], 20)
    kryo.register(classOf[Array[Long]], 21)
    kryo.register(classOf[Array[Float]], 22)
    kryo.register(classOf[Array[Double]], 23)
    kryo.register(classOf[Array[Boolean]], 24)
    kryo.register(classOf[Array[Object]], 25)

    if (settings.KryoTrace)
      MiniLog.TRACE()

    strategy match {
      case "default" => {}

      case "incremental" => {
        kryo.setRegistrationRequired(false)

        for ((fqcn: String, idNum: String) <- mappings) {
          val id = idNum.toInt
          // Load class
          if (fqcn.startsWith("Array")) {
            val arrayOfString = fqcn.substring(6, fqcn.length - 1)
            system.dynamicAccess.getClassFor[AnyRef](arrayOfString) match {
              case Success(clazz) =>
                val exampleArray = ClassTag(clazz).newArray(1)
                kryo.register(exampleArray.getClass, id)
              case Failure(e) =>
                log.error(s"Class could not be loaded and/or registered: $fqcn")
                e.printStackTrace
                throw e
            }
          } else {
            system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
              case Success(clazz) => kryo.register(clazz, id)
              case Failure(e) => {
                log.error("Class could not be loaded and/or registered: {} ", fqcn)
                throw e
              }
            }
          }
        }

        for (classname <- classnames) {
          // Load class
          if (classname.startsWith("Array")) {
            val arrayOfString = classname.substring(6, classname.length - 1)
            system.dynamicAccess.getClassFor[AnyRef](arrayOfString) match {
              case Success(clazz) =>
                val exampleArray = ClassTag(clazz).newArray(1)
                kryo.register(exampleArray.getClass)
              case Failure(e) =>
                log.error(s"Class could not be loaded and/or registered: $classname")
                e.printStackTrace
                throw e
            }
          } else {
            system.dynamicAccess.getClassFor[AnyRef](classname) match {
              case Success(clazz) => kryo.register(clazz)
              case Failure(e) => {
                log.warning("Class could not be loaded and/or registered: {} ", classname)
                e.printStackTrace
                throw e
              }
            }
          }
        }
      }

      case "explicit" => {
        kryo.setRegistrationRequired(false)

        for ((fqcn: String, idNum: String) <- mappings) {
          val id = idNum.toInt
          // Load class
          if (fqcn.startsWith("Array")) {
            val arrayOfString = fqcn.substring(6, fqcn.length - 1)
            system.dynamicAccess.getClassFor[AnyRef](arrayOfString) match {
              case Success(clazz) =>
                val exampleArray = ClassTag(clazz).newArray(1)
                kryo.register(exampleArray.getClass, id)
              case Failure(e) =>
                log.error(s"Class could not be loaded and/or registered: $fqcn")
                e.printStackTrace
                throw e
            }
          } else {
            system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
              case Success(clazz) => kryo.register(clazz, id)
              case Failure(e) => {
                log.error("Class could not be loaded and/or registered: {} ", fqcn)
                e.printStackTrace
                throw e
              }
            }
          }
        }

        for (classname <- classnames) {
          // Load class
          system.dynamicAccess.getClassFor[AnyRef](classname) match {
            case Success(clazz) => kryo.register(clazz)
            case Failure(e) => {
              log.warning("Class could not be loaded and/or registered: {} ", classname)
              /* throw e */
            }
          }
        }
        kryo.setRegistrationRequired(true)
      }
    }

    serializerType match {
      case "graph" => kryo.setReferences(true)
      case _       => kryo.setReferences(false)
    }

    kryo
  }

}

/**
 * *
 * Kryo-based serializer backend
 */
class KryoBasedSerializer(val kryo: Kryo, val bufferSize: Int, val bufferPoolSize: Int, val useManifests: Boolean) extends Serializer {

  // This is whether "fromBinary" requires a "clazz" or not
  def includeManifest: Boolean = useManifests

  // A unique identifier for this Serializer
  def identifier = 12454323

  // "toBinary" serializes the given object to an Array of Bytes
  def toBinary(obj: AnyRef): Array[Byte] = {
    val buffer = getBuffer
    try {
      if (!useManifests)
        kryo.writeClassAndObject(buffer, obj)
      else
        kryo.writeObject(buffer, obj)
      buffer.toBytes()
    } finally
      releaseBuffer(buffer)
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  // into the optionally provided classLoader.
  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    if (!useManifests)
      kryo.readClassAndObject(new Input(bytes)).asInstanceOf[AnyRef]
    else {
      clazz match {
        case Some(c) => kryo.readObject(new Input(bytes), c).asInstanceOf[AnyRef]
        case _       => throw new RuntimeException("Object of unknown class cannot be deserialized")
      }
    }
  }

  val buf = new Output(bufferSize, 1024 * 1024)
  private def getBuffer = buf
  private def releaseBuffer(buffer: Output) = { buffer.clear() }

}

// Support pooling of objects. Useful if you want to reduce
// the GC overhead and memory pressure.
class ObjectPool[T](number: Int, newInstance: () => T) {

  private val size = new AtomicInteger(0)
  private val pool = new ArrayBlockingQueue[T](number)

  def fetch(): T = {
    val item = pool.poll
    if (item != null) {
      item
    } else {
      createOrBlock
    }
  }

  def release(o: T): Unit = {
    pool.offer(o)
  }

  def add(o: T): Unit = {
    pool.add(o)
  }

  private def createOrBlock: T = {
    size.get match {
      case e: Int if e == number => block
      case _                     => create
    }
  }

  private def create: T = {
    size.incrementAndGet match {
      case e: Int if e > number =>
        size.decrementAndGet; fetch()
      case e: Int => newInstance()
    }
  }

  private def block: T = {
    val timeout = 5000
    try {
      pool.poll(timeout, TimeUnit.MILLISECONDS)
    } catch {
      case t: Throwable => throw new Exception("Couldn't acquire object in %d milliseconds.".format(timeout))
    }
  }
}
