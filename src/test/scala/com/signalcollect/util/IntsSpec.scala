package com.signalcollect.util

import org.scalatest._
import org.scalacheck.Arbitrary
import org.scalatest.prop.Checkers
import org.scalacheck.Gen
import com.signalcollect.util.Ints._

class IntsSpec extends FlatSpec with ShouldMatchers with Checkers {

  "IntSet" should "correctly compress sets of Ints" in {
    check(
      (ints: Array[Int]) => {
        val intSet = ints.toSet
        println(intSet)
        val compact = Ints.create(ints)
        val expanded = IntSet(compact).toSet
        expanded == intSet
      },
      minSuccessful(200))
  }

}
