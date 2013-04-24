/*
 *  @author Silvan Troxler
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
 */

package com.signalcollect.console

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import com.signalcollect.GraphBuilder
import org.specs2.runner.JUnitRunner
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.io.IOException
import java.net.ServerSocket

@RunWith(classOf[JUnitRunner])
class ConsoleServerSpec extends SpecificationWithJUnit with Mockito {

  sequential

  "ConsoleServer" should {
    
    "start successfully" in {
      val port = 8098
      val socketAddress = new InetSocketAddress(InetAddress.getByName(null), port)
      val timeoutInMilliseconds = 5000
      val socket = new Socket
      var isServerOnline = false

      val graph = GraphBuilder.withConsole(true, port).build
      
      try {
        socket.connect(socketAddress, timeoutInMilliseconds)
        isServerOnline = true
      } catch {
        case _ : Throwable => 
      } finally {
        try {
            socket.close();
        } catch { case _ : Throwable => }
      }

      isServerOnline === true
    }

  }


}