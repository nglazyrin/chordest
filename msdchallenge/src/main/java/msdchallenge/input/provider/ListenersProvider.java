package msdchallenge.input.provider;

import msdchallenge.simple.Constants;
import msdchallenge.simple.IoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenersProvider {

	private static final Logger LOG = LoggerFactory.getLogger(ListenersProvider.class);

	private String[][] listeners = new String[Constants.TOTAL_TRACKS][];

	public ListenersProvider() {
		for (int i = 0; i < 78; i++) {
			int number = i * Constants.PROCESS_TRACKS;
			String listenersFileName = Constants.DATA_DIR + "listeners" + number + ".bin";
			String[][] listenersLocal = IoUtil.deserialize(listenersFileName);
			for (int j = 0; j < Constants.PROCESS_TRACKS; j++) {
				listeners[j + number] = listenersLocal[j];
			}
		}
		LOG.info(listeners.length + " tracks' listeners have been read from " + Constants.DATA_DIR + "listeners*.bin");
	}

	public String[][] getListeners() {
		return listeners;
	}

}
