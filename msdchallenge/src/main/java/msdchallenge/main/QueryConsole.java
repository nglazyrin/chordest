package msdchallenge.main;

import java.util.Scanner;

import msdchallenge.old.query.QueryCollector;
import msdchallenge.old.query.QueryEvaluator;
import msdchallenge.old.repository.IRepositoryWrapper;
import msdchallenge.old.repository.OwlimRepositoryWrapper;
import msdchallenge.old.repository.Parameters;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a repository and allows to run any query from /queries folder by
 * entering query file name in the console and pressing Enter. Type 'exit' and
 * press Enter to stop.
 * 
 * @author Nikolay
 *
 */
public class QueryConsole {

	private static final Logger LOG = LoggerFactory.getLogger(QueryConsole.class);

	public static void main(String[] args) {
		// Parse all the parameters
		Parameters params = new Parameters(args);
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_CONFIG, "config/owlim_test.ttl");
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_SHOWRESULTS, "true");
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_SHOWSTATS, "false");
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_UPDATES, "false");
//		params.setDefaultValue(RepositoryWrapper.PARAM_PRELOAD, PREFIX + "preload");
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_QUERYFILE, "scooter");

		LOG.info("Using parameters:");
		LOG.info(params.toString());

		IRepositoryWrapper m = new OwlimRepositoryWrapper(params.getParameters());

		QueryEvaluator q = new QueryEvaluator(m.getRepositoryConnection(), true);
		evaluate(q, params.getParameters().get(OwlimRepositoryWrapper.PARAM_QUERYFILE));
		
		m.shutdown();
	}

	private static void evaluate(QueryEvaluator evaluator, String firstFileName) {
		String fileName = firstFileName;
		Scanner in = new Scanner(System.in);
		while (! "exit".equalsIgnoreCase(fileName)) {
			String[] queries = QueryCollector.collectQueries("queries/" + fileName + ".sparql");
			evaluator.evaluateQueries(queries);
			fileName = StringUtils.strip(in.nextLine());
		}
		in.close();
	}

}
