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

			boolean added = false;
			for(RemoteController r : MainController.controllers) {
				if(r.putRequest(order)) {
					added = true;
					break;
				}
			}
			if(!added) {
				orderQue.put(order);
			}

		}
	}
}
