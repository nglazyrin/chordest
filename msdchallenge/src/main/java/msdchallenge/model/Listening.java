package msdchallenge.model;

public class Listening {

	public final String userId;
	public final String trackId;
	public final int count;

	public Listening(String userId, String trackId, int count) {
		this.userId = userId;
		this.trackId = trackId;
		this.count = count;
	}

}
