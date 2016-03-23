package remote.controller;

import java.io.OutputStream;
import java.util.StringTokenizer;

/********************************************
 * 
 * Used to parse input and respond to the
 * input.
 *
 ********************************************/

public class Parser extends Thread {
	private ButtonOrderQue buttonOrderQue;
	private ActionQue actionQue;

	/*
	 * Constructor that creates a new instance with
	 * an given actionQue and buttonOrderQue
	 */
	public Parser (ActionQue a, ButtonOrderQue q){
		actionQue = a;
		buttonOrderQue = q;
	}
	
	/*
	 * Function called for a newly spawned thread,
	 * takes an input string from the actionQue,
	 * parse the string and make an appropriate
	 * respons to the input
	 */
	@Override
	public void run() {
		while(true) {
			String input = actionQue.getAction();
			String[] action = parseInput(input);

			if(action != null) respond(action);
		}
	}

	/*
	 * Function that parse an input string and
	 * return the split up string as an array
	 */
	private String[] parseInput(String input) {
		StringTokenizer tok = new StringTokenizer(input);
		int i = 0;
		String action[] = new String[3];
		
		while(tok.hasMoreTokens()) {
			if(i > 3) {
				break;
			}
			action[i] = tok.nextToken();
			i++;
		}
		return action;

	}

	/*
	 * Make an respons given the split up input string
	 * that is sent as the input argument(action)
	 */
	private void respond(String[] action) {
		try {
			// An request for an elevator has been made if the first character is b
			if(action[0].equals("b")) {
				elevatorRequested(Integer.parseInt(action[1]), Integer.parseInt(action[2]));
			
			// Update position of a given elevator if the first character is f
			} else if(action[0].equals("f")) {
				int elevatorID = Integer.parseInt(action[1]) - 1;
				MainController.controllers[elevatorID].setPosition(Double.parseDouble(action[2]));
			
			// New order from inside an elevator is created
			} else if(action[0].equals("p")) {
				elevatorButtonPressed(Integer.parseInt(action[1]), Integer.parseInt(action[2]));
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	/*
	 * Function that create an button order corresponding to the
	 * input request that have been made outside an elevator
	 */
	private void elevatorRequested(int floor, int direction) {
		ButtonOrder e = new ButtonOrder(floor, direction);
		buttonOrderQue.put(e);
	}
	
	/*
	 * An button within and elevator has been pressed so the
	 * order is added to the remote controller
	 */
	private void elevatorButtonPressed(int elevatorID, int targetFloor) {
		MainController.controllers[elevatorID - 1].addOrder(targetFloor);
	}
}
