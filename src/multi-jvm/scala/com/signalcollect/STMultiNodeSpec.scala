package com.signalcollect

import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest._

/**
 * Created by blingannagari on 29/09/15.
 */
trait STMultiNodeSpec extends MultiNodeSpecCallbacks with WordSpecLike with ShouldMatchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}
