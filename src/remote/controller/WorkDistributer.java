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

			for(RemoteController r : MainController.controllers) {
				if(r.putRequest(order)) {
					orderQue.acceptOrder(order);
					break;
				}
			}

		}
	}
	
//	private int evaluateOrder(ButtonOrder b, RemoteController r) {
//		
//	}
}
