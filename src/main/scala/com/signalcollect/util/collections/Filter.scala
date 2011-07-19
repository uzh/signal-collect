package com.signalcollect.util.collections

object Filter {

  /**
   * Checks if the type of an instance corresponds exactly to a given filter class.
   * If the instance matches exactly with the class, then the instance is cast to
   * that class and returned as Some(instance). Else None is returned.
   */
  def byClass[G](filterClass: Class[G], instance: Any): Option[G] = {
    instance match {
      case t: Byte if filterClass == classOf[Byte] => Some(t.asInstanceOf[G])
      case t: Short if filterClass == classOf[Short] => Some(t.asInstanceOf[G])
      case t: Char if filterClass == classOf[Char] => Some(t.asInstanceOf[G])
      case t: Int if filterClass == classOf[Int] => Some(t.asInstanceOf[G])
      case t: Long if filterClass == classOf[Long] => Some(t.asInstanceOf[G])
      case t: Float if filterClass == classOf[Float] => Some(t.asInstanceOf[G])
      case t: Double if filterClass == classOf[Double] => Some(t.asInstanceOf[G])
      case t: Boolean if filterClass == classOf[Boolean] => Some(t.asInstanceOf[G])
      case t: Unit if filterClass == classOf[Unit] => Some(t.asInstanceOf[G])
      case reference if (reference.asInstanceOf[AnyRef].getClass == filterClass) => Some(reference.asInstanceOf[G])
      case other => None
    }
  }

  /**
   * Checks if the type of an instance is equal to a given class or to one of its subclasses.
   * 
   * @param filterClass the class to test against.
   * 
   * @return an Option containing the instance cast into an object of the type of the superclass if such a
   * cast is possible, None otherwise.
   */
  def bySuperClass[G](filterClass: Class[G], instance: Any): Option[G] = {
    instance match {
      case t: Byte if filterClass == classOf[Byte] => Some(t.asInstanceOf[G])
      case t: Short if filterClass == classOf[Short] => Some(t.asInstanceOf[G])
      case t: Char if filterClass == classOf[Char] => Some(t.asInstanceOf[G])
      case t: Int if filterClass == classOf[Int] => Some(t.asInstanceOf[G])
      case t: Long if filterClass == classOf[Long] => Some(t.asInstanceOf[G])
      case t: Float if filterClass == classOf[Float] => Some(t.asInstanceOf[G])
      case t: Double if filterClass == classOf[Double] => Some(t.asInstanceOf[G])
      case t: Boolean if filterClass == classOf[Boolean] => Some(t.asInstanceOf[G])
      case t: Unit if filterClass == classOf[Unit] => Some(t.asInstanceOf[G])
      case t: Any if filterClass == classOf[Any] => Some(t.asInstanceOf[G])
      case reference: AnyRef if filterClass.isAssignableFrom(reference.getClass) => Some(reference.asInstanceOf[G])
      case other => None
    }
  }
}