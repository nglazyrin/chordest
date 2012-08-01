package msdchallenge.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import msdchallenge.input.ListeningsFileReader;
import msdchallenge.model.Listening;
import msdchallenge.simple.AbstractMsdcWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllUsersFileGenerator extends AbstractMsdcWorker {

	private static final Logger LOG = LoggerFactory.getLogger(AllUsersFileGenerator.class);

	private static final String RESULT_FILE = KAGGLE_DIR + "all_users.txt";

	private FileWriter writer;

	private int usersProcessed = 0;

	public static void main(String[] args) {
		AllUsersFileGenerator lc = new AllUsersFileGenerator();
		lc.findAllUsers();
	}

	public AllUsersFileGenerator() {
		lastUser = "";
		try {
			writer = new FileWriter(RESULT_FILE);
		} catch (IOException e) {
			LOG.error("Could not open result file", e);
		}
	}

	public void findAllUsers() {
		File trainListenings = new File(TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(trainListenings, this);

		File testListenings = new File(TEST_TRIPLETS_FILE);
		ListeningsFileReader.process(testListenings, this);
		
		LOG.info(usersProcessed + " users in total");
		
		closeWriter();
	}

	private void closeWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			LOG.error("Error closing result file", e);
		}
	}

	@Override
	public void process(Listening listening) {
		if (! lastUser.equals(listening.userId)) {
			lastUser = listening.userId;
			usersProcessed++;
			try {
				writer.write(lastUser);
				writer.write("\r\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
