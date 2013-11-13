package com.signalcollect.util

import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.prop.Checkers

class IntsSpec extends FlatSpec with ShouldMatchers with Checkers {

  "IntSet" should "store sets of Ints" in {
    check(
      (ints: Array[Int]) => {
        val intSet = ints.toSet
        val compact = Ints.createCompactSet(ints)
        new IntSet(compact).toSet == intSet
      },
      minSuccessful(1000))
  }

  it should "support the 'contains' operation" in {
    check(
      (ints: Array[Int], item: Int) => {
        val intSet = ints.toSet
        val compact = Ints.createCompactSet(ints)
        new IntSet(compact).contains(item) == intSet.contains(item)
      },
      minSuccessful(1000))
  }

  it should "support the 'insert' operation" in {
    check(
      (ints: Array[Int], item: Int) => {
        val intSet = ints.toSet + item
        val compact = Ints.createCompactSet(ints)
        new IntSet(new IntSet(compact).insert(item)).toSet == intSet
      },
      minSuccessful(1000))
  }

  "SearchableIntSet" should "store sets of Ints" in {
    check(
      (ints: Array[Int]) => {
        val intSet = ints.toSet
        val searchable = Ints.createSearchableSet(ints)
        new SearchableIntSet(searchable).toSet == intSet
      },
      minSuccessful(1000))
  }

  it should "support the 'contains' operation" in {
    check(
      (ints: Array[Int], item: Int) => {
        val intSet = ints.toSet
        val searchable = Ints.createSearchableSet(ints)
        new SearchableIntSet(searchable).contains(item) == intSet.contains(item)
      },
      minSuccessful(1000))
  }

  it should "support the 'insertIndex' operation" in {
    check(
      (ints: Array[Int], item: Int) => {
        val sortedInts = ints.distinct.toArray.sorted
        val referenceInsertIndex = {
          var index = -1
          val length = sortedInts.length
          for (i <- 0 until length) {
            if (index == -1) {
              if (sortedInts(i) >= item) {
                index = i
              } else if (i == length - 1) {
                index = length
              }
            }
          }
          math.max(index, 0)
        }
        val searchable = Ints.createSearchableSet(ints)
        val index = new SearchableIntSet(searchable).insertIndex(item)
        index == referenceInsertIndex
      },
      minSuccessful(1000))
  }

  it should "support the 'insert' operation" in {
    check(
      (ints: Array[Int], item: Int) => {
        val intList = (ints.toSet + item).toList.sorted
        val searchable = Ints.createSearchableSet(ints)
        val actual = new SearchableIntSet(new SearchableIntSet(searchable).insert(item)).toList
        actual == intList
      },
      minSuccessful(1000))
  }

}
