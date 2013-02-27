package com.romix.akka.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

// Adapted from https://github.com/twitter/chill/blob/develop/src/main/scala/com/twitter/chill/SingletonSerializer.scala
class NoneSerializer extends Serializer[None.type] {

  override def read(kryo: Kryo, input: Input, cls: Class[None.type]): None.type = scala.None
  
  override def write(kryo: Kryo, output: Output, obj: None.type) = {}

}
