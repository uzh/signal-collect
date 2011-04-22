package signalcollect.implementations.serialization

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

import signalcollect.interfaces.Vertex
import signalcollect.api.SignalMapVertex


trait DefaultSerializer {

  def write[A](inputObject: A): Array[Byte] = {
    val barr = new java.io.ByteArrayOutputStream(512)
    val out = new java.io.ObjectOutputStream(barr)
    out.writeObject(inputObject)
    out.close()
    barr.toByteArray()
  }

  def read[A](buffer: Array[Byte]): A= {
	val input = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(buffer))
	val obj = input.readObject()
	input.close()
	obj.asInstanceOf[A]
  }
}