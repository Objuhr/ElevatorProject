package remote.controller;

public class WorkDistributer extends Thread {
	private ButtonOrderQue orderQue;

	public WorkDistributer(ButtonOrderQue q) {
		orderQue = q;
	}

	@Override
	public void run() {
		while(true) {
			ButtonOrder order = orderQue.get();

			int topPriority = MainController.amountOfFloors*10;
			RemoteController target = null;


			for(RemoteController r : MainController.controllers) {
				int priority = evaluateOrder(order, r);
				if(priority == -1) 
					continue;
				else if(priority < topPriority) {
					topPriority = priority;
					target = r;
				}
			}

			if(target == null)
				orderQue.put(order);
			else {
				// Try to put request
				if(target.putRequest(order)) {
					System.err.println("Order added to " + target.getID());
					orderQue.acceptOrder(order);
				} else {
					orderQue.put(order);
				}
			}
		}
	}

	private int evaluateOrder(ButtonOrder b, RemoteController r) {
		if(r.isStopped()) return -1;

		int elevatorDirection = r.getDirection();
		if(elevatorDirection == b.direction || elevatorDirection == 0) {
			if(elevatorDirection == 1) {
				if(b.floor > r.getTarget()) {
					return b.floor - r.getTarget() + r.getNumberOfOrders();
				}
			} else if (elevatorDirection == -1) {
				if(b.floor < r.getTarget()) {
					return  r.getTarget() - b.floor + r.getNumberOfOrders();
				}
			}
		}
		if(elevatorDirection == 0)
			return Math.abs(b.floor - r.getPosition()) + r.getNumberOfOrders();
		else
			return (MainController.amountOfFloors + Math.abs(b.floor - r.getTarget()) + r.getNumberOfOrders()) + 2;
	}
}
