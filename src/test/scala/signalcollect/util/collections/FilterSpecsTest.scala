package signalcollect.util.collections

import org.junit.Test
import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher

import scala.collection.mutable.{ IndexedSeq, ArrayBuffer, ListBuffer }

@Test
class FilterSpecsTest extends SpecificationWithJUnit {
  
  "bySuperClass must not filter when invoked with ListBuffer and ListBuffer" in {
    Filter.bySuperClass(classOf[ListBuffer[Any]], new ListBuffer[Any]) must beSome
  }

  "bySuperClass must not filter when invoked with IndexedSeq and ArrayBuffer" in {
    Filter.bySuperClass(classOf[IndexedSeq[Any]], new ArrayBuffer[Any]) must beSome
  }

  "bySuperClass must filter when invoked with IndexedSeq and ListBuffer" in {
    Filter.bySuperClass(classOf[IndexedSeq[Any]], new ListBuffer[Any]) must beNone
  }

  "bySuperClass must not filter when invoked with Int and 1" in {
    Filter.bySuperClass(classOf[Int], 1) must beSome
  }

  "bySuperClass must filter when invoked with Int and 1L" in {
    Filter.bySuperClass(classOf[Int], 1L) must beNone
  }

  "bySuperClass must not filter when invoked with Any and 1" in {
    Filter.bySuperClass(classOf[Any], 1) must beSome
  }

  "bySuperClass must not filter when invoked with AnyVal and 1" in {
    Filter.bySuperClass(classOf[AnyVal], 1) must beSome
  }

}