/*
 *  @author Daniel Strebel
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

package com.signalcollect.javaapi.examples;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import com.signalcollect.Vertex;
import com.signalcollect.Edge;
import com.signalcollect.ExecutionInformation;
import com.signalcollect.javaapi.*;
import com.signalcollect.Graph;
import com.signalcollect.StateForwarderEdge;

/**
 * A simulation of a "Conway's Game of Life"
 * (http://en.wikipedia.org/wiki/Conway's_Game_of_Life) based on the signal
 * collect computation model.
 * 
 * To ensure termination and enable the output of intermediate steps a
 * synchronous execution model is chosen here.
 * 
 * @author Daniel Strebel
 * 
 */
public class GameOfLife {

	private final int COLUMNS = 20; // Number of columns in the simulation grid
	private final int ROWS = 20; // Number of rows in the simulation grid
	private static final int NUMBER_OF_ITERATIONS = 50; // Maximal number of
													// synchronization steps.
	private PixelMap image = new PixelMap();
	private Graph g;

	/**
	 * Sets up the graph and generate all vertices and edges.
	 * Each vertex links to its direct neighbors.
	 * 
	 * All neighbors of cell 'O' are marked with an 'X'
	 * 
	 * --------------------
	 * |  |   |   |   |   |
	 * --------------------
	 * |  | X | X | X |   |
	 * --------------------
	 * |  | X | O | X |   |
	 * --------------------
	 * |  | X | X | X |   |
	 * --------------------
	 * |  |   |   |   |   |
	 * --------------------
	 */
	public void init() {
		g = GraphBuilder.build();
		// initialize vertices
		Random rand = new Random();
		for (int i = 0; i < COLUMNS * ROWS; i++) {
			g.addVertex(new GameOfLifeCell(i, rand.nextBoolean()));
		}

		// set up edges
		for (int row = 0; row < ROWS; row++) {
			for (int collumn = 0; collumn < COLUMNS; collumn++) {
				for (Edge edge : generateOutgoingLinks(row, collumn)) {
					g.addEdge(edge);
				}
			}

		}
	}

	@SuppressWarnings("unchecked")
	public void executionStep(boolean showImage) {

		ExecutionInformation stats = g
				.execute(new SynchronousExecutionConfiguration(1));

		if (showImage) {
			byte[] data = new byte[ROWS * COLUMNS];
			Map<Integer, Boolean> idValueMap = g
					.aggregate(new IdStateJavaAggregator<Integer, Boolean>());
			for (int id : idValueMap.keySet()) {
				if (idValueMap.get(id)) {
					data[id] = (byte) 1;
				} else {
					data[id] = (byte) 0;
				}
			}
			image.setData(data);
		} else {
			g.foreachVertex(new VertexCommand() {
				public void f(Vertex v) {
					System.out.println(v);
				}
			});
			
		}
		System.out.println(stats);
	}
	
	public void shutDown() {
		g.shutdown();
	}
	
	/**
	 * Creates a list of all edges that go from a cell to its neighbors.
	 * Edges are only included if the coordinates are within the grid.
	 * 
	 * @param row horizontal coordinate of the source cell
	 * @param column vertical coordinate of the source cell
	 * @return list of all outgoing edges form the cell at the specified coordinates.
	 */
	private ArrayList<Edge> generateOutgoingLinks(int row, int column) {
		int sourceId = generateVertexId(row, column);
		ArrayList<Edge> outgoingLinks = new ArrayList<Edge>();
		
		//iterates over all neighbors and the cell itself but the link to itself is excluded.
		for(int colIt = (column-1); colIt<=(column+1); colIt++) {
			for(int rowIt = (row-1); rowIt<=(row+1); rowIt++) {
				if((rowIt!=row||colIt!=column) && isValidCoordinate(rowIt,colIt)) {
					outgoingLinks.add(new StateForwarderEdge<Integer, Integer>(sourceId,
							generateVertexId(rowIt, colIt)));
				}
			}
		}
		return outgoingLinks;
	}

	/**
	 * utility method to convert grid coordinates to ids. Id starts with 0 in the top left corner and increments
	 * row wise.
	 * 
	 * if x1<x2 then generateVertexId(x1, y) < generateVertexId(x2, y)
	 * if y1<y2 then generateVertexId(x, y1) < generateVertexId(x, y2)
	 * 
	 * @param row
	 * @param collumn
	 * @return the unique id for the cell at the specified location
	 */
	private int generateVertexId(int row, int collumn) {
		return row * COLUMNS + collumn;
	}

	/**
	 * Utility that checks if a coordinate is within the grid. All negative values and all values bigger or equal
	 * to the size constraints of the grid are considered invalid.
	 * 
	 * @param row
	 * @param collumn
	 * @return
	 */
	private boolean isValidCoordinate(int row, int collumn) {
		if (row < 0 || row >= ROWS) {
			return false;
		} else if (collumn < 0 || collumn >= COLUMNS) {
			return false;
		}
		return true;
	}

	/**
	 * Runs the Game of Live simulation with simple visualization.
	 * @param args
	 */
	public static void main(String[] args) {
		GameOfLife simulation = new GameOfLife();
		simulation.init();
		for (int i=0; i<NUMBER_OF_ITERATIONS; i++) {
			simulation.executionStep(true);
		}
		simulation.shutDown();
	}
}
