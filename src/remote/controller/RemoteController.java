package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import elevator.Elevators;

/******************************************************
 * 
 * Class that is spawned as a thread. The thread 
 * maneuver and elevator based on the current position
 * and orders received for the specific elevator
 *
 ******************************************************/

public class RemoteController extends Thread {
	public static final int TIME_DOORS_ARE_OPEN = 1000;		// Time the Doors are open before the start to close again
	public static final int WAIT_BEFORE_MOVE = 200;			// Time after doors close before the elevator start to move again

	private int id = - 1;		// The id of the elevator the controller corresponds to
	private int motor = 0;		// Current direction of the motor

	private LinkedList<ButtonOrder> upOrders = new LinkedList<ButtonOrder>();		// Used to store all order in upwards direction
	private LinkedList<ButtonOrder> downOrders = new LinkedList<ButtonOrder>();		// Used to store all order in downwards direction

	private double position = 0;	// Store the current position
	private int direction = 0;		// Store the direction of the target elevator order
	private int target = 0;			// Store the floor of the target elevator order
	private int scale = 0;			// Store the current scale value

	private Communicator communicator;			// Used to communicate over the tcp connection
	private ButtonOrderQue buttonOrderQue;		// Reference to the common buttonOrderQue
	private ButtonOrder currentOrder = null;	// The current order the elevator is moving towards

	private ReentrantLock orderLock = new ReentrantLock();	// Lock that creates mutual exclusions to the linkedLists(upOrders and downOrder)
	private Condition newOrder = orderLock.newCondition();	// Condition variable that is signaled when new orders are added to the remoteController

	private ReentrantLock positionLock = new ReentrantLock();	// Lock that prevent race conditions for when updating the current position
	private Condition stop = positionLock.newCondition();		// Condition variable that is signaled when the elevator is stopped

	private ReentrantLock doorLock = new ReentrantLock();				// Lock that ensures mutual exclusion when handling the door
	private Condition doorOpenedOrClosed = doorLock.newCondition();		// Condition variable that is signaled when the door is opened of closed
	private int doorMovements = 0;										// Store the amount of movements the door has made
	private boolean doorsAreOpeningAndClosing = false;					// Indicate if the doors are currently moving

	private boolean stopped = false;			// Indicate if the elevator have been stopped
	private boolean hasPassengers = false;		// Indicate if the elevator can have passengers
	private boolean doNotOpenDoors = false;		// Prevent the doors from opening when elevator is stopped if set to true

	/*
	 * Constructor for a remoteController with a specific id for the
	 * elevator it is controlling, a reference to the communicator and
	 * a reference to the buttonOrderQue
	 */
	public RemoteController(int id, Communicator c, ButtonOrderQue q) {
		this.id = id;
		this.communicator = c;
		this.buttonOrderQue = q;
	}

	/*
	 * When the remote controller thread run it sets the next target 
	 * order, start to move the elevator in the right direction then
	 * wait for arrival
	 */
	@Override
	public void run() {
		while(true) {
			setNextTarget();
			move();
			awaitArrival();
		}
	}

	/*
	 * Function that suspends the remoteController thread until
	 * the elevator is stopped and the motor is set to zero
	 */
	private void awaitArrival() {
		positionLock.lock();
		try {

			while(motor != 0) {
				stop.await();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			positionLock.unlock();
		}

		orderLock.lock();
		try {
			if(stopped == true) {
				if(currentOrder != null) {
					buttonOrderQue.carriedOut(currentOrder);
					currentOrder = null;
				}
				emptyQues(); 	// Return all orders if stopped
				direction = 0;
				return;
			}
			hasPassengers = true;
		} finally {
			orderLock.unlock();
		}

		openDoors();
		buttonOrderQue.carriedOut(currentOrder);
		currentOrder = null; 
	}

	/*
	 * Call from the remoteController to start opening the doors,
	 * suspends the calling thread until the doors are open, after
	 * a delay the doors are set to close again. The calling thread 
	 * is suspended again until they are fully closed
	 */
	private void openDoors() {
		doorLock.lock();
		try {
			if(doNotOpenDoors) return;
			
			doorsAreOpeningAndClosing = true;
			doorMovements = 0;
			communicator.send("d " + id + " 1");

			// Wait until doors are fully open
			while(doorMovements < 4) {
				doorOpenedOrClosed.await();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			doorLock.unlock();
		}

		try {
			Thread.sleep(TIME_DOORS_ARE_OPEN);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		doorLock.lock();
		try {
			doorMovements = 0;
			communicator.send("d " + id + " -1");

			// Wait until doors are fully closed
			while(doorMovements < 4) {
				doorOpenedOrClosed.await();
			}

			doorsAreOpeningAndClosing = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			doorLock.unlock();
		}

		try {
			Thread.sleep(WAIT_BEFORE_MOVE);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	/*
	 * Extract the appropriate order to be next target
	 * and set it to be the current target for the current
	 * remote controller
	 */
	private void setNextTarget() {
		orderLock.lock();
		try {
			if(direction == 1) {
				if(!upOrders.isEmpty()) {
					if(mostImmediateOrder(upOrders)) {
						return;
					} else if (!downOrders.isEmpty()) {
						direction = -1;
						mostDistantOrder(downOrders);
						return;
					}
				}
			} else if(direction == -1) {
				if(!downOrders.isEmpty()){
					if(mostImmediateOrder(downOrders)) {
						return;
					} else if (!upOrders.isEmpty()) {
						direction = 1;
						mostDistantOrder(upOrders);
						return;
					}
				}
			}
			direction = 0;

			while(upOrders.isEmpty() && downOrders.isEmpty()) {
				hasPassengers = false;
				newOrder.await();
			}

			if(!downOrders.isEmpty()) {
				direction = -1;
				mostDistantOrder(downOrders);
				return;
			} else {
				direction = 1;
				mostDistantOrder(upOrders);
				return;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			orderLock.unlock();
		}
	}

	/*
	 * Get the most extreme order. If the direction of the orders
	 * are upwards the order from the lowest floor is set to be
	 * current target.  If the direction of the orders are downwards
	 * the order from the highest floor is set to be current target.
	 * Is always called with from a thread that holds the orderLock 
	 * in the current remoteController.
	 */
	private void mostDistantOrder(LinkedList<ButtonOrder> orders) {
		while(orders.isEmpty())
		{
			try {
				newOrder.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		ButtonOrder distant = null;
		int dir = orders.getFirst().direction;
		int val = (direction == 1) ? MainController.amountOfFloors + 1: -1;

		for(ButtonOrder o : orders) {
			if(dir == 1) {
				if(o.floor < val) {
					val = o.floor;
					distant = o;
				}
			} else {
				if(o.floor > val) {
					val = o.floor;
					distant = o;
				}
			}
		}
		if(distant != null) {
			currentOrder = distant;
			orders.remove(distant);
			target = currentOrder.floor;
		} else {
			mostDistantOrder(orders);
		}
	}

	/*
	 * Set the elevator in motion towards the current target
	 * of the remoteController
	 */
	private void move() {
		positionLock.lock();
		try {
			if(Math.abs(position - (double) target) < 0.05) {
				stop.signal();
				motor = 0;
				communicator.send("m " + id + " 0");
				return;
			}

			if(target > position) {
				communicator.send("m " + id + " 1");
				motor = 1;
			} else {
				communicator.send("m " + id + " -1");
				motor = -1;
			}
		} finally {
			positionLock.unlock();
		}
		
		doorLock.lock();
		try {
			doNotOpenDoors = false;
		} finally {
			doorLock.unlock();
		}
	}

	/*
	 * Return all orders of the current remoteController,
	 * is called when the elevator is stopped and the requests
	 * need to be pickup by another elevator.
	 */
	private void emptyQues() {
		for(ButtonOrder o : downOrders) {
			buttonOrderQue.carriedOut(o);
		}
		for(ButtonOrder o : upOrders) {
			buttonOrderQue.carriedOut(o);
		}
		buttonOrderQue.returnList(downOrders);
		buttonOrderQue.returnList(upOrders);		

		downOrders = new LinkedList<ButtonOrder>();
		upOrders = new LinkedList<ButtonOrder>();
	}

	/*
	 * Set the current target to the most immediate order in the
	 * elevators current direction. If the direction is downwards 
	 * the closest order that is on a lower floor than the 
	 * elevator is picked as the next target.
	 */
	private boolean mostImmediateOrder(LinkedList<ButtonOrder> orders) {
		while(orders.isEmpty())
		{
			try {
				newOrder.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		int pos = -1;
		int currentPos = 0;
		int dir = orders.getFirst().direction;

		int val = (dir == 1) ? MainController.amountOfFloors : -1;
		int elevatorPos = Math.round((float) position);

		for(ButtonOrder o : orders) {
			if(dir == 1) {
				if(o.floor > elevatorPos && o.floor < val) {
					pos = currentPos;
					val = o.floor;
				}
			} else {
				if(o.floor < elevatorPos && o.floor > val) {
					pos = currentPos;
					val = o.floor;
				}
			}
			currentPos++;
		}
		if(pos == -1) 
			return false;

		currentOrder = orders.remove(pos);
		target = currentOrder.floor;
		return true;
	}

	/*****************************************************
	   Functions used by by other threads in the monitor
	 *****************************************************/

	public void retrieveOrders(RemoteController controller) {		
		orderLock.lock();
		try {
			LinkedList<ButtonOrder> list = (direction == 1) ? upOrders : downOrders;
			controller.giveOrders(list, direction);

			LinkedList<ButtonOrder> returnList = (direction == -1) ? upOrders : downOrders;
			while(!returnList.isEmpty()){
				buttonOrderQue.returnOrder(returnList.remove());
			}
		} finally {
			orderLock.unlock();
		}
	}

	public void giveOrders(LinkedList<ButtonOrder> list, int dir) {
		orderLock.lock();
		try {
			LinkedList<ButtonOrder> emptyList = (dir == 1) ? upOrders : downOrders;

			if(currentOrder != null) {
				if(currentOrder.direction == dir) {
					list.add(currentOrder);
					currentOrder = null;
				}
			}

			while(!emptyList.isEmpty()) {
				list.add(emptyList.remove());
			}
			direction = 0;
		} finally {
			orderLock.unlock();
		}

		doorLock.lock();
		try {
			doNotOpenDoors = true;
		} finally {
			doorLock.unlock();
		}
		
		positionLock.lock();
		try {
			communicator.send("m " + id + " 0");
			motor = 0;
			stop.signal();
		} finally {
			positionLock.unlock();
		}
	}

	public boolean putRequest(ButtonOrder c) {
		orderLock.lock();
		try {
			if(stopped) return false;

			if(c.direction == -1)  {
				if(currentOrder != null) {
					if((c.floor > currentOrder.floor && (double) c.floor < position) ||
							(!hasPassengers && c.floor > target)) {

						if(!hasPassengers && this.direction != c.direction) 
							this.direction = c.direction;

						if(currentOrder.direction == 1) {
							upOrders.add(currentOrder);
						} else {
							downOrders.add(currentOrder);
						}

						currentOrder = c;
						target = c.floor;
						return true;
					}
				} 
				downOrders.add(c);

			} else {
				if(currentOrder != null) {
					if((c.floor < currentOrder.floor && (double) c.floor > position) ||
							(!hasPassengers && c.floor < target)) {

						if(!hasPassengers && this.direction != c.direction) 
							this.direction = c.direction;

						if(currentOrder.direction == 1) {
							upOrders.add(currentOrder);
						} else {
							downOrders.add(currentOrder);
						}

						currentOrder = c;
						target = c.floor;
						return true;
					} 
				}
				upOrders.add(c);
			}
			newOrder.signal();
			return true;

		} finally {
			orderLock.unlock();
		}
	}

	private void doorMoved() {

		doorMovements++;
		if(doorMovements == 4)
			doorOpenedOrClosed.signal();
		else if(doorMovements == 8)
			doorOpenedOrClosed.signal();
	}

	public void setPosition(double pos) {
		doorLock.lock();
		try {
			if(doorsAreOpeningAndClosing) {
				doorMoved();
				return;
			}
		} finally {
			doorLock.unlock();
		}

		positionLock.lock();
		try {
			position = pos;

			if((motor == 1 && position >= (double) target - Elevators.step) ||
					(motor == -1 && position <= (double) target + Elevators.step))  {

				communicator.send("m " + id + " 0");
				motor = 0;
				stop.signal();
			}

			int floor = Math.round((float) position);
			if (scale != floor) {
				scale = floor;
				communicator.send("s " + id + " " + floor);
			}
		} finally {
			positionLock.unlock();
		}
	}

	private boolean orderExist(ButtonOrder order) {
		if(currentOrder != null) {
			if(currentOrder.floor == order.floor &&
					currentOrder.direction == order.direction) return true;
		}

		LinkedList<ButtonOrder> que = (order.direction == -1) ? downOrders : upOrders;

		for(ButtonOrder b : que) {
			if(b.floor == order.floor && b.direction == order.direction)
				return true;
		}

		return false;
	}

	public void addOrder(int floor) {
		if(floor == Elevators.SPECIAL_FOR_STOP) {
			orderLock.lock();
			try {
				stopped = true;
			} finally {
				orderLock.unlock();
			}



			positionLock.lock();
			try {
				communicator.send("m " + id + " 0");
				motor = 0;
				stop.signal();
			} finally {
				positionLock.unlock();
			}
			return;
		}

		orderLock.lock();
		try {
			stopped = false;
			hasPassengers = true;
			if(floor < (double) position) {
				ButtonOrder order = new ButtonOrder(floor, -1);
				if(!orderExist(order)) {

					if(floor > target && direction == -1 && !doorsAreOpeningAndClosing) {
						target = floor;
						if(currentOrder != null)
							downOrders.add(currentOrder);

						currentOrder = order;
					} else {
						downOrders.add(order);
					}

					buttonOrderQue.acceptOrder(order);
					newOrder.signal();
				}
			} else {
				ButtonOrder order = new ButtonOrder(floor, 1);
				if(!orderExist(order)) {

					if(floor < target && direction == 1 && !doorsAreOpeningAndClosing) {
						target = floor;
						if(currentOrder != null)
							upOrders.add(currentOrder);

						currentOrder = order;
					} else {
						upOrders.add(order);
					}
					buttonOrderQue.acceptOrder(order);
					newOrder.signal();
				}
			}
		} finally {
			orderLock.unlock();
		}
	}

	public int getDirection() {
		return direction;
	}

	public int getTarget() {
		positionLock.lock();
		try {
			return target;
		} finally {
			positionLock.unlock();
		}
	}

	public double getPosition() {
		positionLock.lock();
		try {
			return position;

		} finally {
			positionLock.unlock();
		}
	}

	public int getFloor() {
		positionLock.lock();
		try {
			return Math.round((float) position);
		} finally {
			positionLock.unlock();
		}
	}

	public int getNumberOfOrders() {
		orderLock.lock();
		try {
			return upOrders.size() + downOrders.size();
		} finally {
			orderLock.unlock();
		}
	}

	public boolean isStopped() {
		return stopped;
	}

	public int getID() {
		return id;
	}

	public boolean gotPassengers() {
		return hasPassengers;
	}

	public int getMotor() {
		return motor;
	}
}

