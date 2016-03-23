package remote.controller;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/********************************************
 * 
 *  Used for the receiver to store input 
 *  strings from the tcp connection and for
 *  the parser to collect the strings.
 *
 ********************************************/

public class ActionQue {
	private LinkedList<String> inputStrings = new LinkedList<String>();	// Used to store the strings
	private ReentrantLock queLock = new ReentrantLock();				// Lock to ensure mutual exclusion to the linked-list (actionQue)
	private Condition newInputString = queLock.newCondition();			// Condition variable that is signaled when new input is available
	
	/*
	 * Add an input string to the linked-list
	 */
	public void putAction(String s) {
		queLock.lock();
		try {
			inputStrings.add(s);
			newInputString.signal();
		} finally {
			queLock.unlock();
		}
	}

	/*
	 * Get an input string from the linked-list
	 */
	public String getAction() {
		queLock.lock();
		try {
			// Wait until the linked-list is not empty
			while(inputStrings.isEmpty()) 
				newInputString.await();

			return inputStrings.removeFirst();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			queLock.unlock();
		}
		return null;
	}
}
