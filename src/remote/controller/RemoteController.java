package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import elevator.Elevators;

public class RemoteController extends Thread {
	public static final int TIME_DOORS_ARE_OPEN = 1000;
	public static final int WAIT_BEFORE_MOVE = 200;

	private int id = - 1;
	private int motor = 0;

	private LinkedList<ButtonOrder> upOrders = new LinkedList<ButtonOrder>();
	private LinkedList<ButtonOrder> downOrders = new LinkedList<ButtonOrder>();

	private double position = 0;
	private int direction = 0;
	private int target = 0;
	private int scale = 0;

	private Communicator c;
	private ButtonOrderQue buttonOrderQue;
	private ButtonOrder currentOrder = null;

	private ReentrantLock orderLock = new ReentrantLock();
	private Condition newOrder = orderLock.newCondition();

	private ReentrantLock positionLock = new ReentrantLock();
	private Condition stop = positionLock.newCondition();

	private ReentrantLock doorLock = new ReentrantLock();
	private Condition doorOpenedOrClosed = doorLock.newCondition();
	private int doorMovements = 0;
	private boolean doorsAreOpening = false;

	private boolean stopped = false;
	private boolean hasPassengers = false;
	private boolean doNotOpenDoors = false;

	public RemoteController(int id, Communicator c, ButtonOrderQue q) {
		this.id = id;
		this.c = c;
		this.buttonOrderQue = q;
	}

	@Override
	public void run() {
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
		while(true) {
			nextOrder();
			move();
			awaitArrival();
		}
	}

	/**************************************************
	   Functions used by the remoteControlle thread
	 **************************************************/

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
				emptyQues();
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

	private void openDoors() {
		doorLock.lock();
		try {
			if(doNotOpenDoors) return;
			
			doorsAreOpening = true;
			doorMovements = 0;
			c.send("d " + id + " 1");

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
			c.send("d " + id + " -1");
			doorMovements = 0;

			while(doorMovements < 4) {
				doorOpenedOrClosed.await();
			}

			doorsAreOpening = false;
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

	private void nextOrder() {
		orderLock.lock();
		try {
			if(direction == 1) {
				if(!upOrders.isEmpty()) {
					if(mostImidietOrder(upOrders)) {
						return;
					} else if (!downOrders.isEmpty()) {
						direction = -1;
						mostDistantOrder(downOrders);
						return;
					}
				}
			}
			if(direction == -1) {
				if(!downOrders.isEmpty()){
					if(mostImidietOrder(downOrders)) {
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
			mostImidietOrder(orders);
		}
	}

	private void move() {
		positionLock.lock();
		try {
			if(Math.abs(position - (double) target) < 0.05) {
				stop.signal();
				motor = 0;
				c.send("m " + id + " 0");
				return;
			}

			if(target > position) {
				c.send("m " + id + " 1");
				motor = 1;
			} else {
				c.send("m " + id + " -1");
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

	private boolean mostImidietOrder(LinkedList<ButtonOrder> orders) {
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
			c.send("m " + id + " 0");
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
			if(doorsAreOpening) {
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

				c.send("m " + id + " 0");
				motor = 0;
				stop.signal();
			}

			int floor = Math.round((float) position);
			if (scale != floor) {
				scale = floor;
				c.send("s " + id + " " + floor);
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
				c.send("m " + id + " 0");
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

					if(floor > target && direction == -1 && !doorsAreOpening) {
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

					if(floor < target && direction == 1 && !doorsAreOpening) {
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

