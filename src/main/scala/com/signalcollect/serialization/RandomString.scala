/*
 *  @author Daniel Strebel
 *
 *  Copyright 2011 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.signalcollect.serialization

/**
 * Little utility to create a random string of variable length with an optional prefix
 */
object RandomString {
  def apply(prefix: String="", length: Int): String = {
	  val chars = (('a' to 'z') ++ ('A' to 'Z') ++ ('1' to '9')).toList
	  var res = prefix
	  for(i <- 0 to length) {
	 	  res+=chars(scala.util.Random.nextInt(chars.size))
	  }
	  res
  }
}