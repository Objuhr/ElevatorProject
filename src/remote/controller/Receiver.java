package remote.controller;

/********************************************
 * 
 * Class that is started as a separate thread.
 * The thread checks the tcp connection and 
 * save the input to a actionQue.
 * 
 ********************************************/

public class Receiver extends Thread {

	private Communicator c;		// Used to access the tcp connection
	private ActionQue a;		// Used to store the input from the tcp connection
	
	/*
	 * Instantiate the receiver
	 */
	public Receiver(Communicator c, ActionQue a) {
		this.c = c;
		this.a = a;
	}
	
	/*
	 * The thread receives input from the tcp connection
	 * and stores it in an actionQue
	 */
	@Override
	public void run() {		
		while(true) {
			String action = c.recieve();
			a.putAction(action);
		}
	}
}
