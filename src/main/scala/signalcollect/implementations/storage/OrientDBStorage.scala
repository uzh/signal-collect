/*
 *  @author Daniel Strebel
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

package signalcollect.implementations.storage

import signalcollect.interfaces._
import javax.persistence.{ Id, Version }
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLUpdate
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLDelete
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import com.orientechnologies.orient.core.db.`object`.{ ODatabaseObject, ODatabaseObjectPool, ODatabaseObjectTx }

/**
 * Adapter class for serialized vertices
 */
case class OrientWrapper(vertexID: String, var serializedVertex: Array[Byte]) {
  @Id
  var id: String = _
  def this() = this(null, null) // default ctor for unmarshalling
}

class OrientDBStorage(storage: Storage, DBLocation: String) extends VertexStore with DefaultSerializer {

  implicit def dbWrapper(db: ODatabaseObjectTx) = new {
    def queryBySql[T](sql: String, params: AnyRef*): List[T] = {
      val params4java = params.toArray
      val results: java.util.List[T] = db.query(new OSQLSynchQuery[T](sql), params4java: _*)
      results.asScala.toList
    }
  }

  val messageBus = storage.getMessageBus

  // Create DB or connect to existing
  var uri: String = "local:" + DBLocation
  var db: ODatabaseObjectTx = new ODatabaseObjectTx(uri)
  if (!db.exists) {
    db.create()
  } else {
    db.open("admin", "admin") //Default
  }
  db.getEntityManager.registerEntityClasses("signalcollect.implementations.storage") // Registers the adapter class

  def get(id: Any): Vertex[_, _] = {
    val serialized = db.queryBySql[OrientWrapper]("select from OrientWrapper where vertexID = ?", id.toString.asInstanceOf[AnyRef])
    if (serialized.isEmpty) {
      null
    } else {
      var vertex: Vertex[_, _] = read(serialized.last.serializedVertex)
      vertex.setMessageBus(messageBus)
      vertex
    }
  }

  def put(vertex: Vertex[_, _]): Boolean = {
    var alreadyStored = false
    if (size > 0) {
      val queryResults = db.queryBySql[OrientWrapper]("select from OrientWrapper where vertexID = ?", vertex.id.toString.asInstanceOf[AnyRef])
      alreadyStored = queryResults.size > 0
    }
    if (!alreadyStored) {
      db.save(new OrientWrapper(vertex.id.toString, write(vertex)))
      storage.toCollect.add(vertex.id)
      storage.toSignal.add(vertex.id)
      true
    } else {
      false
    }
  }

  def remove(id: Any) = {
    val queryResult = db.queryBySql[OrientWrapper]("select from OrientWrapper where vertexID = ?", id.toString.asInstanceOf[AnyRef])
    if(!queryResult.isEmpty) {
    	db.delete(queryResult.last)
    	storage.toSignal.remove(id)
    	storage.toCollect.remove(id)     
    }
  }

  def updateStateOfVertex(vertex: Vertex[_, _]) = {
    var wrappedVertex = db.queryBySql[OrientWrapper]("select from OrientWrapper where vertexID = ?", vertex.id.toString.asInstanceOf[AnyRef]).last
    wrappedVertex.serializedVertex = write(vertex)
    db.save(wrappedVertex)
  }

  def foreach[U](f: (Vertex[_, _]) => U) {
    val iterator = db.browseClass[OrientWrapper]("OrientWrapper")
    while (iterator.hasNext) {
      val vertex: Vertex[_, _] = read(iterator.next.serializedVertex)
      f(vertex)
    }
  }

  def size: Long = db.countClass(classOf[OrientWrapper])
}

/**
 * To allow mixing-in this storage implementation into a more general storage implementation
 */
trait Orient extends DefaultStorage {
  override protected def vertexStoreFactory = new OrientDBStorage(this, getRandomString("/tmp/orient", 3))
}