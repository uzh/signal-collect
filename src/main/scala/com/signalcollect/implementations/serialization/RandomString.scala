package com.signalcollect.implementations.serialization

object RandomString {
  def apply(prefix: String, length: Int): String = {
	  val chars = (('a' to 'z') ++ ('A' to 'Z') ++ ('1' to '9')).toList
	  var res = prefix
	  for(i <- 0 to length) {
	 	  res+=chars(scala.util.Random.nextInt(chars.size))
	  }
	  res
  }
}