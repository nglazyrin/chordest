package msdchallenge.input.provider;

import java.io.File;
import java.util.HashMap;

import msdchallenge.input.reader.UsersFileReader;
import msdchallenge.input.reader.UsersFileReader.IUserProcessor;

public class UsersProvider implements IUserProcessor {

	private final HashMap<String, Integer> users = new HashMap<String, Integer>();

	private int usersProcessed = 0;

	public UsersProvider(String allUsersFileName) {
		File file = new File(allUsersFileName);
		UsersFileReader.process(file, this);
	}

	@Override
	public void process(String user) {
		users.put(user, usersProcessed++);
	}

	public HashMap<String, Integer> getUsers() {
		return users;
	}

}
