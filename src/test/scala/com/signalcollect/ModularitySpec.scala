/*
 *  @author Philip Stutz
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
 *
 */

package com.signalcollect

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import java.util.Map.Entry
import org.specs2.analysis.ClassycleDependencyFinder
import org.specs2.specification.Analysis

/**
 * Thanks to Jan Machacek for the description
 * @ http://www.cakesolutions.net/teamblogs/2012/11/20/maven-sbt-and-modularisation/.
 */
@RunWith(classOf[JUnitRunner])
class ModularitySpec extends SpecificationWithJUnit with Analysis with ClassycleDependencyFinder {

  sequential

  "The modules" should {

    " have no direct dependencies between each other" in {

      // Prevents this test from being run if run with an older JVM version.
      System.getProperty("java.version") must startWith("1.7").orSkip("This test only works with java 1.7")

      // All except for configuration, factory, serialization and interfaces.
      val layerStructure = layers(
        "console",
        "coordinator",
        "examples",
        "factory",
        "logging",
        "messaging",
        "nodeprovisioning",
        "storage",
        "worker").withPrefix("com.signalcollect")
        .inTargetDir("target/scala-2.10")
      layerStructure must beRespected
    }
  }

}