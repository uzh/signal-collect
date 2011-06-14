/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package signalcollect.algorithms

import signalcollect.api._
import scala.util.Random

/**
 * 	This algorithm attempts to find a vertex coloring.
 * A valid vertex coloring is defined as an assignment of labels (colors)
 * 	to vertices such that no two vertices that share an edge have the same label.
 *
 * Usage restriction: this implementation *ONLY* works on *UNDIRECTED* graphs.
 * In Signal/Collect this means that there is either no edge between 2 vertices
 * or one in each direction.
 *
 * @param id: the vertex id
 * @param numColors: the number of colors (labels) used to color the graph
 */
class ColoredVertex(id: Any, numColors: Int, initialColor: Int, isFixed: Boolean) extends SignalMapVertex(id, initialColor) { //Random.nextInt(numColors) + 1

  /**
   * Indicates that every signal this vertex receives is
   * an instance of Int. This avoids type-checks/-casts.
   */
  override type UpperSignalTypeBound <: Int

  /** The set of available colors */
  val colors: Set[Int] = (1 to numColors).toSet

  /** Returns a random color from the set of available colors */
  def getRandomColor: Int = Random.nextInt(numColors) + 1

  /**
   * Variable that indicates if the neighbors of this vertex should be informed
   *  about its color choice. This is only the case if this vertex changed its
   *  color.
   */
  var informNeighbors: Boolean = false

  /**
   * Checks if one of the neighbors shares the same color. If so, the state is
   *  set to a random color and the neighbors are informed about this vertex'
   *  new color. If no neighbor shares the same color, we stay with the old color.
   */
  def collect: Int = {
    if (mostRecentSignalMap.values.iterator.contains(state)) {
      informNeighbors = true
      if (isFixed) {
        initialColor
      } else {
        val r = Random.nextDouble
        if (r > 0.8) {
          val freeColors = colors -- signals(classOf[Int])
          val numberOfFreeColors = freeColors.size
          if (numberOfFreeColors > 0) {
            freeColors.toSeq(Random.nextInt(numberOfFreeColors))
          } else {
            getRandomColor
          }
        } else {
          getRandomColor
        }
      }
    } else {
      informNeighbors = false
      state
    }
  }

  /**
   * The signal score is 1 if this vertex hasn't signaled before or if it has
   *  changed its color (kept track of by informNeighbors). Else it's 0.
   */
  override def scoreSignal = if (informNeighbors || lastSignalState == None) 1 else 0

}

/**
 * Builds a Vertex Coloring compute graph and executes the computation
 *
 * StateForwarderEdge is a built-in edge type that simply sends the state
 * of the source vertex as the signal, which means that this algorithm does
 * not require a custom edge type.
 */
object VertexColoring extends App {
  val cg = new DefaultComputeGraph()
  cg.add(new ColoredVertex(1, 2, 1, false))
  cg.add(new ColoredVertex(2, 2, 1, false))
  cg.add(new ColoredVertex(3, 2, 1, false))
  cg.add(new StateForwarderEdge(1, 2))
  cg.add(new StateForwarderEdge(2, 1))
  cg.add(new StateForwarderEdge(2, 3))
  cg.add(new StateForwarderEdge(3, 2))
  val stats = cg.execute
  println(stats)
  cg.foreachVertex(println(_))
  cg.shutdown
}