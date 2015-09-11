package com.signalcollect.configuration

import com.esotericsoftware.kryo.Kryo

class TestKryoInit extends KryoInit {
  override def customize(kryo: Kryo): Unit = {
    kryo.setReferences(true)
    kryo.setCopyReferences(true)
    register(kryo)
  }
}
