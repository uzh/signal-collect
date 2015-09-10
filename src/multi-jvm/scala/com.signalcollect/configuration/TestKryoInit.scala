package com.signalcollect.configuration

import com.esotericsoftware.kryo.Kryo

class TestKryoInit extends KryoInit {
  override def customize(kryo: Kryo): Unit = {
    kryo.setReferences(true)
    kryo.setCopyReferences(true)
    kryo.register(Class.forName("com.signalcollect.ClusterIntegrationSpec$$anonfun$test$1$$anonfun$apply$1$$anon$1"), 2037610320)
    kryo.register(Class.forName("com.signalcollect.examples.PageRankEdge"), 2037610321)
    kryo.register(Class.forName("com.signalcollect.examples.PageRankVertex"), 2037610322)
    register(kryo)
  }
}