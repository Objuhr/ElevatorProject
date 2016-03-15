package remote.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import elevator.Elevators;

public class MainController {
	public static RemoteController[] controllers;
	public static int amountOfElevators = Elevators.DefaultNumberOfElevators;
	public static int amountOfFloors = Elevators.DefaultTopFloor + 1;
	private static Socket socket = null;
	private static Communicator c = null;
	
	private static ButtonOrderQue q = new ButtonOrderQue();
	private static ActionQue a = new ActionQue();
	
	private static String ip = "localhost";
	private static int port = Elevators.defaultPort;
	
	public static void main(String[] args) {
		parseInput(args);
		initConnection();
		initControllers();
		initInputReaders();
	}
	
	public static void parseInput(String[] args) {
		if(args.length > 0) {
			amountOfElevators = Integer.parseInt(args[0]);
		}
		if(args.length > 1) {
			ip = args[1];
		}
		if(args.length > 2) {
			port = Integer.parseInt(args[2]);
		}
	}
	
	public static void initConnection() {
		try {
			socket = new Socket(ip, port);
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
		c = new Communicator(os, is);
	}
	
	public static void initControllers() {
		controllers = new RemoteController[amountOfElevators];
		

		for(int i = 0; i < amountOfElevators; i++) {
			controllers[i] = new RemoteController(i + 1, c, q);
			controllers[i].start();
		}
	}
	
	public static void initInputReaders() {		
		new WorkDistributer(q).start();
		new Reciever(c, a).start();
		new Parser(c, a, q).run();
	}
}
