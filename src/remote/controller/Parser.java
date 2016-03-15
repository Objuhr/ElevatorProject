package remote.controller;

import java.io.OutputStream;
import java.util.StringTokenizer;

public class Parser extends Thread {
	private String input;
	private Communicator communicator;
	private String action[] = new String[3];
	private FifoQue callQue;

	public Parser (String i, Communicator c, FifoQue q){
		input = i;
		communicator = c;
		callQue = q;
	}

	private void parseInput() {
		StringTokenizer tok = new StringTokenizer(input);
		int i = 0;
		while(tok.hasMoreTokens()) {
			if(i > 3) {
				System.err.println("Wrong amount of arguments"); 
				this.interrupt();
			}
			action[i] = tok.nextToken();
			i++;
		}

	}

	@Override
	public void run() {
		parseInput();

		try {
			if(action[0].equals("b")) {
				buttonPressed(Integer.parseInt(action[1]), Integer.parseInt(action[2]));
			} else {

			}
		} catch(Exception e) { e.printStackTrace(); }
	}

	private void buttonPressed(int floor, int direction) {
		ButtonCalls e = new ButtonCalls(floor, direction);
		callQue.put(e);
	}
}
