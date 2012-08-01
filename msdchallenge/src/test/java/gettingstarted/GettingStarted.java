package gettingstarted;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import msdchallenge.old.repository.Parameters;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.sail.memory.MemoryStore;


/**
 * <p>
 * This sample application is intended to illustrate how to prepare, configure
 * and run a <a href="http://www.openrdf.org">Sesame</a> repository using the <a
 * href="http://www.ontotext.com/owlim/">OWLIM</a> SAIL. The basic operations
 * are demonstrated in separate methods: get namespaces, evaluate queries, add
 * and delete statements, parse and load files, etc.
 * </p>
 * <p>
 * Addition and removal are performed only when the input parameter 'updates' is
 * set to 'true'. Thus, potentially slow and irrelevant delete operations are
 * avoided in case the example is adapted for loading large data-sets.
 * </p>
 * <p>
 * This application can be used also as an easy test-bed for loading and
 * querying different ontologies and data-sets without needing to build a
 * separate application.
 * </p>
 * <p>
 * The command line parameters are given as key=value' pairs as follows:
 * </p>
 * <ul>
 * <li>config - the name of the OWLIM configuration file (default './owlim.ttl')
 * </li>
 * <li>exportfile - dump the repository contents to the given filename</li>
 * <li>exporttype - export all/explicit/implicit statements, default is explicit
 * </li>
 * <li>exportformat - the RDF format:</li>
 * <li>preload - the name of a file or directory containing RDF data
 * -directories are recursively scanned (default './preload')</li>
 * <li>queryfile - the name of a file containing queries to execute - SPARQL or
 * SERQL (default './queries/sample.sparql')</li>
 * <li>repository - used in conjunction with the 'url' parameter, used to
 * identify the repository on the server</li>
 * <li>showresults - indicates whether to enumerate query results (default
 * 'true')</li>
 * <li>showstats - indicates whether to show initialisation statistics after
 * loading the selected data files (default 'false')</li>
 * <li>updates - indicates whether to perform the test statement insert and
 * delete operations (default 'false')</li>
 * <li>url - used in conjunction with the 'repository' parameter, a URL that
 * identifies the Sesame server. This overrides the 'config' parameter</li>
 * </ul>
 * 
 * @author Damyan Ognyanoff (damyan@sirma.bg)
 */
public class GettingStarted {
	// Some system properties used to add some flexibility
	public static String PARAM_CONFIG = "config";
	public static String PARAM_PRELOAD = "preload";
	public static String PARAM_QUERYFILE = "queryfile";
	public static String PARAM_REPOSITORY = "repository";
	public static String PARAM_SHOWRESULTS = "showresults";
	public static String PARAM_SHOWSTATS = "showstats";
	public static String PARAM_UPDATES = "updates";
	public static String PARAM_URL = "url";
	public static String PARAM_CONTEXT = "context";
	public static String PARAM_EXPORT_FILE = "exportfile";
	public static String PARAM_EXPORT_TYPE = "exporttype";
	public static String PARAM_EXPORT_FORMAT = "exportformat";

	// The storage for the command line parameters
	private Map<String, String> parameters;

	// A map of namespace-to-prefix
	private Map<String, String> namespacePrefixes = new HashMap<String, String>();

	// The repository manager
	private RepositoryManager repositoryManager;

	// From repositoryManager.getRepository(...) - the actual repository we will
	// work with
	private Repository repository;

	// From repository.getConnection() - the connection through which we will
	// use the repository
	private RepositoryConnection repositoryConnection;

	// A flag to indicate whether query results should be output.
	private boolean showResults = false;

	// Flag for executing queries using multiple threads
	private boolean doMultithreadQueryEvaluation = false;

	/**
	 * Constructor - uses a map of configuration parameters to initialise the
	 * application
	 * <ul>
	 * <li>uses the configuration file and repository ID to initialise a
	 * LocalRepositoryManager and instantiate a repository, OR</li>
	 * <li>initialises a RemoteRepositoryManager and connects to the remote
	 * repository given by the 'url' parameter</li>
	 * </ul>
	 * 
	 * @param parameters
	 *            a map of configuration parameters
	 */
	public GettingStarted(Map<String, String> parameters) {

		this.parameters = parameters;

		log("===== Initialize and load imported ontologies =========");

		// Set the 'output results' flag
		showResults = isTrue(PARAM_SHOWRESULTS);

		String url = parameters.get(PARAM_URL);
		String repositoryId = null;
		if (url == null) {
			// The configuration file
			String configFilename = parameters.get(PARAM_CONFIG);
			File configFile = new File(configFilename);
			log("Using configuration file: " + configFile.getAbsolutePath());

			// Parse the configuration file, assuming it is in Turtle format
			Graph repositoryRdfDescription = null;

			try {
				repositoryRdfDescription = parseFile(configFile,
						RDFFormat.TURTLE, "http://example.org#");
			} catch (OpenRDFException e) {
				log("There was an error reading/parsing the Turtle configuration file '"
						+ configFilename + "': " + e.getMessage());
			} catch (FileNotFoundException e) {
				log("The turtle configuration file '" + configFilename
						+ "' was not found, please check the '" + PARAM_CONFIG
						+ "' parameter");
			} catch (IOException e) {
				log("An I/O error occurred while processing the configuration file '"
						+ configFilename + "': " + e.getMessage());
			}

			if (repositoryRdfDescription == null)
				System.exit(-1);

			// Look for the subject of the first matching statement for
			// "?s type Repository"
			final String repositoryUri = "http://www.openrdf.org/config/repository#Repository";
			final String repositoryIdUri = "http://www.openrdf.org/config/repository#repositoryID";
			Iterator<Statement> iter = repositoryRdfDescription.match(null,
					RDF.TYPE, new URIImpl(repositoryUri));
			Resource repositoryNode = null;
			if (iter.hasNext()) {
				Statement st = iter.next();
				repositoryNode = st.getSubject();
			}
			if (repositoryNode == null) {
				log("The turtle configuration file '"
						+ configFile.getName()
						+ "' does not contain a valid repository description, because it is missing a resource with rdf:type <"
						+ repositoryUri + ">");
				System.exit(-2);
			}

			// Get the repository ID (and ignore the one passed with the
			// 'repository' parameter
			iter = repositoryRdfDescription.match(repositoryNode, new URIImpl(
					repositoryIdUri), null);
			if (iter.hasNext()) {
				Statement st = iter.next();
				repositoryId = st.getObject().stringValue();
			} else {
				log("The turtle configuration file '"
						+ configFile.getName()
						+ "' does not contain a valid repository description, because it is missing a <"
						+ repositoryUri + "> with a property <"
						+ repositoryIdUri + ">");
				System.exit(-2);
			}

			try {
				// Create a manager for local repositories and initialise it
				repositoryManager = new LocalRepositoryManager(new File("."));
				repositoryManager.initialize();
			} catch (RepositoryException e) {
				log("");
				System.exit(-3);
			}

			try {
				// Create a configuration object from the configuration file and
				// add
				// it to the repositoryManager
				RepositoryConfig repositoryConfig = RepositoryConfig.create(
						repositoryRdfDescription, repositoryNode);
				repositoryManager.addRepositoryConfig(repositoryConfig);
			} catch (OpenRDFException e) {
				log("Unable to process the repository configuration: "
						+ e.getMessage());
				System.exit(-4);
			}
		} else {
			repositoryId = parameters.get(PARAM_REPOSITORY);
			if (repositoryId == null) {
				log("No repository ID specified. When using the '"
						+ PARAM_URL
						+ "' parameter to specify a Sesame server, you must also use the '"
						+ PARAM_REPOSITORY
						+ "' parameter to specify a repository on that server.");
				System.exit(-5);
			}
			try {
				// Create a manager for the remote Sesame server and initialise
				// it
				repositoryManager = new RemoteRepositoryManager(url);
				repositoryManager.initialize();
			} catch (RepositoryException e) {
				log("Unable to establish a connection with the Sesame server '"
						+ url + "': " + e.getMessage());
				System.exit(-5);
			}
		}

		// Get the repository to use
		try {
			repository = repositoryManager.getRepository(repositoryId);

			if (repository == null) {
				log("Unknown repository '" + repositoryId + "'");
				String message = "Please make sure that the value of the '"
						+ PARAM_REPOSITORY + "' parameter (current value '"
						+ repositoryId + "') ";
				if (url == null) {
					message += "corresponds to the repository ID given in the configuration file identified by the '"
							+ PARAM_CONFIG
							+ "' parameter (current value '"
							+ parameters.get(PARAM_CONFIG) + "')";
				} else {
					message += "identifies an existing repository on the Sesame server located at "
							+ url;
				}
				log(message);
				System.exit(-6);
			}

			// Open a connection to this repository
			repositoryConnection = repository.getConnection();
			repositoryConnection.setAutoCommit(false);
		} catch (OpenRDFException e) {
			log("Unable to establish a connection to the repository '"
					+ repositoryId + "': " + e.getMessage());
			System.exit(-7);
		}
	}

	/**
	 * Parse the given RDF file and return the contents as a Graph
	 * 
	 * @param configurationFile
	 *            The file containing the RDF data
	 * @return The contents of the file as an RDF graph
	 * @throws RDFHandlerException
	 * @throws RDFParseException
	 * @throws IOException
	 */
	private Graph parseFile(File configurationFile, RDFFormat format,
			String defaultNamespace) throws RDFParseException,
			RDFHandlerException, IOException {
		Reader reader = new FileReader(configurationFile);

		final Graph graph = new GraphImpl();
		RDFParser parser = Rio.createParser(format);
		RDFHandler handler = new RDFHandler() {
			@Override
			public void endRDF() throws RDFHandlerException {
			}

			@Override
			public void handleComment(String arg0) throws RDFHandlerException {
			}

			@Override
			public void handleNamespace(String arg0, String arg1)
					throws RDFHandlerException {
			}

			@Override
			public void handleStatement(Statement statement)
					throws RDFHandlerException {
				graph.add(statement);
			}

			@Override
			public void startRDF() throws RDFHandlerException {
			}
		};
		parser.setRDFHandler(handler);
		parser.parse(reader, defaultNamespace);
		return graph;
	}

	/**
	 * Parses and loads all files specified in PARAM_PRELOAD
	 */
	public void loadFiles() throws Exception {
		log("===== Load Files (from the '" + PARAM_PRELOAD
				+ "' parameter) ==========");

		// Load all the files from the pre-load folder
		String preload = parameters.get(PARAM_PRELOAD);

		if (preload == null)
			log("No pre-load directory/filename provided.");
		else {
			FileWalker.Handler handler = new FileWalker.Handler() {

				@Override
				public void file(File file) throws Exception {
					loadFile(file);
				}

				@Override
				public void directory(File directory) throws Exception {
					log("Loading files from: " + directory.getAbsolutePath());
				}
			};

			FileWalker walker = new FileWalker();
			walker.setHandler(handler);
			walker.walk(new File(preload));
		}
	}

	/**
	 * Load an RDF file by trying to parse in all known formats.
	 * 
	 * @param file
	 *            The file to load in to the repository.
	 */
	private void loadFile(File file) throws RepositoryException, IOException {

		String contextParam = parameters.get(PARAM_CONTEXT);

		URI context = null;
		if (contextParam == null) {
			context = new URIImpl(file.toURI().toString());
		} else {
			if (contextParam.length() > 0) {
				context = new URIImpl(contextParam);
			}
		}

		boolean loaded = false;

		// Try all formats
		for (RDFFormat rdfFormat : allFormats) {
			Reader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file), 1024 * 1024);
				repositoryConnection.add(reader, "http://example.org/owlim#",
						rdfFormat, context);
				repositoryConnection.commit();
				log("Loaded file '" + file.getName() + "' ("
						+ rdfFormat.getName() + ").");
				loaded = true;
				break;
			} catch (UnsupportedRDFormatException e) {
				// Format not supported, so try the next format in the list.
			} catch (RDFParseException e) {
				// Can't parse the file, so it is probably in another format.
				// Try the next format.
			} finally {
				if (reader != null)
					reader.close();
			}
			if (!loaded)
				repositoryConnection.rollback();
		}
		if (!loaded)
			log("Failed to load '" + file.getName() + "'.");
	}

	// A list of RDF file formats used in loadFile().
	private static final RDFFormat allFormats[] = new RDFFormat[] {
			RDFFormat.NTRIPLES, RDFFormat.N3, RDFFormat.RDFXML,
			RDFFormat.TURTLE, RDFFormat.TRIG, RDFFormat.TRIX };

	private static RDFFormat stringToRDFFormat(String strFormat) {
		for (RDFFormat format : allFormats) {
			if (format.getName().equals(strFormat))
				return format;
		}
		return null;
	}

	private boolean isTrue(String parameter) {
		return parameters.get(parameter).equalsIgnoreCase("true");
	}

	/**
	 * Show some initialisation statistics
	 */
	public void showInitializationStatistics(long startupTime) throws Exception {

		if (isTrue(PARAM_SHOWSTATS)) {
			long explicitStatements = numberOfExplicitStatements();
			long implicitStatements = numberOfImplicitStatements();

			log("Loaded: " + explicitStatements + " explicit statements.");
			log("Inferred: " + implicitStatements + " implicit statements.");

			if (startupTime > 0) {
				double loadSpeed = explicitStatements / (startupTime / 1000.0);
				log(" in " + startupTime + "ms.");
				log("Loading speed: " + loadSpeed
						+ " explicit statements per second.");
			} else {
				log(" in less than 1 second.");
			}
			log("Total number of statements: "
					+ (explicitStatements + implicitStatements));
		}
	}

	/**
	 * Two approaches for finding the total number of explicit statements in a
	 * repository.
	 * 
	 * @return The number of explicit statements
	 */
	private long numberOfExplicitStatements() throws Exception {

		// This call should return the number of explicit statements.
		long explicitStatements = repositoryConnection.size();

		// Another approach is to get an iterator to the explicit statements
		// (by setting the includeInferred parameter to false) and then counting
		// them.
		RepositoryResult<Statement> statements = repositoryConnection
				.getStatements(null, null, null, false);
		explicitStatements = 0;

		while (statements.hasNext()) {
			statements.next();
			explicitStatements++;
		}
		statements.close();
		return explicitStatements;
	}

	/**
	 * A method to count only the inferred statements in the repository. No
	 * method for this is available through the Sesame API, so OWLIM uses a
	 * special context that is interpreted as instruction to retrieve only the
	 * implicit statements, i.e. not explicitly asserted in the repository.
	 * 
	 * @return The number of implicit statements.
	 */
	private long numberOfImplicitStatements() throws Exception {
		// Retrieve all inferred statements
		RepositoryResult<Statement> statements = repositoryConnection
				.getStatements(null, null, null, true, new URIImpl(
						"http://www.ontotext.com/implicit"));
		long implicitStatements = 0;

		while (statements.hasNext()) {
			statements.next();
			implicitStatements++;
		}
		statements.close();
		return implicitStatements;
	}

	/**
	 * Iterates and collects the list of the namespaces, used in URIs in the
	 * repository
	 */
	public void iterateNamespaces() throws Exception {
		log("===== Namespace List ==================================");

		log("Namespaces collected in the repository:");
		RepositoryResult<Namespace> iter = repositoryConnection.getNamespaces();

		while (iter.hasNext()) {
			Namespace namespace = iter.next();
			String prefix = namespace.getPrefix();
			String name = namespace.getName();
			namespacePrefixes.put(name, prefix);
			System.out.println(prefix + ":\t" + name);
		}
		iter.close();
	}

	/**
	 * Demonstrates query evaluation. First parse the query file. Each of the
	 * queries is executed against the prepared repository. If the printResults
	 * is set to true the actual values of the bindings are output to the
	 * console. We also count the time for evaluation and the number of results
	 * per query and output this information.
	 */
	public void evaluateQueries() throws Exception {
		log("===== Query Evaluation ======================");

		String queryFile = parameters.get(PARAM_QUERYFILE);
		if (queryFile == null) {
			log("No query file given in parameter '" + PARAM_QUERYFILE + "'.");
			return;
		}

		long startQueries = System.currentTimeMillis();

		// process the query file to get the queries
		String[] queries = collectQueries(queryFile);

		final CountDownLatch numberOfQueriesToProcess = new CountDownLatch(
				queries.length);
		// evaluate each query and, optionally, print the bindings
		for (int i = 0; i < queries.length; i++) {
			final String name = queries[i]
					.substring(0, queries[i].indexOf(":"));
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
					@Override
					public void run() {
						executeSingleQuery(query);
						numberOfQueriesToProcess.countDown();
					} // run
				}.start(); // thread

			} else {
				executeSingleQuery(query);
			}
		} // for
		if (doMultithreadQueryEvaluation)
			numberOfQueriesToProcess.await();

		long endQueries = System.currentTimeMillis();
		log("Queries run in " + (endQueries - startQueries) + " ms.");
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
	private Query prepareQuery(String query, QueryLanguage language)
			throws RepositoryException {
		Repository tempRepository = new SailRepository(new MemoryStore());
		tempRepository.initialize();

		RepositoryConnection tempConnection = tempRepository.getConnection();

		try {
			try {
				tempConnection.prepareTupleQuery(language, query);
				return repositoryConnection.prepareTupleQuery(language, query);
			} catch (Exception e) {
			}

			try {
				tempConnection.prepareBooleanQuery(language, query);
				return repositoryConnection
						.prepareBooleanQuery(language, query);
			} catch (Exception e) {
			}

			try {
				tempConnection.prepareGraphQuery(language, query);
				return repositoryConnection.prepareGraphQuery(language, query);
			} catch (Exception e) {
			}

			return null;
		} finally {
			try {
				tempConnection.close();
				tempRepository.shutDown();
			} catch (Exception e) {
			}
		}
	}

	private Query prepareQuery(String query) throws Exception {

		for (QueryLanguage language : queryLanguages) {
			Query result = prepareQuery(query, language);
			if (result != null)
				return result;
		}
		// Can't prepare this query in any language
		return null;
	}

	private static final QueryLanguage[] queryLanguages = new QueryLanguage[] {
			QueryLanguage.SPARQL, QueryLanguage.SERQL, QueryLanguage.SERQO };

	private void executeSingleQuery(String query) {
		try {
			Query preparedQuery = prepareQuery(query);
			if (preparedQuery == null) {
				log("Unable to parse query: " + query);
				return;
			}

			if (preparedQuery instanceof BooleanQuery) {
				log("Result: " + ((BooleanQuery) preparedQuery).evaluate());
				return;
			}

			if (preparedQuery instanceof GraphQuery) {
				GraphQuery q = (GraphQuery) preparedQuery;
				long queryBegin = System.nanoTime();

				GraphQueryResult result = q.evaluate();
				int rows = 0;
				while (result.hasNext()) {
					Statement statement = result.next();
					rows++;
					if (showResults) {
						System.out.print(beautifyRDFValue(statement
								.getSubject()));
						System.out.print(" "
								+ beautifyRDFValue(statement.getPredicate())
								+ " ");
						System.out
								.print(" "
										+ beautifyRDFValue(statement
												.getObject()) + " ");
						Resource context = statement.getContext();
						if (context != null)
							System.out.print(" " + beautifyRDFValue(context)
									+ " ");
						System.out.println();
					}
				}
				if (showResults)
					System.out.println();

				result.close();

				long queryEnd = System.nanoTime();
				log(rows + " result(s) in " + (queryEnd - queryBegin) / 1000000
						+ "ms.");
			}

			if (preparedQuery instanceof TupleQuery) {
				TupleQuery q = (TupleQuery) preparedQuery;
				long queryBegin = System.nanoTime();

				TupleQueryResult result = q.evaluate();

				int rows = 0;
				while (result.hasNext()) {
					BindingSet tuple = result.next();
					if (rows == 0) {
						for (Iterator<Binding> iter = tuple.iterator(); iter
								.hasNext();) {
							System.out.print(iter.next().getName());
							System.out.print("\t");
						}
						System.out.println();
						System.out
								.println("---------------------------------------------");
					}
					rows++;
					if (showResults) {
						for (Iterator<Binding> iter = tuple.iterator(); iter
								.hasNext();) {
							try {
								System.out.print(beautifyRDFValue(iter.next()
										.getValue()) + "\t");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						System.out.println();
					}
				}
				if (showResults)
					System.out.println();

				result.close();

				long queryEnd = System.nanoTime();
				log(rows + " result(s) in " + (queryEnd - queryBegin) / 1000000
						+ "ms.");
			}
		} catch (Throwable e) {
			log("An error occurred during query execution: " + e.getMessage());
		}
	}

	/**
	 * Creates a statement and adds it to the repository. Then deletes this
	 * statement and checks to make sure it is gone.
	 */
	public void insertAndDeleteStatement() throws Exception {
		if (isTrue(PARAM_UPDATES)) {
			log("===== Upload and Delete Statements ====================");

			// Add a statement directly to the SAIL
			log("----- Upload and check --------------------------------");
			// first, create the RDF nodes for the statement
			URI subj = repository.getValueFactory().createURI(
					"http://example.org/owlim#Pilot");
			URI pred = RDF.TYPE;
			URI obj = repository.getValueFactory().createURI(
					"http://example.org/owlim#Human");

			repositoryConnection.add(subj, pred, obj);
			repositoryConnection.commit();

			// Now check whether the new statement can be retrieved
			RepositoryResult<Statement> iter = repositoryConnection
					.getStatements(subj, null, obj, true);
			boolean retrieved = false;
			while (iter.hasNext()) {
				retrieved = true;
				System.out.println(beautifyStatement(iter.next()));
			}
			// CLOSE the iterator to avoid memory leaks
			iter.close();

			if (!retrieved)
				log("**** Failed to retrieve the statement that was just added. ****");

			// Remove the above statement in a separate transaction
			log("----- Remove and check --------------------------------");
			repositoryConnection.remove(subj, pred, obj);
			repositoryConnection.commit();

			// Check whether there is some statement matching the subject of the
			// deleted one
			iter = repositoryConnection.getStatements(subj, null, null, true);
			retrieved = false;
			while (iter.hasNext()) {
				retrieved = true;
				System.out.println(beautifyStatement(iter.next()));
			}
			// CLOSE the iterator to avoid memory leaks
			iter.close();
			if (retrieved)
				log("**** Statement was not deleted properly in last step. ****");
		}
	}

	/**
	 * Export the contents of the repository (explicit, implicit or all
	 * statements) to the given filename in the given RDF format,
	 */
	public void export() throws RepositoryException,
			UnsupportedRDFormatException, IOException, RDFHandlerException {
		String filename = parameters.get(PARAM_EXPORT_FILE);
		if (filename != null) {
			RDFFormat exportFormat;
			String strFormat = parameters.get(PARAM_EXPORT_FORMAT);
			if (strFormat == null)
				exportFormat = RDFFormat.NTRIPLES;
			else
				exportFormat = stringToRDFFormat(strFormat);

			if (exportFormat == null) {
				log("Unknown RDF format for export");
				return;
			}

			String type = parameters.get(PARAM_EXPORT_TYPE);

			RepositoryResult<Statement> statements;
			if (type == null || type.equalsIgnoreCase("explicit"))
				statements = repositoryConnection.getStatements(null, null,
						null, false);
			else if (type.equalsIgnoreCase("all"))
				statements = repositoryConnection.getStatements(null, null,
						null, true);
			else if (type.equalsIgnoreCase("implicit"))
				statements = repositoryConnection.getStatements(null, null,
						null, true, new URIImpl(
								"http://www.ontotext.com/implicit"));
			else {
				log("Unknown export type '" + type
						+ "' - valid values are: explicit, implicit, all");
				return;
			}

			RDFWriter writer = Rio.createWriter(exportFormat, new FileWriter(
					filename));

			writer.startRDF();

			RepositoryResult<Namespace> namespaces = repositoryConnection
					.getNamespaces();
			while (namespaces.hasNext()) {
				Namespace namespace = namespaces.next();
				writer.handleNamespace(namespace.getPrefix(),
						namespace.getName());
			}
			namespaces.close();

			while (statements.hasNext()) {
				Statement statement = statements.next();
				writer.handleStatement(statement);
			}
			statements.close();

			writer.endRDF();
		}
	}

	/**
	 * Shutdown the repository and flush unwritten data.
	 */
	public void shutdown() {
		log("===== Shutting down ==========");
		if (repository != null) {
			try {
				repositoryConnection.close();
				repository.shutDown();
				repositoryManager.shutDown();
			} catch (Exception e) {
				log("An exception occurred during shutdown: " + e.getMessage());
			}
		}
	}

	/**
	 * Auxiliary method, printing an RDF value in a "fancy" manner. In case of
	 * URI, qnames are printed for better readability
	 * 
	 * @param value
	 *            The value to beautify
	 */
	public String beautifyRDFValue(Value value) throws Exception {
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

	/**
	 * Auxiliary method, nicely format an RDF statement.
	 * 
	 * @param statement
	 *            The statement to be formatted.
	 * @return The beautified statement.
	 */
	public String beautifyStatement(Statement statement) throws Exception {
		return beautifyRDFValue(statement.getSubject()) + " "
				+ beautifyRDFValue(statement.getPredicate()) + " "
				+ beautifyRDFValue(statement.getObject());
	}

	/**
	 * Parse the query file and return the queries defined there for further
	 * evaluation. The file can contain several queries; each query starts with
	 * an id enclosed in square brackets '[' and ']' on a single line; the text
	 * in between two query ids is treated as a SeRQL query. Each line starting
	 * with a '#' symbol will be considered as a single-line comment and
	 * ignored. Query file syntax example:
	 * 
	 * #some comment [queryid1] <query line1> <query line2> ... <query linen>
	 * #some other comment [nextqueryid] <query line1> ... <EOF>
	 * 
	 * @param queryFile
	 * @return an array of strings containing the queries. Each string starts
	 *         with the query id followed by ':', then the actual query string
	 */
	private static String[] collectQueries(String queryFile) throws Exception {
		try {
			List<String> queries = new ArrayList<String>();
			BufferedReader input = new BufferedReader(new FileReader(queryFile));
			String nextLine = null;

			for (;;) {
				String line = nextLine;
				nextLine = null;
				if (line == null) {
					line = input.readLine();
				}
				if (line == null) {
					break;
				}
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				if (line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("^[") && line.endsWith("]")) {
					StringBuffer buff = new StringBuffer(line.substring(2,
							line.length() - 1));
					buff.append(": ");

					for (;;) {
						line = input.readLine();
						if (line == null) {
							break;
						}
						line = line.trim();
						if (line.length() == 0) {
							continue;
						}
						if (line.startsWith("#")) {
							continue;
						}
						if (line.startsWith("^[")) {
							nextLine = line;
							break;
						}
						buff.append(line);
						buff.append(System.getProperty("line.separator"));
					}

					queries.add(buff.toString());
				}
			}

			String[] result = new String[queries.size()];
			for (int i = 0; i < queries.size(); i++) {
				result[i] = queries.get(i);
			}
			input.close();
			return result;
		} catch (Exception e) {
			log("Unable to load query file '" + queryFile + "':" + e);
			return new String[0];
		}
	}

	private static SimpleDateFormat logTimestamp = new SimpleDateFormat(
			"HH:mm:ss ");

	private static void log(String message) {
		System.out.println(logTimestamp.format(new Date()) + message);
	}

	/**
	 * This is the entry point of the example application. First, the
	 * command-line parameters are intialised. Then these parameters are passed
	 * to an instance of the GettingStarted application and used to create,
	 * initialise and login to the local instance of Sesame.
	 * 
	 * @param args
	 *            Command line parameters
	 */
	public static void main(String[] args) {

		// Special handling for JAXP XML parser that limits entity expansion
		// see
		// http://java.sun.com/j2se/1.5.0/docs/guide/xml/jaxp/JAXP-Compatibility_150.html#JAXP_security
		System.setProperty("entityExpansionLimit", "1000000");

		final String PREFIX = "src/test/resources/";

		// Parse all the parameters
		Parameters params = new Parameters(args);

		// Set default values for missing parameters
		params.setDefaultValue(PARAM_CONFIG, PREFIX + "owlim.ttl");
		params.setDefaultValue(PARAM_SHOWRESULTS, "true");
		params.setDefaultValue(PARAM_SHOWSTATS, "false");
		params.setDefaultValue(PARAM_UPDATES, "false");
		params.setDefaultValue(PARAM_PRELOAD, PREFIX + "preload");
		params.setDefaultValue(PARAM_QUERYFILE, PREFIX + "queries/sample.sparql");

		log("Using parameters:");
		log(params.toString());

		GettingStarted gettingStartedApplication = null;
		try {
			long initializationStart = System.currentTimeMillis();
			// The ontologies and datasets specified in the 'import' parameter
			// of the Sesame configuration file are loaded during
			// initialization.
			// Thus, for large datasets the initialisation could take
			// considerable time.
			gettingStartedApplication = new GettingStarted(
					params.getParameters());

			// Demonstrate the basic operations on a repository
			gettingStartedApplication.loadFiles();
			gettingStartedApplication.showInitializationStatistics(System
					.currentTimeMillis() - initializationStart);
//			gettingStartedApplication.iterateNamespaces();
//			gettingStartedApplication.evaluateQueries();
			gettingStartedApplication.insertAndDeleteStatement();
//			gettingStartedApplication.export();
		} catch (Throwable ex) {
			log("An exception occured at some point during execution:");
			ex.printStackTrace();
		} finally {
			if (gettingStartedApplication != null)
				gettingStartedApplication.shutdown();
		}
	}


	/**
	 * Utility for a depth first traversal of a file-system starting from a
	 * given node (file or directory).
	 */
	public static class FileWalker {

		/**
		 * The call back interface for traversal.
		 */
		public interface Handler {
			/**
			 * Called to notify that a normal file has been encountered.
			 * 
			 * @param file
			 *            The file encountered.
			 */
			void file(File file) throws Exception;

			/**
			 * Called to notify that a directory has been encountered.
			 * 
			 * @param directory
			 *            The directory encountered.
			 */
			void directory(File directory) throws Exception;
		}

		/**
		 * Set the notification handler.
		 * 
		 * @param handler
		 *            The object that receives notifications of encountered
		 *            nodes.
		 */
		public void setHandler(Handler handler) {
			this.handler = handler;
		}

		/**
		 * Start the walk at the given location, which can be a file, for a very
		 * short walk, or a directory which will be traversed recursively.
		 * 
		 * @param node
		 *            The starting point for the walk.
		 */
		public void walk(File node) throws Exception {
			if (node.isDirectory()) {
				handler.directory(node);
				File[] children = node.listFiles();
				Arrays.sort(children, new Comparator<File>() {

					@Override
					public int compare(File lhs, File rhs) {
						return lhs.getName().compareTo(rhs.getName());
					}

				});
				for (File child : children) {
					walk(child);
				}
			} else {
				handler.file(node);
			}
		}

		private Handler handler;
	}
}
