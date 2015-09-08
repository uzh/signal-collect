package com.signalcollect.configuration

import com.esotericsoftware.kryo.Kryo

class TestKryoInit1 extends KryoInit {
  override def customize(kryo: Kryo): Unit = {
    kryo.setReferences(true)
    kryo.setCopyReferences(true)
    register(kryo)
  }
}

class TestKryoInit2 extends KryoInit {
  override def customize(kryo: Kryo): Unit = {
    kryo.setReferences(true)
    kryo.setCopyReferences(true)
    register(kryo)
  }
}

class TestKryoInit3 extends KryoInit {
  override def customize(kryo: Kryo): Unit = {
    kryo.setReferences(true)
    kryo.setCopyReferences(true)
    register(kryo)
  }
}
