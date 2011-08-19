package com.signalcollect.javaapi.examples;

import com.signalcollect.*;
import com.signalcollect.javaapi.*;

public class PageRankJ {

	public static void main(String[] args) {
		PageRankJ pr = new PageRankJ();
		pr.executePageRank();
	}

	public void executePageRank() {
		Graph cg = GraphBuilderJ.build();
		cg.addVertex(new PageRankVertexJ(1, 0.15));
		cg.addVertex(new PageRankVertexJ(2, 0.15));
		cg.addVertex(new PageRankVertexJ(3, 0.15));
		cg.addEdge(new PageRankEdgeJ(1, 2));
		cg.addEdge(new PageRankEdgeJ(2, 1));
		cg.addEdge(new PageRankEdgeJ(2, 3));
		cg.addEdge(new PageRankEdgeJ(3, 2));
		ExecutionInformation stats = cg.execute();
		System.out.println(stats);
//		cg.foreachVertex(new CommandJ() {
//			public void f(Vertex v) {
//				System.out.println(v);
//			}
//		});
		cg.forVertexWithId(1, new CommandJ() {
			public void f(Vertex v) {
				System.out.println(v);
			}
		});
//		cg.countVertices(m)
		cg.shutdown();
	}
}
