package msdchallenge.repository;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SesameNativeRepositoryWrapper implements IRepositoryWrapper {

	private static final Logger LOG = LoggerFactory.getLogger(SesameNativeRepositoryWrapper.class);

	private final Repository repository;

	private final RepositoryConnection repositoryConnection;

	public SesameNativeRepositoryWrapper() {
		File dataDir = new File("sesame_repository/");
		String indexes = "spoc,posc,cosp";
		repository = new SailRepository(new NativeStore(dataDir, indexes));
		try {
			repository.initialize();
			LOG.info("repository initialized");
		} catch (RepositoryException e) {
			e.printStackTrace();
			throw new IllegalStateException("Error when initializing repository", e);
		}
		try {
			repositoryConnection = repository.getConnection();
			repositoryConnection.setAutoCommit(false);
			LOG.info("repository connection initialized");
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalStateException("Error when initializing repository connection", e);
		}
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public RepositoryConnection getRepositoryConnection() {
		return repositoryConnection;
	}

	@Override
	public void shutdown() {
		try {
			repositoryConnection.close();
			LOG.info("repository connection closed");
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		try {
			repository.shutDown();
			LOG.info("repository shut down");
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}

}
