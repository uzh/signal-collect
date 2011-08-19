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

package com.signalcollect.javaapi;

import com.signalcollect.interfaces.*;

import scala.collection.JavaConversions;
import com.signalcollect.*;

import java.util.HashMap;
import java.util.LinkedList;

@SuppressWarnings("serial")
public abstract class DataFlowVertexJ<IdTypeParameter, StateTypeParameter, SignalTypeParameter> extends VertexJWithResetStateAfterSignaling<IdTypeParameter, StateTypeParameter, SignalTypeParameter> {

	public DataFlowVertexJ(IdTypeParameter vertexId, StateTypeParameter initialState, StateTypeParameter resetState) {
		super(vertexId, initialState, resetState);
	}
	
	protected HashMap<EdgeId<?, IdTypeParameter>, SignalTypeParameter> mostRecentSignalMap = new HashMap<EdgeId<?, IdTypeParameter>, SignalTypeParameter>();
	
	public void executeCollectOperation(scala.collection.Iterable<SignalMessage<?, ?, ?>> signalMessages, MessageBus<Object> messageBus) {
	    Iterable<SignalMessage<?, ?, ?>> javaMessages = JavaConversions.asJavaIterable(signalMessages);
	    LinkedList<SignalTypeParameter> uncollectedSignals = new LinkedList<SignalTypeParameter>();
	    for (SignalMessage<?, ?, ?> message : javaMessages) {
	    	@SuppressWarnings("unchecked")
			SignalTypeParameter castSignal = (SignalTypeParameter) message.signal();
	    	uncollectedSignals.add(castSignal);
	    }
	    setState(collect(uncollectedSignals));
	}
	
	public abstract StateTypeParameter collect(Iterable<SignalTypeParameter> uncollectedSignals);
  
}