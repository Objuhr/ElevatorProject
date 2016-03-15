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
	private double target = 0;
	Communicator c;

	private ReentrantLock orderLock = new ReentrantLock();
	private Condition newOrder = orderLock.newCondition();

	private ReentrantLock positionLock = new ReentrantLock();
	private Condition stop = orderLock.newCondition();

	public RemoteController(int id, Communicator c) {
		this.id = id;
		this.c = c;
	}

	private boolean rightDirection(ButtonOrder b) {
		if(b.floor >= target && direction == 1)
			return true;
		else if (b.floor <= target && direction == -1)
			return true;
		else if (direction == 0) {
			if(b.floor > position) direction = 1;
			if(b.floor < position) direction = -1;
			return true;
		}

		return false;
	}

	public boolean putOrder(ButtonOrder c) {
		orderLock.lock();
		if(rightDirection(c)) {
			try {
				if(c.direction == -1) 
					downOrders.add(c);
				else
					upOrders.add(c);
				newOrder.signal();
			} finally {
				orderLock.unlock();
			}
			return true;
		}
		return false;
	}

	public void setPosition(double pos) {
		positionLock.lock();
		try {
			position = pos;
			if(direction == 1) {
				if(position >= target) {
					c.send("m " + id + " 0");
					stop.signal();
				}
			} else if(direction == -1) {
				if(position <= target)
					c.send("m " + id + " 0");
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
				if(upOrders.size() == 0) {
					direction = 0;
				} else {
					return mostImidietOrder(upOrders);
				}
			}
			if(direction == -1) {
				if(downOrders.size() == 0){
					direction = 0;
				} else {
					return mostImidietOrder(downOrders);
				}
			}
			while(upOrders.size() == 0 && downOrders.size() == 0) {
				newOrder.await();
			}
			LinkedList<ButtonOrder> l = null;
			
			if(downOrders.size() != 0) {
				direction = -1;
				l = downOrders;
			} else {
				direction = 1;
				l = upOrders;
			}
			
			return mostImidietOrder(l);

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
	
	private void startToMove(ButtonOrder b) {
		target = b.floor;
		if(target > position) {
			c.send("m " + id + " 1");
		} else {
			c.send("m " + id + " -1");
		}
	}
}

