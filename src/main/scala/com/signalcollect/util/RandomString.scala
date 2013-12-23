/*
 *  @author Philip Stutz
 *
 *  Copyright 2013 University of Zurich
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

package com.signalcollect.util

import scala.util.Random

object RandomString {
  def alpha = "abcdefghijklmnopqrstuvwxyz"
  def numeric = "0123456789"
  def alphaNumeric = alpha + numeric

  def generate(length: Int, characters: String = alpha): String = {
    def randomIndex = Random.nextInt(characters.length)
    def randomChar = characters(randomIndex)
    val sb = new StringBuilder(length)
    for (i <- 0 until length) {
      sb.append(randomChar)
    }
    sb.toString
  }

}