package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ButtonOrderQue {
	private LinkedList<ButtonOrder> pendingOrders = new LinkedList<ButtonOrder>();
	private LinkedList<ButtonOrder> acceptedOrders = new LinkedList<ButtonOrder>();

	private ReentrantLock queLock = new ReentrantLock();
	private Condition newElements = queLock.newCondition();

	public void acceptOrder(ButtonOrder b) {
		queLock.lock();
		try {
			acceptedOrders.add(b);
			pendingOrders.remove(b);
		} finally {
			queLock.unlock();
		}
	}

	public void carriedOut(ButtonOrder b) {
		queLock.lock();
		try {
			acceptedOrders.remove(b);
		} finally {
			queLock.unlock();
		}
	}

	public ButtonOrder get() {
		queLock.lock();
		try {
			while(pendingOrders.isEmpty()) newElements.await();

			ButtonOrder order = pendingOrders.removeLast();
			pendingOrders.add(order);
			return order;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			queLock.unlock();
		}

		return null;
	}

	public boolean isEmpty() {
		return pendingOrders.isEmpty();
	}

	public void put(ButtonOrder b) {
		queLock.lock();
		try {
			if(!orderExist(b)){
				pendingOrders.add(b);
				newElements.signal();
			}
		} finally {
			queLock.unlock();
		}
	}

	private boolean orderExist(ButtonOrder b) {
		for(ButtonOrder o : pendingOrders) {
			if(o.direction == b.direction && o.floor == b.floor)
				return true;
		}
		for(ButtonOrder o : acceptedOrders) {
			if(o.direction == b.direction && o.floor == b.floor)
				return true;
		}

		return false;
	}

	public void returnList(LinkedList<ButtonOrder> list) {
		queLock.lock();
		try {
			for(ButtonOrder b : list) {
				if(!orderExist(b)) {
					pendingOrders.add(b);
				}
			}
		} finally {
			queLock.unlock();
		}
	}
}
