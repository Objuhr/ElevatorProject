package remote.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import elevator.Elevators;

/**************************************************
 * 
 * Main class that parse input strings, initiate 
 * connection and spawn workDistributer, parser, 
 * input receiver and remoteController threads.
 * 
 **************************************************/

public class MainController {
	public final static int DefaultNumberOfElevators = 3;
	public final static int DefaultTopFloor = 6;
	public static final int defaultPort = 4711;
	
	public static RemoteController[] controllers;							// Used to access remotecontrollers
	public static int amountOfElevators = DefaultNumberOfElevators;			// Used to set amount of elevators
	public static int amountOfFloors = DefaultTopFloor + 1;					// Used to set amount of floors
	private static Socket socketIn = null;									// Used to store socket for the tcp connection
	private static Socket socketOut = null;									// Used to store socket for the tcp connection
	private static Communicator communicator = null;						// Used to communicate over the tcp connection
	
	private static ButtonOrderQue buttonOrderQue = new ButtonOrderQue();	// Used to store all orders for the elevators
	private static ActionQue actionQue = new ActionQue();					// Used to store all input reciver recives
	
	private static String hostIp = "localhost";								// Store the host ip to the Gui
	private static int portIn = defaultPort;								// Store the receive port
	private static int portOut = defaultPort;								// Store the send port
	
	/*
	 * Main function that start the remote 
	 * controlling of the elevators
	 */
	public static void main(String[] args) {
		parseInput(args);
		initConnection();
		initControllers();
		initInputReadersAndDistributer();
	}
	
	/*
	 * Parse the input and set settings accordingly
	 */
	private static void parseInput(String[] args) {
		if(args.length > 0) {
			amountOfElevators = Integer.parseInt(args[0]);
		}
		if(args.length > 1) {
			hostIp = args[1];
		}
		if(args.length > 2) {
			portIn = Integer.parseInt(args[2]);
		}
	}
	
	/*
	 * Initiate the tcp connection to the Gui
	 * and instantiate the object used for 
	 * communication(communicator)
	 */
	private static void initConnection() {
		try {
			socketIn = new Socket(hostIp, portIn);
			
			if(portOut == portIn) {
				socketOut = socketIn;
			} else {
				socketOut= new Socket(hostIp, portOut);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		InputStream is = null;
		OutputStream os = null;
		try {
			is = socketIn.getInputStream();
			os = socketOut.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		communicator = new Communicator(os, is);
	}
	
	/*
	 * Initiate the remote controller monitors
	 * and start the separate threads
	 */
	private static void initControllers() {
		controllers = new RemoteController[amountOfElevators];
		

		for(int i = 0; i < amountOfElevators; i++) {
			controllers[i] = new RemoteController(i + 1, communicator, buttonOrderQue);
			controllers[i].start();
		}
	}
	
	/*
	 * Initiate the receiver, parser and work 
	 * distributer threads
	 */
	private static void initInputReadersAndDistributer() {		
		new WorkDistributer(buttonOrderQue).start();
		new Receiver(communicator, actionQue).start();
		new Parser(actionQue, buttonOrderQue).run();
	}
}
