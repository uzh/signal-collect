package signalcollect.algorithms.java;

import signalcollect.javaapi.DefaultEdgeJ;

/**
 * Represents a citation in the publication-citation PageRank graph
 * 
 * @param citerId
 *            : the identifier of the citing publication
 * @param citedId
 *            : the identifier of the cited publication
 */
@SuppressWarnings("serial")
public class CitationEdgeJ extends DefaultEdgeJ<Object, Object> {

	public CitationEdgeJ(Object citerId, Object citedId) {
		super(citerId, citedId);
	}

	/**
	 * Signals the weighted rank of the citing publication to the cited
	 * publication
	 */
	public Object signal() {
		PublicationVertexJ sourceVertex = (PublicationVertexJ) source();
		return sourceVertex.state() * weight() / sourceVertex.sumOfOutWeights();
	}
}
