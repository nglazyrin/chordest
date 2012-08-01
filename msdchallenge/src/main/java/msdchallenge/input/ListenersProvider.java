package msdchallenge.input;

import msdchallenge.simple.AbstractMsdcWorker;

public class ListenersProvider {

	private String[][] listeners = new String[AbstractMsdcWorker.TOTAL_TRACKS][];

	public ListenersProvider() {
		for (int i = 0; i < 78; i++) {
			int number = i * AbstractMsdcWorker.PROCESS_TRACKS;
			String listenersFileName = AbstractMsdcWorker.DATA_DIR + "listeners" + number + ".bin";
			String[][] listenersLocal = AbstractMsdcWorker.deserialize(listenersFileName);
			for (int j = 0; j < AbstractMsdcWorker.PROCESS_TRACKS; j++) {
				listeners[j + number] = listenersLocal[j];
			}
		}
	}

	public String[][] getListeners() {
		return listeners;
	}

}
