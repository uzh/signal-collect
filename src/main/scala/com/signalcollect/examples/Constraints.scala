package com.signalcollect.examples

import scala.collection.mutable._

object ConstraintExample extends App {
  
  //  import Expression._
//  val fC: Constraint = Variable("Id3")*4+2==10
//  println(fC.satisfies(Map("Id3"-> 2)))

  
}
 //todo treat case when the index is not inside
	/// todo add +, - , / operators
// todo add brackets
//todo ?? precedence?


object Expression {
  implicit def toExpression(x: Int): Expression = new IntExpression(x)
  implicit def toExpression(x: String): Expression = new Variable(x)
  
}

case class IntExpression(x: Int) extends Expression {
  def evaluate(configuration: Map[Any, Double]): Double = x
  
  override def toString: String = {
    x.toString
  }
}

case class EqualsConstraint ( lhs: Expression, rhs: Expression, utilitySatisfied: Double = 1, utilityNonSatisfied: Double = 0) extends Constraint {

  def satisfies(configuration: Map[Any, Double]): Boolean = lhs.evaluate(configuration) == rhs.evaluate(configuration)
  def satisfiesInt(configuration: Map[Any, Double]): Int = if (satisfies(configuration)) {return 1} else {return 0}
  def utility(configuration: Map[Any, Double]): Double = if (satisfies(configuration)) utilitySatisfied else utilityNonSatisfied
  override def toString: String = {
    lhs.toString + "==" + rhs.toString
  }
}

case class LessEqualsConstraint( lhs: Expression, rhs: Expression, utilitySatisfied: Double = 1, utilityNonSatisfied: Double = 0) extends Constraint {

  def satisfies(configuration: Map[Any, Double]): Boolean = lhs.evaluate(configuration) <= rhs.evaluate(configuration)
  def satisfiesInt(configuration: Map[Any, Double]): Int = if (satisfies(configuration)) {return 1} else {return 0}
  def utility(configuration: Map[Any, Double]): Double = if (satisfies(configuration)) utilitySatisfied else utilityNonSatisfied
  override def toString: String = {
    lhs.toString + "<=" + rhs.toString
  }
}


case class NotEqualsConstraint( lhs: Expression, rhs: Expression, utilitySatisfied: Double = 1, utilityNonSatisfied: Double = 0) extends Constraint {

  def satisfies(configuration: Map[Any, Double]): Boolean = lhs.evaluate(configuration) != rhs.evaluate(configuration)
  def satisfiesInt(configuration: Map[Any, Double]): Int = if (satisfies(configuration)) {return 1} else {return 0}
  def utility(configuration: Map[Any, Double]): Double = if (satisfies(configuration)) utilitySatisfied else utilityNonSatisfied
  override def toString: String = {
    lhs.toString + "!=" + rhs.toString
  }
}

trait Constraint {
	def satisfies(configuration: Map[Any, Double]): Boolean
	def satisfiesInt(configuration: Map[Any, Double]): Int 
	def utility(configuration: Map[Any, Double]): Double
}


case class Multiplication(e1: Expression, e2: Expression) extends Expression {
  def evaluate(configuration: Map[Any, Double]): Double = {
    e1.evaluate(configuration) * e2.evaluate(configuration)
  }

  override def toString: String = {
    e1.toString + "*" + e2.toString
  }
}

case class Addition(e1: Expression, e2: Expression) extends Expression {
  def evaluate(configuration: Map[Any, Double]): Double = {
    e1.evaluate(configuration) + e2.evaluate(configuration)
  }

  override def toString: String = {
    e1.toString + "+" + e2.toString
  }
}

case class Subtraction(e1: Expression, e2: Expression) extends Expression {
  def evaluate(configuration: Map[Any, Double]): Double = {
    e1.evaluate(configuration) - e2.evaluate(configuration)
  }

  override def toString: String = {
    e1.toString + "-" + e2.toString
  }
}

case class Division(e1: Expression, e2: Expression) extends Expression {
  def evaluate(configuration: Map[Any, Double]): Double = {
    e1.evaluate(configuration) / e2.evaluate(configuration)
  }

  override def toString: String = {
    e1.toString + "/" + e2.toString
  }
}

trait Expression {
  def *(other: Expression): Expression = Multiplication(this, other)
  def +(other: Expression): Expression = Addition(this, other)
  def -(other: Expression): Expression = Subtraction(this, other)
  def /(other: Expression): Expression = Division(this, other)


  def evaluate(configuration: Map[Any, Double]): Double

  def ==(other: Expression): Constraint = EqualsConstraint(this, other)
  def <=(other: Expression): Constraint = LessEqualsConstraint(this, other)
  def !=(other: Expression): Constraint = NotEqualsConstraint(this, other)

}

case class Variable(name: Any) extends Expression {
  def evaluate(configuration: Map[Any, Double]): Double = {
    configuration(name)
  }
  
  override def toString: String = {
    name.toString()
  }
}