package msdchallenge.input;


import msdchallenge.input.ListeningsFileReader.IListeningProcessor;
import msdchallenge.input.SongNumbersFileReader.ISongNumberProcessor;
import msdchallenge.input.TracksFileReader.ITrackProcessor;
import msdchallenge.input.UsersFileReader.IUserProcessor;
import msdchallenge.main.RepositoryFiller;
import msdchallenge.model.Listening;
import msdchallenge.model.Track;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InputProcessor implements IListeningProcessor, ITrackProcessor,
ISongNumberProcessor, IUserProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryFiller.class);

	private final static String NS = "http://example.org/owlim#";

	private final ValueFactory factory;
	private final RepositoryConnection connection;

	private final URI hasAuthor;
	private final URI hasName;
	private final URI hasNumber;
	private final URI isKaggleUser;
	private final URI listens;

	private int updatesCount = 0;

	public InputProcessor(ValueFactory factory, RepositoryConnection connection) {
		if (factory == null) { throw new NullPointerException(); }
		if (connection == null) { throw new NullPointerException(); }
		this.factory = factory;
		this.connection = connection;
		
		hasAuthor = factory.createURI(NS + "hasAuthor");
		hasName = factory.createURI(NS + "hasName");
		hasNumber = factory.createURI(NS + "hasNumber");
		isKaggleUser = factory.createURI(NS + "isKaggleUser");
		listens = factory.createURI(NS + "listens");
	}

	@Override
	public void process(Listening listening) {
		URI userId = factory.createURI(NS + listening.userId);
		URI trackId = factory.createURI(NS + listening.trackId);
		try {
			connection.add(userId, listens, trackId);
			if (updatesCount++ >= 1000000) {
				commit();
			}
		} catch (RepositoryException e) {
			LOG.error("Error adding new listening to repository", e);
		}
	}

	@Override
	public void process(Track track) {
		Literal name = factory.createLiteral(track.song);
		Literal author = factory.createLiteral(track.artist);
		URI trackId = factory.createURI(NS + track.trackId);
		try {
			connection.add(trackId, hasAuthor, author);
			connection.add(trackId, hasName, name);
		} catch (RepositoryException e) {
			LOG.error("Error adding new track to repository", e);
		}
	}

	@Override
	public void process(String trackId, int number) {
		URI subj = factory.createURI(NS + trackId);
		Literal obj = factory.createLiteral(number);
		try {
			connection.add(subj, hasNumber, obj);
		} catch (RepositoryException e) {
			LOG.error("Error adding new track number to repository", e);
		}
	}

	@Override
	public void process(String kaggleUser) {
		URI subj = factory.createURI(NS + kaggleUser);
		Literal obj = factory.createLiteral(true);
		try {
			connection.add(subj, isKaggleUser, obj);
		} catch (RepositoryException e) {
			LOG.error("Error adding new user to repository", e);
		}
	}

	private void commit() {
		try {
			connection.commit();
			updatesCount = 0;
		} catch (RepositoryException e) {
			LOG.error("Error performing commit", e);
		}
	}

}
