package msdchallenge.old.main;

import msdchallenge.old.repository.IRepositoryWrapper;
import msdchallenge.old.repository.SesameNativeRepositoryWrapper;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a repository containing the data from all available filex except
 * train_triplets.txt . This repository may be used for fast evaluation of
 * algorithm and queries.
 * @author Nikolay
 *
 */
public class TestRepositoryFiller extends RepositoryFiller {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryFiller.class);

	public TestRepositoryFiller(Repository repository, RepositoryConnection connection) {
		super(repository, connection);
	}

	public static void main(String[] args) {
		// Parse all the parameters
//		Parameters params = new Parameters(args);
//		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_CONFIG, "config/owlim_test.ttl");
//		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_SHOWRESULTS, "true");
//		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_SHOWSTATS, "false");
//		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_UPDATES, "false");
//
//		LOG.info("Using parameters:");
//		LOG.info(params.toString());

//		IRepositoryWrapper wrapper = new OwlimRepositoryWrapper(params.getParameters());
		IRepositoryWrapper wrapper = SesameNativeRepositoryWrapper.getTestRepository();
		
		RepositoryFiller rf = new RepositoryFiller(wrapper.getRepository(), wrapper.getRepositoryConnection());
		rf.fillRepository();

		wrapper.shutdown();
	}

}
