package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ActionQue {
	private LinkedList<String> actionQue = new LinkedList<String>();
	private ReentrantLock queLock = new ReentrantLock();
	private Condition newAction = queLock.newCondition();
	
	public void putAction(String s) {
		queLock.lock();
		try {
			actionQue.add(s);
			newAction.signal();
		} finally {
			queLock.unlock();
		}
	}

	public String getAction() {
		queLock.lock();
		try {
			String a = actionQue.removeFirst();
			while(a == null) {
				newAction.await();
				a = actionQue.removeFirst();
			}
			
			return a;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			queLock.unlock();
		}
		return null;
	}
}
