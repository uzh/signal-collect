package com.esotericsoftware.kryo.serializers

import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.KryoException
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.InputChunked
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.io.OutputChunked

class FixedDeflateSerializer[G](
  val serializer: Serializer[G],
  val compressionLevel: Int = 4,
  val noHeaders: Boolean = true,
  val bufferSize: Int = 8 * 1024)
    extends Serializer[G] {

  def write(kryo: Kryo, output: Output, toSerialize: G) {
    val deflater = new Deflater(compressionLevel, noHeaders)
    val outputChunked = new OutputChunked(output, bufferSize)
    val deflaterStream = new DeflaterOutputStream(outputChunked, deflater)
    val deflaterOutput = new Output(deflaterStream, bufferSize)
    kryo.writeObject(deflaterOutput, toSerialize, serializer)
    deflaterOutput.flush
    try {
      deflaterStream.finish
    } catch {
      case io: IOException => throw new KryoException(io)
    }
    outputChunked.endChunks
  }

  def read(kryo: Kryo, input: Input, tpe: Class[G]): G = {
    // The inflater would read from input beyond the compressed bytes if chunked enoding wasn't used.
    val inflaterStream = new InflaterInputStream(new InputChunked(input, bufferSize), new Inflater(noHeaders))
    kryo.readObject(new Input(inflaterStream, bufferSize), tpe, serializer)
  }

  override def copy(kryo: Kryo, original: G): G = {
    serializer.copy(kryo, original)
  }
}