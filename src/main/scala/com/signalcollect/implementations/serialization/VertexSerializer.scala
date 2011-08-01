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
 */

package com.signalcollect.implementations.serialization

import com.signalcollect.interfaces.{ Vertex, Serializer, Edge, SignalMessage }
import com.signalcollect.api.SignalMapVertex
import com.signalcollect.examples._
import java.util.LinkedList
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream }

/**
 * Special serializer that serializes some vertices more efficiently than the default version. 
 */
object VertexSerializer extends VertexSerializer

trait VertexSerializer {

  def write[A](inputObject: A): Array[Byte] = {
    val barr = new ByteArrayOutputStream(512)
    val out = new ObjectOutputStream(barr)
    inputObject match {
//      case a: Page => out.writeByte(1); writePage(out, inputObject.asInstanceOf[Page])
      case _ => out.writeByte(Byte.MaxValue); out.writeObject(inputObject)

    }
    out.writeObject(inputObject)
    out.close()
    barr.toByteArray()
  }

  def read[A](buffer: Array[Byte]): Vertex = {
    val input = new ObjectInputStream(new ByteArrayInputStream(buffer))
    val storedType = input.readByte
    storedType match {
//      case 1 => val page = readPage(input); input.close; page
      case _ => val v = input.readObject; input.close; v.asInstanceOf[Vertex]
    }
  }

//  protected def writePage(out: ObjectOutputStream, page: Page) {
//    out.writeInt(page.id.asInstanceOf[Int])
//    out.writeDouble(page.getDampingFactor)
//    out.writeDouble(page.state)
//    out.writeObject(page.getOutgoingEdges)
//    out.writeBoolean(page.getOutgoingEdgesAdded)
//    out.writeObject(page.getLastSignalState)
//    out.writeObject(page.getMessageInbox)
//
//    //Sum of out weights
//    out.writeDouble(page.sumOfOutWeights)
//    //most recent signals
//    out.writeObject(page.getMostRecentSignalMap)
//  }
//
//  protected def readPage(in: ObjectInputStream): Page = {
//    val s_id = in.readInt
//    val s_dampingFactor = in.readDouble
//    val page = new Page(s_id, s_dampingFactor)
//    page.state = in.readDouble
//    val edges = in.readObject.asInstanceOf[scala.collection.mutable.HashMap[(Any, Any, String), Edge[Any, _]]]
//    page.setOutgoingEdges(edges)
//    page.setOutgoingEdgeAddedSinceSignalOperation(in.readBoolean)
//    page.setLastSignalState(in.readObject.asInstanceOf[Option[Double]])
//    page.setMessageInbox(in.readObject.asInstanceOf[LinkedList[SignalMessage[_, _, _]]])
//    page.sumOfOutWeights = in.readDouble
//    page.setMostRecentSignalMap(in.readObject.asInstanceOf[scala.collection.mutable.Map[Any, Double]])
//    in.close
//    page
//  }

}