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
	private Communicator communicator;
	private FifoQue callQue = new FifoQue();
	
	public Reciever() {
		try {
			socket = new Socket("localhost", defaultPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
		InputStream is = null;
		OutputStream os = null;
		try {
			is = socket.getInputStream();
			os = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		communicator = new Communicator(os, is);
	}
	
	@Override
	public void run() {
		while(true) {
			String action = communicator.recieve();
			Parser e = new Parser(action, communicator, callQue);
			e.start();
		}
	}
}
