package com.signalcollect.javaapi.examples;

import java.lang.Iterable;
import java.util.HashMap;

import com.signalcollect.interfaces.MessageBus;
import com.signalcollect.interfaces.SignalMessage;
import com.signalcollect.javaapi.*;

/**
 * Represents a cell in a "Conway's Game of Life" (http://en.wikipedia.org/wiki/Conway's_Game_of_Life) simulation
 * The cell calculates a new state according to the states of its surrounding neighbors.
 * 
 * @author Daniel Strebel
 *
 */
public class GameOfLifeCell extends JavaVertex<Integer, Boolean, Boolean> {

	private static final long serialVersionUID = 1L;
	private HashMap<Integer, Boolean> neighbors = new HashMap<Integer, Boolean>(); //holds all states of the cell's neighbors
	private boolean needsToSignal = true; //flag to signal if the cell has changed its state since the last signal step.
										  // if set to true the cell needs to forward an update to its neighbors.
	
	/**
	 * Constructor for a Game of Life cell.
	 * 
	 * @param id a unique integer for identifying the cell
	 * @param state sets the current state i.e. if the cell is alive or not
	 */
	public GameOfLifeCell(Integer id, Boolean state) {
		super(id, state);
	}
	
	/**
	 *  Defines a collect operation which determines how a cell should calculate
	 *  its state depending on the messages of its neighbors which contain their
	 *  current state.
	 *  
	 *  A message with state true means that the sender of this message is alive and
	 *  a message with state false means that the sender is currently not alive.
	 *  
	 *  Each cell will determine if it should resurrect, die or stay in the current
	 *  state according to the number of alive neighbors:
	 *  
	 *  #of alive neighbors		consequence
	 *  =====================================================
	 *  count <= 1				Cell dies of loneliness
	 *  count = 2				Cell stays in current state
	 *  count = 3				Cell resurrects
	 *  count > 3				Cell dies because of overcrowding
	 *  
	 *  @param signals the newest signals that were sent to the cell
	 *  @param messageBus
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void executeCollect(Iterable signals, MessageBus messageBus) {
		
		// Inserts all new signal messages 
		for (Object object : signals) {
			SignalMessage<Integer, Integer, Boolean> message = (SignalMessage<Integer, Integer, Boolean>) object;
			neighbors.put(message.edgeId().sourceId(), message.signal());
		}
		
		//counts all alive neighbors of a cell.
		int sumOfAliveNeighbors = 0;
		for (Boolean isAlive : neighbors.values()) {
			if(isAlive) {
				sumOfAliveNeighbors+=1;
			}
		}
		
		boolean oldState = this.getState(); //To compare state to old state
		
		//checks if cell should change its state according to the rules stated above
		if(sumOfAliveNeighbors<=1 || sumOfAliveNeighbors > 3) {
			this.setState(false);	
		}
		else if(sumOfAliveNeighbors == 3) {
			this.setState(true);
		}
		
		//if the cell has changed its state the cell should signal its new state to its neighbors
		if(oldState!=this.getState()) {
			needsToSignal = true;
		}
		else {
			needsToSignal = false;
		}
	}
	
	
	/**
	 * This function determines if new signals should be sent along the outgoing edges.
	 * The need to signal is controlled by the variable needsToSignal.
	 */
	public double scoreSignal() {
		if(needsToSignal) {
			return 1.0;
		}
		else {
			return 0.0;
		}
	}
}
