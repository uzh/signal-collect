package signalcollect.algorithms.java;

import signalcollect.interfaces.ComputationStatistics;
import signalcollect.api.DefaultBuilder;
import signalcollect.interfaces.ComputeGraph;
import signalcollect.interfaces.Vertex;
import signalcollect.javaapi.VertexFunctionJ;

public class PageRankJ {

	public static void main(String[] args) {
		ComputeGraph cg = DefaultBuilder.build();
		cg.addVertex(new PublicationVertexJ(1, 0.85));
		cg.addVertex(new PublicationVertexJ(2, 0.85));
		cg.addVertex(new PublicationVertexJ(3, 0.85));
		cg.addEdge(new CitationEdgeJ(1, 2));
		cg.addEdge(new CitationEdgeJ(2, 1));
		cg.addEdge(new CitationEdgeJ(2, 3));
		cg.addEdge(new CitationEdgeJ(3, 2));
		ComputationStatistics stats = cg.execute(); // executes the PageRank
													// computation on the
													// citation graph
		System.out.println(stats);
		cg.foreach(new VertexFunctionJ() {
			public void doApply(Vertex<?, ?> v) {
				System.out.println(v);
			}
		});
		cg.shutDown();
	}
}