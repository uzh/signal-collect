package com.signalcollect.javaapi.examples;

import com.signalcollect.javaapi.*;

@SuppressWarnings("serial")
public class LinkJ extends EdgeJ<PageJ> {

	public LinkJ(Integer sourceId, Integer targetId) {
		super(sourceId, targetId);
	}
	
	public Object signal(PageJ sourceVertex) {
		return sourceVertex.getState() * weight() / sourceVertex.getSumOfOutWeights();
	}
}