package com.signalcollect.javaapi.examples;

import com.signalcollect.javaapi.*;

@SuppressWarnings("serial")
public class PageJ extends DataGraphVertexJ<Integer, Double, Double> {

	Double baseRank;
	Double dampingFactor;

	public PageJ(Integer vertexId, Double baseRank) {
		super(vertexId, baseRank);
		this.baseRank = baseRank;
		this.dampingFactor = 1 - baseRank;
	}

	public Double collect(Iterable<Double> mostRecentSignals) {
		Double rankSum = 0.0;
		for (Double signal : mostRecentSignals) {
			rankSum += signal;
		}
		return baseRank + dampingFactor * rankSum;
	}
}