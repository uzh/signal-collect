package com.signalcollect.util

import org.scalatest._
import org.scalacheck.Arbitrary
import org.scalatest.prop.Checkers
import org.scalacheck.Gen
import com.signalcollect.util.Ints._

class IntsSpec extends FlatSpec with ShouldMatchers with Checkers {

  "IntSet" should "compress sets of Ints" in {
    check(
      (ints: Array[Int]) => {
        val intSet = ints.toSet
        val compact = Ints.create(ints)
        IntSet(compact).toSet == intSet
      },
      minSuccessful(1000))
  }

  it should "support the 'contains' operation" in {
    check(
      (ints: Array[Int], item: Int) => {
        val intSet = ints.toSet
        val compact = Ints.create(ints)
        IntSet(compact).contains(item) == intSet.contains(item)
      },
      minSuccessful(1000))
  }

  it should "support the 'insert' operation" in {
    check(
      (ints: Array[Int], item: Int) => {
        val intSet = ints.toSet + item
        val compact = Ints.create(ints)
        IntSet(IntSet(compact).insert(item)).toSet == intSet
      },
      minSuccessful(1000))
  }

}
