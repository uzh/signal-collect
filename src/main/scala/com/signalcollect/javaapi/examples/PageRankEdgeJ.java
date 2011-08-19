package com.signalcollect.javaapi.examples;

import com.signalcollect.javaapi.*;

@SuppressWarnings("serial")
public class PageRankEdgeJ extends EdgeJ<PageRankVertexJ> {

	public PageRankEdgeJ(Integer sourceId, Integer targetId) {
		super(sourceId, targetId);
	}
	
	public Object signal(PageRankVertexJ sourceVertex) {
		return sourceVertex.getState() * weight() / sourceVertex.getSumOfOutWeights();
	}
}