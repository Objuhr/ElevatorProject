package remote.controller;

import java.io.OutputStream;
import java.util.StringTokenizer;

public class Parser extends Thread {
	private Communicator communicator;
	private ButtonOrderQue callQue;
	private ActionQue actionQue;

	public Parser (Communicator c, ActionQue a, ButtonOrderQue q){
		communicator = c;
		actionQue = a;
		callQue = q;
	}

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

	@Override
	public void run() {
		while(true) {
			String input = actionQue.getAction();
			String[] action = parseInput(input);

			if(action != null) takeAction(action);
		}
	}

	private void takeAction(String[] action) {
		try {
			if(action[0].equals("b")) {
				buttonPressed(Integer.parseInt(action[1]), Integer.parseInt(action[2]));
			
			} else if(action[0].equals("f")) {
				int elevatorID = Integer.parseInt(action[1]) - 1;
				MainController.controllers[elevatorID].setPosition(Double.parseDouble(action[2]));
			
			} else if(action[0].equals("p")) {
				elevatorOrder(Integer.parseInt(action[1]), Integer.parseInt(action[2]));
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	private void buttonPressed(int floor, int direction) {
		ButtonOrder e = new ButtonOrder(floor, direction);
		System.err.println();
		System.err.println("Parser tries to put bo: floor=" + floor + " dir=" +direction);
		callQue.put(e);
	}
	
	private void elevatorOrder(int elevatorID, int targetFloor) {
		MainController.controllers[elevatorID - 1].addOrder(targetFloor);
	}
}
