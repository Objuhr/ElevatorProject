package remote.controller;

public class WorkDistributer extends Thread {
	private ButtonOrderQue orderQue;

	public WorkDistributer(ButtonOrderQue q) {
		orderQue = q;
	}

	@Override
	public void run() {
		while(true) {
			distributeOrder();
			balanceLoad(1);
			balanceLoad(-1);
		}
	}

	private void balanceLoad(int dir) {
		RemoteController target = null;

		// Find most extremt positioned elevator
		for(RemoteController controller : MainController.controllers) {
			
			if(controller.getDirection() != dir) continue;

			if(target == null) {
				target = controller;

			} else if (dir == 1) {
				if(controller.getTarget() < target.getTarget()) 
					target = controller;

				// if dir == -1
			} else {
				if (controller.getTarget() > target.getTarget())
					target = controller;
			}

		}

		// If no elevator in that direction exist, return
		if(target == null) return;

		for(RemoteController controller : MainController.controllers) {
			if(controller == target) continue;

			if(controller.getDirection() == dir && !controller.gotPassengers()) {
				target.retrieveOrders(controller);
			}
		}
	}

	private void distributeOrder() {
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
			// Try to put request
			if(target.putRequest(order)) {
				orderQue.acceptOrder(order);
			}
		}
	}

	private int evaluateOrder(ButtonOrder button, RemoteController r) {
		if(r.isStopped()) return -1;

		int elevatorDirection = r.getDirection();
		if(elevatorDirection == button.direction) {
			if(elevatorDirection == 1) {
				if(button.floor > r.getTarget()) {
					return button.floor - r.getTarget() + Math.abs(r.getFloor() - r.getTarget());
				} else if (button.floor > r.getPosition()) {
					return button.floor - r.getFloor();
				} else if (!r.gotPassengers()) {
					return Math.abs(button.floor - r.getTarget());
				}
			} else if (elevatorDirection == -1) {
				if(button.floor < r.getTarget()) {
					return  r.getTarget() - button.floor + Math.abs(r.getFloor() - r.getTarget());
				} else if (button.floor < r.getPosition()) {
					return r.getFloor() - button.floor;
				} else if (!r.gotPassengers()) {
					return Math.abs(button.floor - r.getTarget());
				}
			}

			// If elevatorDirection != button.direction
		} else if(r.getMotor() == button.direction && !r.gotPassengers()){

			if(elevatorDirection == 1) {
				if((double)button.floor < r.getPosition() && button.floor > r.getTarget()) {
					return (int) Math.abs(r.getPosition() - button.floor);
				}
			} else if(elevatorDirection == -1) {
				if((double)button.floor > r.getPosition() && button.floor < r.getTarget()) {
					return (int) Math.abs(r.getPosition() - button.floor);
				}
			}
		}
		if(elevatorDirection == 0)
			return Math.abs(button.floor - r.getFloor()) + MainController.amountOfFloors*2;
		else
			return (MainController.amountOfFloors*3 + Math.abs(button.floor - r.getTarget()) + r.getNumberOfOrders());
	}
}
