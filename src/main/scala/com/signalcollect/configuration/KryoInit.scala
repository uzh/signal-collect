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
    kryo.register(classOf[Array[Int]])
    kryo.register(classOf[Array[Long]])
    kryo.register(classOf[Array[Float]])
    kryo.register(classOf[Array[Double]])
    kryo.register(classOf[Array[Boolean]])
    kryo.register(classOf[Array[String]])
    kryo.register(classOf[Array[Object]])
    kryo.register(classOf[Array[Array[Int]]])
    kryo.register(classOf[Array[Array[Long]]])
    kryo.register(classOf[Array[Array[Float]]])
    kryo.register(classOf[Array[Array[Double]]])
    kryo.register(classOf[Array[Array[Boolean]]])
    kryo.register(classOf[Array[Array[String]]])
    kryo.register(classOf[Array[Array[Object]]])
    //registerWithCompression(kryo, classOf[Array[Array[Int]]])
  }

  protected def registerWithCompression(kryo: Kryo, c: Class[_]) {
    kryo.register(c)
    val s = kryo.getSerializer(c)
    kryo.register(c, new DeflateSerializer(s))
  }

}
