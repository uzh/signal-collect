package signalcollect.javaapi

import signalcollect.api.{ DefaultVertex => ScalaVertex }
import scala.collection.JavaConversions._
import java.lang.Iterable

class DefaultVertexJ[IdType, StateType](
  id: IdType,
  initialState: StateType)
  extends ScalaVertex[IdType, StateType](
    id: IdType,
    initialState: StateType) {

  def signalsJ[G](filterClass: Class[G]): java.lang.Iterable[G] = new Iterable[G] {
    def iterator = signals(filterClass).iterator
  }
  
  def uncollectedSignalsJ[G](filterClass: Class[G]): java.lang.Iterable[G] = new Iterable[G] {
    def iterator = uncollectedSignals(filterClass).iterator
  }
  
  def collect: StateType = null.asInstanceOf[StateType]
  
}
 
