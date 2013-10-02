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
  val compressionLevel: Int = -1,
  val noHeaders: Boolean = true,
  val bufferSize: Int = 128 * 1024)
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
      case io: IOException =>
        outputChunked.endChunks
        throw new KryoException(io)
    }
    outputChunked.endChunks
    //    val in = deflater.getTotalIn
    //    val out = deflater.getTotalOut
    //    println(s"Compressed size: ${((out.toDouble / in) * 100).round}%")
    //    println(s"Bytes saved: ${in - out}")
  }

  def read(kryo: Kryo, input: Input, tpe: Class[G]): G = {
    val inflater = new Inflater(noHeaders)
    // The inflater would read from input beyond the compressed bytes
    // if chunked enoding wasn't used.
    val inputChunked = new InputChunked(input, bufferSize)
    val inflaterStream = new InflaterInputStream(inputChunked, inflater)
    val inflaterInput = new Input(inflaterStream, bufferSize)
    val deserialized = kryo.readObject(inflaterInput, tpe, serializer)
    deserialized
  }

  override def copy(kryo: Kryo, original: G): G = {
    serializer.copy(kryo, original)
  }
}