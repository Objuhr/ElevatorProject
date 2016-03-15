package remote.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import elevator.ElevatorIO;
import elevator.Elevators;

public class MainController {
	public static RemoteController[] controllers;
	public static int amountOfElevators = Elevators.DefaultNumberOfElevators;
	private static Socket socket = null;
	private static Communicator c = null;
	
	private static String ip = "localhost";
	private static int port = Elevators.defaultPort;
	
	public static void main(String[] args) {
		parseInput(args);
		initConnection();
		initControllers();
		
		controllers = new RemoteController[amountOfElevators];
	}
	
	private static void initControllers() {
		int i = 1;
		for(RemoteController r : controllers) {
			r = new RemoteController(i, c);
			r.start();
			i++;
		}
	}
	
	private static void parseInput(String[] args) {
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
	
	private static void initConnection() {
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
		
		ActionQue a = new ActionQue();
		
		Reciever r = new Reciever(c, a);
		Parser p = new Parser(c, a);
		
		r.start();
		p.start();
	}
}
