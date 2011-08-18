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

@SuppressWarnings("serial")
public abstract class DataGraphVertexJ<IdTypeParameter, StateTypeParameter, SignalTypeParameter> extends VertexJ<IdTypeParameter, StateTypeParameter, SignalTypeParameter> {

	public DataGraphVertexJ(IdTypeParameter vertexId, StateTypeParameter initialState) {
		super(vertexId, initialState);
	}
	
	protected HashMap<EdgeId<?, IdTypeParameter>, SignalTypeParameter> mostRecentSignalMap = new HashMap<EdgeId<?, IdTypeParameter>, SignalTypeParameter>();
	
	public void executeCollectOperation(scala.collection.Iterable<SignalMessage<?, ?, ?>> signalMessages, MessageBus<Object> messageBus) {
	    Iterable<SignalMessage<?, ?, ?>> javaMessages = JavaConversions.asJavaIterable(signalMessages);
	    for (SignalMessage<?, ?, ?> message : javaMessages) {
	    	@SuppressWarnings("unchecked")
			SignalMessage<?, IdTypeParameter, SignalTypeParameter> castMessage = (SignalMessage<?, IdTypeParameter, SignalTypeParameter>) message;
	    	mostRecentSignalMap.put(castMessage.edgeId(), castMessage.signal());
	    }
	    setState(collect(mostRecentSignalMap.values()));
	}
	
	public abstract StateTypeParameter collect(Iterable<SignalTypeParameter> mostRecentSignals);
  
}