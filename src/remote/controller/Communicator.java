package remote.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.locks.ReentrantLock;

public class Communicator {
	private BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	private PrintStream p = System.out;
	private ReentrantLock sendLock = new ReentrantLock();
	
	
	public Communicator(OutputStream os, InputStream is) {
		in = new BufferedReader(new InputStreamReader(is));
		p = new PrintStream(os);
	}

	public void send(String send) {
		sendLock.lock();
		try {
			p.println(send);
		} finally {
			sendLock.unlock();
		}
	}

	public String recieve() {
		String input = null;

		while(input == null) {
			try {
				input = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new String(input);
	}
}
