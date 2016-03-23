package remote.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.locks.ReentrantLock;

/**************************************************
 * 
 * Instance used to communicate over the tcp 
 * connection.
 * 
 **************************************************/

public class Communicator {
	private BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); 	// Used to read InputStream
	private PrintStream p = System.out;													// Used to write to OutputStream
	
	/*
	 * Instantiate the communicator with a given output 
	 * and input stream
	 */
	public Communicator(OutputStream os, InputStream is) {
		in = new BufferedReader(new InputStreamReader(is));
		p = new PrintStream(os);
	}

	/*
	 * Used for other instances to send to send to the Gui
	 * over the OutputStream
	 */
	public void send(String send) {
			p.println(send);
	}

	/*
	 * Blocking receive call, only return when there have
	 * been anything sent throught the InputStream
	 */
	public String recieve() {
		String input = null;

		while(input == null) {
			try {
				input = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return input;
	}
}
