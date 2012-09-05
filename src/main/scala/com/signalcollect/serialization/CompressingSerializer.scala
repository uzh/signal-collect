/*
 *  @author Philip Stutz
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

package com.signalcollect.serialization

import com.signalcollect.storage.DefaultStorage
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream }
import java.util.zip.{ Deflater, Inflater, DeflaterOutputStream, InflaterInputStream }

/**
 * Companion object
 */
object CompressingSerializer extends CompressingSerializer

/**
 * Serializes objects with the default Serializer and
 * uses compression to reduce the size of the serialized objects.
 */
trait CompressingSerializer extends DefaultSerializer {
  
  /**
   * Serializes an object and compresses it with the default compression settings
   * 
   * @param inputObject the object to serialize
   * @return serialized object as byte array
   */
  override def write[A](inputObject: A): Array[Byte] = {
    val uncompressed = super.write(inputObject)
    compress(uncompressed)
  }
  
  /**
   * Uncompresses and deserializes an object
   * 
   * @param the serialized object as byte array
   */
  override def read[A](buffer: Array[Byte]): A = {
    val uncompressed = decompress(buffer)
    super.read(uncompressed)
  }
  
  /**
   * Compresses the byte array using the ZLIB compression library
   * The resulting array has the exact size of the output i.e. the buffer is trimmed to the actual size of its content
   * 
   * @param input the array to compress
   * @param compressionLevel determines tradeoff between speed and compression
   */
  def compress(input: Array[Byte], compressionLevel: Int = Deflater.DEFAULT_COMPRESSION, intermediateArraySize: Option[Int] = None): Array[Byte] = {
    val output = new Array[Byte](intermediateArraySize.getOrElse(8192 + input.size))
    val compresser = new Deflater(compressionLevel)
    compresser.setInput(input)
    compresser.finish
    val compressedDataLength = compresser.deflate(output)
    val trimmedResult = new Array[Byte](compressedDataLength)
    trimmedResult.clone
    output.copyToArray(trimmedResult, 0)
    trimmedResult
  }

  
  /**
   * Uncompresses a compressed byte array using the ZLIB library
   * The resulting array has the exact size of the output i.e. the buffer is trimmed to the actual size of its content
   * 
   * @param the compressed byte array
   * @result the uncompressed version
   */
  def decompress(input: Array[Byte], intermediateArraySize: Option[Int] = None): Array[Byte] = {
    val decompresser = new Inflater
    decompresser.setInput(input)
    val result = new Array[Byte](intermediateArraySize.getOrElse(8192 + input.size * 10))
    val resultLength = decompresser.inflate(result)
    decompresser.end
    val trimmedResult = new Array[Byte](resultLength)
    result.copyToArray(trimmedResult, 0)
    trimmedResult
  }
}

/**
 * Allows mixing the compressed serializer to override the default serializer of a storage 
 */
trait CompressedSerialization extends DefaultStorage {
  override def serializer = CompressingSerializer
}