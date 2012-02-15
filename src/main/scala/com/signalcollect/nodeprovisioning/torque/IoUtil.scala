/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package com.signalcollect.nodeprovisioning.torque

import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import ch.ethz.ssh2.StreamGobbler

object IoUtil {

  def streamToString(in: InputStream): String = {
    val reader = new BufferedReader(new InputStreamReader(in))
    val builder = new StringBuilder
    var line = reader.readLine
    while (line != null) {
      builder.append(line)
      builder.append("\n")
      line = reader.readLine
    }
    builder.toString
  }

  def printStream(in: InputStream) {
    val reader = new BufferedReader(new InputStreamReader(in))
    val builder = new StringBuilder
    var line = reader.readLine
    while (line != null) {
      println(line)
      line = reader.readLine
    }
  }
  
}