package com.signalcollect.triplerush

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbInt
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.prop.Checkers
import com.signalcollect.GraphEditor
import org.scalamock.scalatest.MockFactory

class VertexSpec extends FlatSpec with ShouldMatchers with Checkers with MockFactory {

  import Tree._

  val mockGraphEditor = mock[GraphEditor[Any, Any]]
  
  implicit def arbTree[G](implicit g: Arbitrary[G]): Arbitrary[Tree[G]] = Arbitrary(genTree[G])

  def genTree[G](implicit g: Arbitrary[G]): Gen[Tree[G]] = Gen.oneOf(Gen.const(Leaf), genInner[G])

  def genInner[G](implicit g: Arbitrary[G]): Gen[Tree[G]] = for {
    v <- arbitrary[G]
    l <- genTree[G]
    r <- genTree[G]
  } yield Inner(v, l, r)

  "Tree" should "correctly sum integers" in {
    check(
      (v: Int, l: Tree[Int], r: Tree[Int]) => {
        val newTree = Inner(v, l, r)
        sum(newTree) == v + sum(l) + sum(r)
      })
  }

  it should "support pre-order traversal" in {
    check(
      (v: Int, l: Tree[Int], r: Tree[Int]) => {
        val newTree = Inner(v, l, r)
        preOrder(newTree) == v :: preOrder(l) ::: preOrder(r)
      })
  }

  it should "support in-order traversal" in {
    check(
      (v: Int, l: Tree[Int], r: Tree[Int]) => {
        val newTree = Inner(v, l, r)
        Tree.inOrder(newTree) == Tree.inOrder(l) ::: (v :: Tree.inOrder(r))
      })
  }

  it should "support post-order traversal" in {
    check(
      (v: Int, l: Tree[Int], r: Tree[Int]) => {
        val newTree = Inner(v, l, r)
        postOrder(newTree) == postOrder(l) ++ (postOrder(r) :+ v)
      })
  }

}

trait Tree[+G]

case class Inner[G](value: G, left: Tree[G], right: Tree[G]) extends Tree[G]

case object Leaf extends Tree[Nothing]

object Tree {

  def sum[G: Numeric](t: Tree[G]): G = {
    val numeric = implicitly[Numeric[G]]
    t match {
      case Leaf => numeric.zero
      case Inner(v, l, r) => numeric.plus(numeric.plus(v, sum(l)), sum(r))
    }
  }

  def preOrder[G](t: Tree[G]): List[G] = {
    t match {
      case Leaf => Nil
      case Inner(v, l, r) => v :: preOrder(l) ::: preOrder(r)
    }
  }

  def inOrder[G](t: Tree[G]): List[G] = {
    t match {
      case Leaf => Nil
      case Inner(v, l, r) => inOrder(l) ::: (v :: inOrder(r))
    }
  }

  def postOrder[G](t: Tree[G]): List[G] = {
    t match {
      case Leaf => Nil
      case Inner(v, l, r) => postOrder(l) ++ (postOrder(r) :+ v)
    }
  }

}
