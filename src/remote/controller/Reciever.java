package remote.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Reciever extends Thread {

	/**
	 * Default port for input/output via a TCP socket = 4711
	 */
	public static final int defaultPort = 4711;

	private Socket socket;
	private Communicator c;
	private ActionQue a;
	private ButtonOrderQue callQue = new ButtonOrderQue();
	
	public Reciever(Communicator c, ActionQue a) {
		this.c = c;
		this.a = a;
	}
	
	@Override
	public void run() {
		while(true) {
			String action = c.recieve();
			a.putAction(action);
		}
	}
}
