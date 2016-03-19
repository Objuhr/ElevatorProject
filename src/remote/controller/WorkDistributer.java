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

			if(target != null) {
				System.err.println("Dist tries to give ele: " + target.getID() + 
						" bo: floor=" + order.floor + " dir=" + order.direction);
				// Try to put request
				if(target.putRequest(order)) {
					orderQue.acceptOrder(order);
					System.err.println("Dist gave ele: " + target.getID() + 
							" bo: floor=" + order.floor + " dir=" + order.direction);
				}
			}
		}
	}

	private int evaluateOrder(ButtonOrder b, RemoteController r) {
		if(r.isStopped()) return -1;

		int elevatorDirection = r.getDirection();
		if(elevatorDirection == b.direction) {
			if(elevatorDirection == 1) {
				if(b.floor > r.getTarget()) {
					return b.floor - r.getTarget() + Math.abs(r.getFloor() - r.getTarget());
				} else if (b.floor > r.getPosition()) {
					return b.floor - r.getFloor();
				}
			} else if (elevatorDirection == -1) {
				if(b.floor < r.getTarget()) {
					return  r.getTarget() - b.floor + Math.abs(r.getFloor() - r.getTarget());
				} else if (b.floor < r.getPosition()) {
					return r.getFloor() - b.floor;
				}
			}
		}
		if(elevatorDirection == 0)
			return Math.abs(b.floor - r.getFloor()) + MainController.amountOfFloors*2;
		else
			return (MainController.amountOfFloors*3 + Math.abs(b.floor - r.getTarget()) + r.getNumberOfOrders());
	}
}
