package signalcollect.algorithms.java;

import signalcollect.javaapi.DefaultVertexJ;

/**
 * Represents a publication in the publication-citation PageRank graph
 * 
 * @param id
 *            : the unique identifier of the publication
 * @param dampingFactor
 *            : damping factor that limits influence spreading in the PageRank
 *            algorithm, usually set to 0.85
 */
@SuppressWarnings("serial")
public class PublicationVertexJ extends DefaultVertexJ<Object, Double> {

	Double dampingFactor;

	public PublicationVertexJ(Object id, Double dampingFactor) {
		super(id, 1 - dampingFactor);
		this.dampingFactor = dampingFactor;
	}

	/**
	 * Calculates the new PageRank of the Publication vertex by adding the
	 * dampened sum of all received rank-signals to the base PageRank
	 */
	public Double collect() {
		Double rankSum = 0.0;
		for (Double rankSignal : signalsJ(Double.class)) {
			rankSum += rankSignal;
		}
		return 1 - dampingFactor + dampingFactor * rankSum;
	}

}
