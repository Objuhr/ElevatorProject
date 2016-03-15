package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RemoteController {
	private int id = - 1;
	private LinkedList<ButtonCalls> orders = new LinkedList<ButtonCalls>();
	private double position = 0;
	private int direction = 0;
	private double target = 0;

	private ReentrantLock orderLock = new ReentrantLock();
	private Condition newOrder = orderLock.newCondition();
	
	private ReentrantLock positionLock = new ReentrantLock();
	private Condition stop = orderLock.newCondition();
	
	public RemoteController(int id) {
		this.id = id;
	}
	
	public void putOrder(ButtonCalls c) {
		orderLock.lock();
		try {
			orders.add(c);
			newOrder.signal();
		} finally {
			orderLock.unlock();
		}
	}
	
	public void setPosition(int pos) {
		position = pos;
		if(direction == 1) {
			if(position >= target) 
				stop.signal();
		} else if(direction == -1) {
			if(position <= target)
				stop.signal();
		}
	}
	
	private ButtonCalls nextOrder() {
		orderLock.lock();
		try {
			return orders.removeFirst();
		} finally {
			orderLock.unlock();
		}
	}
}
