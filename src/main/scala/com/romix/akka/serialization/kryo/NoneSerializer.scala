/*
 *  @author Thomas Keller
 *
 *  Copyright 2013 University of Zurich
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
 *  This serializer was adapted from
 *  https://github.com/twitter/chill/blob/048500d9da7dee777a84f309998069f06b650926/src/main/scala/com/twitter/chill/SingletonSerializer.scala
 */

package com.romix.akka.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

class NoneSerializer extends Serializer[None.type] {

  override def read(kryo: Kryo, input: Input, cls: Class[None.type]): None.type = scala.None

  override def write(kryo: Kryo, output: Output, obj: None.type) = {}

}
