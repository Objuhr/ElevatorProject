package remote.controller;

/********************************************
 * 
 * Instance that represent both requests for 
 * elevators made from outside the elevators
 * and orders made from inside the elevator
 * 
 ********************************************/
public class ButtonOrder {
	public int floor;		// Indicate which floor the order came from
	public int direction;	// Indicate ordered direction
	
	/*
	 * Constructor that sets the buttonOrders 
	 * floor and direction to given values
	 */
	public ButtonOrder(int f, int d) {
		floor = f;
		direction = d;
	}
}
