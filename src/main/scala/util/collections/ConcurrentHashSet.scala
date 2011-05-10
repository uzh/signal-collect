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

package util.collections;

import scala.collection.generic.GenericCompanion
import scala.collection.mutable.SetLike
import scala.collection.generic.GenericSetTemplate
import scala.collection.generic.CanBuildFrom
import scala.collection.generic.MutableSetFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.Map
import scala.collection.JavaConversions._
import scala.collection.mutable.Set

class ConcurrentHashSet[A](initialCapacity: Int = 16, loadFactor: Float = 0.75f, concurrencyLevel: Int = 16) extends Set[A]
  with GenericSetTemplate[A, ConcurrentHashSet]
  with SetLike[A, ConcurrentHashSet[A]]
  with Serializable {

  private val map = new ConcurrentHashMap[A, Object](initialCapacity, loadFactor, concurrencyLevel)

  object DUMMY extends Object

  override def companion: GenericCompanion[ConcurrentHashSet] = ConcurrentHashSet

  def contains(elem: A): Boolean = map.containsKey(elem)

  def +=(elem: A): this.type = { map.put(elem, DUMMY); this }
  def -=(elem: A): this.type = { map.remove(elem, DUMMY); this }

  def iterator: Iterator[A] = map.keySet.iterator

}

object ConcurrentHashSet extends MutableSetFactory[ConcurrentHashSet] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, ConcurrentHashSet[A]] = setCanBuildFrom[A]
  override def empty[A]: ConcurrentHashSet[A] = new ConcurrentHashSet[A]
}