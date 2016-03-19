package remote.controller;


public class Reciever extends Thread {

	private Communicator c;
	private ActionQue a;
	
	public Reciever(Communicator c, ActionQue a) {
		this.c = c;
		this.a = a;
	}
	
	@Override
	public void run() {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		
		while(true) {
			String action = c.recieve();
			a.putAction(action);
		}
	}
}
