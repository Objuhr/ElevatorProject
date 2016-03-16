package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RemoteController extends Thread {
	private int id = - 1;
	private int motor = 0;

	private LinkedList<ButtonOrder> upOrders = new LinkedList<ButtonOrder>();
	private LinkedList<ButtonOrder> downOrders = new LinkedList<ButtonOrder>();

	private double position = 0;
	private int direction = 0;
	private int target = 0;

	private Communicator c;
	private ButtonOrderQue buttonOrderQue;
	private ButtonOrder currentOrder = null;

	private ReentrantLock orderLock = new ReentrantLock();
	private Condition newOrder = orderLock.newCondition();

	private ReentrantLock positionLock = new ReentrantLock();
	private Condition stop = positionLock.newCondition();

	private ReentrantLock doorLock = new ReentrantLock();
	private Condition doorOpenedAndClosed = doorLock.newCondition();
	private int doorMovements = 0;
	
	public RemoteController(int id, Communicator c, ButtonOrderQue q) {
		this.id = id;
		this.c = c;
		this.buttonOrderQue = q;
	}

	private boolean rightDirection(ButtonOrder b) {

		if(b.floor > target && direction == 1)
			return true;
		else if (b.floor < target && direction == -1)
			return true;
		else if (direction == 0) {
			return true;
		}

		return false;
	}

	public boolean putRequest(ButtonOrder c) {
		orderLock.lock();
		try {
			if(rightDirection(c)) {
				if(c.direction == -1) 
					downOrders.add(c);
				else
					upOrders.add(c);
				newOrder.signal();

				return true;
			}
			return false;
		} finally {
			orderLock.unlock();
		}
	}

	private void waitUntilArrive() {
		positionLock.lock();
		try {
			if(motor == 1) {
				while(position > target - 0.05) {
					System.err.println("ID: " + id + " Wait");
					stop.await();
				}
			} else {
				while(position < target + 0.05) {
					stop.await();
					System.err.println("ID: " + id + " Wait");
				}
			}
			System.err.println("ID: " + id +  " Left");
			openDoors();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			positionLock.unlock();
		}
	}

	public void doorMoved() {
		doorLock.lock();
		doorMovements++;
		if(doorMovements == 8)
			doorOpenedAndClosed.signal();
		
	}
	
	private void openDoors() {
		doorLock.lock();
		try {
			doorMovements = 0;
			
			while(doorMovements < 8) doorOpenedAndClosed.await();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			doorLock.unlock();
		}
	}
	
	public void setPosition(double pos) {
		positionLock.lock();
		try {
			if(position == pos) {
				doorMoved();
				return;
			}
			
			position = pos;
			if(Math.abs(pos - (double) target) < 0.05) {
				c.send("m " + id + " 0");
				motor = 0;
				buttonOrderQue.carriedOut(currentOrder);
				currentOrder = null;
				stop.signal();
			}
			if (Math.abs(pos - Math.round(pos)) < 0.05) {
				c.send("s " + id + " " + Math.round(pos));
			}
		} finally {
			positionLock.unlock();
		}
	}

	private ButtonOrder nextOrder() {
		orderLock.lock();
		try {
			if(direction == 1) {
				if(!upOrders.isEmpty()) {
					return mostImidietOrder(upOrders);
				}
			}
			if(direction == -1) {
				if(!downOrders.isEmpty()){
					return mostImidietOrder(downOrders);
				}
			}
			direction = 0;

			while(upOrders.isEmpty() && downOrders.isEmpty()) {
				newOrder.await();
			}

			if(!downOrders.isEmpty()) {
				direction = -1;
				return mostImidietOrder(downOrders);
			} else {
				direction = 1;
				return mostImidietOrder(upOrders);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			orderLock.unlock();
		}
		return null;
	}

	private ButtonOrder mostImidietOrder(LinkedList<ButtonOrder> orders) {
		while(orders.isEmpty())
		{
			try {
				newOrder.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		int pos = 0;
		int currentPos = 0;
		int val = MainController.amountOfFloors;
		for(ButtonOrder o : orders) {
			if(Math.abs(o.floor - (int) this.position) < val)
				pos = currentPos;
			currentPos++;
		}
		return orders.remove(pos);
	}

	private void move(ButtonOrder b) {
		orderLock.lock();
		positionLock.lock();
		try {
			target = b.floor;
			if(Math.abs(position - (double) target) < 0.05) {
				stop.signalAll(); 
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
			orderLock.unlock();
		}

		waitUntilArrive();
	}

	private boolean orderExist(ButtonOrder order) {
		LinkedList<ButtonOrder> que = (order.direction == -1) ? downOrders : upOrders;

		for(ButtonOrder b : que) {
			if(b.floor == order.floor)
				return true;
		}

		return false;
	}

	public void addOrder(int floor) {
		orderLock.lock();
		try {
			if((double) floor < position) {
				ButtonOrder order = new ButtonOrder(floor, -1);
				if(!orderExist(order)) {

					if(floor > target) {
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

					if(floor < target) {
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

	@Override
	public void run() {
		while(true) {
			currentOrder = nextOrder();
			move(currentOrder);
		}
	}
}

