package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RemoteController extends Thread {
	private int id = - 1;
	
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
			while(Math.abs(position - target) >= 0.01) {
				stop.await();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			positionLock.unlock();
		}
	}

	public void setPosition(double pos) {
		positionLock.lock();
		try {
			System.err.println("Pos: " + pos + " target: " + target + " dir: " + direction);
			position = pos;
			if(Math.abs(pos - target) < 0.01) {
				c.send("m " + id + " 0");
				buttonOrderQue.carriedOut(currentOrder);
				currentOrder = null;
				stop.signal();
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
		int pos = 0;
		int currentPos = 0;
		int val = orders.getFirst().floor;
		for(ButtonOrder o : orders) {
			if(Math.abs(o.floor - (int) this.position) < val)
				pos = currentPos;
			currentPos++;
		}
		return orders.remove(pos);
	}

	private void move(ButtonOrder b) {
		orderLock.lock();
		try {
			target = b.floor;
			if(Math.abs(position - target) < 0.05) return;

			if(target > position) {
				c.send("m " + id + " 1");
			} else {
				c.send("m " + id + " -1");
			}
		} finally {
			orderLock.unlock();
		}

		waitUntilArrive();
	}

	public void addOrder(int floor) {
		orderLock.lock();
		if(floor == target) return;
		try {
			if(floor < target) 
				downOrders.add(new ButtonOrder(floor, -1));
			else 
				upOrders.add(new ButtonOrder(floor, 1));

			newOrder.signal();
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

