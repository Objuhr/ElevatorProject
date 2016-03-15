package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ButtonOrderQue {
	private LinkedList<ButtonOrder> que = new LinkedList<ButtonOrder>();
	private ReentrantLock queLock = new ReentrantLock();
	private Condition newElements = queLock.newCondition();
	
	public ButtonOrder get() {
		queLock.lock();
		try {
			while(que.size() == 0) newElements.await();
			return que.removeFirst();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			queLock.unlock();
		}
		
		return null;
	}
	
	public void put(ButtonOrder val) {
		queLock.lock();
		try {
			que.add(val);
			newElements.signal();
		} finally {
			queLock.unlock();
		}
	}
}
