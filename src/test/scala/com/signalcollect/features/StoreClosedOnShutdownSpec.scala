/*
 *  @author Philip Stutz
 *
 *  Copyright 2015 iHealth Technologies
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

package com.signalcollect.features

import org.scalatest.{ FlatSpec, Matchers }

import com.signalcollect.GraphBuilder
import com.signalcollect.interfaces.{ Storage, StorageFactory }
import com.signalcollect.storage.VertexMapStorage
import com.signalcollect.util.TestAnnouncements

object TestStorageFactory extends StorageFactory[Any, Any] {
  def createInstance: Storage[Any, Any] = TestStorage
}

object TestStorage extends VertexMapStorage[Any, Any] {
  var closed = false
  override def close(): Unit = {
    closed = true
  }
}

class StoreClosedOnShutdownSpec extends FlatSpec with Matchers with TestAnnouncements {

  "Store" should "be closed on shutdown" in {
    val graph = GraphBuilder.withStorageFactory(TestStorageFactory).build
    graph.shutdown
    assert(TestStorage.closed == true)
  }

}
