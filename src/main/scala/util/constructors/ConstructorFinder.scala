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

package util.constructors

import java.lang.reflect.Constructor


/*
 * Contains functions to automatically determine the appropriate constructor for parameters 
 * and to create instances from a class and parameters for a constructor of that class. 
 */
object ConstructorFinder {

  case class ConstructorException(msg: String) extends Exception(msg)

  def getConstructorForParameters[Type](parameters: Seq[Any])(implicit m: Manifest[Type]): Constructor[_] = {
    val clazz = m.erasure
    getConstructorForParameters(clazz, parameters)
  }
  
  /*
   * Returns a constructor for class @clazz that can be invoked with the submitted @parameters.
   * If no suitable constructor is found a ConstructorException is thrown.
   */
  def getConstructorForParameters(clazz: Class[_], parameters: Seq[Any]): Constructor[_] = {
    val submittedParameterTypes: Seq[Class[_]] = parameters map (_.asInstanceOf[AnyRef].getClass)
    val constructors = clazz.getConstructors
    for (constructor <- constructors) {
      val constructorParameterTypes = constructor.getParameterTypes
      if (constructorParameterTypes.size.equals(submittedParameterTypes.size)) {
        val parametersMatch: Boolean = constructorParameterTypes.zipWithIndex forall {
          case (constructorParameterType, index) =>
            var submittedParameterType = submittedParameterTypes(index)
            isConstructorParameterTypeCompatibleWithSubmittedParameterType(constructorParameterType, submittedParameterType)
        }
        if (parametersMatch) {
          return constructor
        }
      }
    }
    throw ConstructorException("Class " + clazz.getSimpleName + ": No constructor found to match parameter types: " + submittedParameterTypes.mkString("[", ", ", "]") + "\n"
      + "Available constructors: " + constructors.map(_.getParameterTypes.map(_.getSimpleName).mkString("[", ", ", "]")).mkString(","))
  }

  /*
   * Checks if a submitted parameter type is compatible with the required parameter type in the constructor
   */
  def isConstructorParameterTypeCompatibleWithSubmittedParameterType(constructorType: Class[_], submittedType: Class[_]): Boolean = {
    var submittedParameterType = submittedType
	// primitives have an associated class object that is different from the class object of the respective wrapper class 
    if (constructorType.isPrimitive && !submittedParameterType.isPrimitive) {
      // adjust for this by changing the submitted parameter type to its primitive counterpart, if that is possible
      try {
        submittedParameterType = submittedParameterType.getField("TYPE").get(null).asInstanceOf[Class[_]]
      } catch { case _ => }
    }
    constructorType.isAssignableFrom(submittedParameterType)
  }

  /*
   * Creates an instance of a class given suitable parameters and the implicit class manifest.
   * Throws a ConstructorException exception if no matching constructor is found.
   */
  def newInstanceFromManifest[InstanceType](parameters: Seq[Object])(implicit m: Manifest[InstanceType]): InstanceType = {
    val clazz = m.erasure
    val constructor = getConstructorForParameters(clazz, parameters)
    val result = constructor.newInstance(parameters: _*)
    result.asInstanceOf[InstanceType]
  }

  /*
   * Java compatible version without manifest.
   */
  def newInstanceFromClass[InstanceType](clazz: Class[InstanceType])(parameters: Seq[Object]): InstanceType = {
    val constructor = getConstructorForParameters(clazz, parameters)
    val result = constructor.newInstance(parameters: _*)
    result.asInstanceOf[InstanceType]
  }
  
}