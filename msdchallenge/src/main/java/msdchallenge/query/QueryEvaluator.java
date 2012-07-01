package msdchallenge.query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class QueryEvaluator {

	private static SimpleDateFormat logTimestamp = new SimpleDateFormat("HH:mm:ss ");
	private static final QueryLanguage[] queryLanguages = new QueryLanguage[] {
			QueryLanguage.SPARQL, QueryLanguage.SERQL, QueryLanguage.SERQO };

	private static void log(String message) {
		System.out.println(logTimestamp.format(new Date()) + message);
	}

	// Flag for executing queries using multiple threads
	private boolean doMultithreadQueryEvaluation = false;

	private Map<String, String> namespacePrefixes = new HashMap<String, String>();

	// From repository.getConnection() - the connection through which we will use the repository
	private final RepositoryConnection repositoryConnection;

	// A flag to indicate whether query results should be output.
	private final boolean showResults;

	public QueryEvaluator(RepositoryConnection connection, boolean showResults) {
		this.repositoryConnection = connection;
		this.showResults = showResults;
	}

	/**
	 * Demonstrates query evaluation. First parse the query file. Each of the
	 * queries is executed against the prepared repository. If the printResults
	 * is set to true the actual values of the bindings are output to the
	 * console. We also count the time for evaluation and the number of results
	 * per query and output this information.
	 */
	public void evaluateQueries(String[] queries) {
		log("===== Query Evaluation ======================");
		long startQueries = System.currentTimeMillis();

		final CountDownLatch numberOfQueriesToProcess = new CountDownLatch(queries.length);
		// evaluate each query and, optionally, print the bindings
		for (int i = 0; i < queries.length; i++) {
			final String name = queries[i].substring(0, queries[i].indexOf(":"));
			final String query = queries[i].substring(name.length() + 2).trim();
			log("Executing query '" + name + "'");

			// this is done via invoking the respoitory's performTableQuery()
			// method
			// the first argument specifies the query language
			// the second is the actual query string
			// the result is returned in a tabular form with columns, the
			// variables in the projection
			// and each result in a separate row. these are simply enumerated
			// and shown in the console
			if (doMultithreadQueryEvaluation) {
				new Thread() {
					@Override public void run() {
						QueryResult<?> result = executeSingleQuery(query);
						try {
							if (result != null) { result.close(); }
						} catch (QueryEvaluationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							numberOfQueriesToProcess.countDown();
						}
					} // run
				}.start(); // thread
			} else {
				QueryResult<?> result = executeSingleQuery(query);
				try {
					if (result != null) { result.close(); }
				} catch (QueryEvaluationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // for
		if (doMultithreadQueryEvaluation) {
			try {
				numberOfQueriesToProcess.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}

		long endQueries = System.currentTimeMillis();
		log("Queries run in " + (endQueries - startQueries) + " ms.");
	}

	public QueryResult<?> executeSingleQuery(String query) {
		try {
			Query preparedQuery = prepareQuery(query);
			if (preparedQuery == null) {
				log("Unable to parse query: " + query);
				return null;
			} else if (preparedQuery instanceof BooleanQuery) {
				log("Result: " + ((BooleanQuery) preparedQuery).evaluate());
				return null;
			} else if (preparedQuery instanceof GraphQuery) {
				GraphQuery q = (GraphQuery) preparedQuery;
				long queryBegin = System.nanoTime();

				GraphQueryResult result = q.evaluate();
				if (showResults) {
					printGraphQueryResult(result, queryBegin);
				}
				return result;
//				result.close();
			} else if (preparedQuery instanceof TupleQuery) {
				TupleQuery q = (TupleQuery) preparedQuery;
				long queryBegin = System.nanoTime();

				TupleQueryResult result = q.evaluate();
				if (showResults) {
					printTupleQueryResult(result, queryBegin);
				}
				return result;
//				result.close();
			}
		} catch (Throwable e) {
			log("An error occurred during query execution: " + e.getMessage());
		}
		return null;
	}

	private void printTupleQueryResult(TupleQueryResult result, long queryBegin)
			throws QueryEvaluationException {
		int rows = 0;
		while (result.hasNext()) {
			BindingSet tuple = result.next();
			if (rows == 0) {
				for (Iterator<Binding> iter = tuple.iterator(); iter.hasNext();) {
					System.out.print(iter.next().getName());
					System.out.print("\t");
				}
				System.out.println();
				System.out.println("---------------------------------------------");
			}
			
			rows++;
			for (Iterator<Binding> iter = tuple.iterator(); iter.hasNext();) {
				try {
					System.out.print(beautifyRDFValue(iter.next().getValue()) + "\t");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println();
		}
		System.out.println();
		long queryEnd = System.nanoTime();
		log(rows + " result(s) in " + (queryEnd - queryBegin) / 1000000 + "ms.");
	}

	private void printGraphQueryResult(GraphQueryResult result, long queryBegin)
			throws QueryEvaluationException, Exception {
		int rows = 0;
		while (result.hasNext()) {
			Statement statement = result.next();
			rows++;
			System.out.print(beautifyRDFValue(statement.getSubject()));
			System.out.print(" " + beautifyRDFValue(statement.getPredicate()) + " ");
			System.out.print(" " + beautifyRDFValue(statement.getObject()) + " ");
			Resource context = statement.getContext();
			if (context != null) {
				System.out.print(" " + beautifyRDFValue(context) + " ");
			}
			System.out.println();
		}
		System.out.println();
		long queryEnd = System.nanoTime();
		log(rows + " result(s) in " + (queryEnd - queryBegin) / 1000000 + "ms.");
	}

	private Query prepareQuery(String query) throws Exception {
		for (QueryLanguage language : queryLanguages) {
			Query result = prepareQuery(query, language);
			if (result != null) {
				return result;
			}
		}
		// Can't prepare this query in any language
		return null;
	}

	/**
	 * The purpose of this method is to try to parse a query locally in order to
	 * determine if the query is a tuple (SELECT), boolean (ASK) or graph
	 * (CONSTRUCT/DESCRIBE) query. This happens automatically if the repository
	 * is local, but for a remote repository the local HTTPClient side can not
	 * work it out. Therefore a temporary in memory SAIL is created and used to
	 * determine the query type.
	 * 
	 * @param query
	 *            Query string to be parsed
	 * @param language
	 *            The query language to assume
	 * @return A parsed query object or null if not possible
	 * @throws RepositoryException
	 *             If the local repository used to test the query type failed
	 *             for some reason
	 */
	private Query prepareQuery(String query, QueryLanguage language) throws RepositoryException {
		Repository tempRepository = new SailRepository(new MemoryStore());
		tempRepository.initialize();

		RepositoryConnection tempConnection = tempRepository.getConnection();

		try {
			try {
				tempConnection.prepareTupleQuery(language, query);
				return repositoryConnection.prepareTupleQuery(language, query);
			} catch (Exception e) { }
			try {
				tempConnection.prepareBooleanQuery(language, query);
				return repositoryConnection.prepareBooleanQuery(language, query);
			} catch (Exception e) { }
			try {
				tempConnection.prepareGraphQuery(language, query);
				return repositoryConnection.prepareGraphQuery(language, query);
			} catch (Exception e) { }
			return null;
		} finally {
			try {
				tempConnection.close();
				tempRepository.shutDown();
			} catch (Exception e) { }
		}
	}

	/**
	 * Auxiliary method, printing an RDF value in a "fancy" manner. In case of
	 * URI, qnames are printed for better readability
	 * 
	 * @param value
	 *            The value to beautify
	 */
	private String beautifyRDFValue(Value value) throws Exception {
		if (value instanceof URI) {
			URI u = (URI) value;
			String namespace = u.getNamespace();
			String prefix = namespacePrefixes.get(namespace);
			if (prefix == null) {
				prefix = u.getNamespace();
			} else {
				prefix += ":";
			}
			return prefix + u.getLocalName();
		} else {
			return value.toString();
		}
	}

}
