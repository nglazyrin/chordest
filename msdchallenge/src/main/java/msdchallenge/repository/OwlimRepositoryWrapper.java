package msdchallenge.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OwlimRepositoryWrapper implements IRepositoryWrapper {

	private static final Logger LOG = LoggerFactory.getLogger(OwlimRepositoryWrapper.class);

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
//	private final Map<String, String> parameters;

	private RepositoryManager repositoryManager;

	// From repositoryManager.getRepository(...) - the actual repository we will work with
	private Repository repository;

	// From repository.getConnection() - the connection through which we will use the repository
	private RepositoryConnection repositoryConnection;

	private static void log(String message) {
		LOG.info(message);
	}

	public OwlimRepositoryWrapper(Map<String, String> parameters) {
		// Special handling for JAXP XML parser that limits entity expansion
		// see
		// http://java.sun.com/j2se/1.5.0/docs/guide/xml/jaxp/JAXP-Compatibility_150.html#JAXP_security
		System.setProperty("entityExpansionLimit", "1000000");
		
//		this.parameters = parameters;
		log("===== Initialize and load imported ontologies =========");

		// The configuration file
		final String configFilename = parameters.get(PARAM_CONFIG);
		log("Using configuration file: " + configFilename);
		Graph repositoryRdfDescription = readRepositoryDescription(configFilename);

		// Look for the subject of the first matching statement for "?s type Repository"
		final String repositoryUri = "http://www.openrdf.org/config/repository#Repository";
		final String repositoryIdUri = "http://www.openrdf.org/config/repository#repositoryID";
		Resource repositoryNode = getRepositoryNode(repositoryRdfDescription, repositoryUri);

		String repositoryId = getRepositoryId(repositoryRdfDescription,
				repositoryUri, repositoryIdUri, repositoryNode);

		initializeRepositoryManager(repositoryRdfDescription, repositoryNode);

		initializeRepositoryAndConnection(parameters, repositoryId);
	}

	private void initializeRepositoryAndConnection(Map<String, String> parameters, String repositoryId) {
		// Get the repository to use
		try {
			repository = repositoryManager.getRepository(repositoryId);
			if (repository == null) {
				log("Unknown repository '" + repositoryId + "'");
				String message = "Please make sure that the value of the '"
						+ PARAM_REPOSITORY + "' parameter (current value '" + repositoryId + "') ";
				message += "corresponds to the repository ID given in the configuration file identified by the '"
						+ PARAM_CONFIG + "' parameter (current value '"
						+ parameters.get(PARAM_CONFIG) + "')";
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

	private void initializeRepositoryManager(Graph repositoryRdfDescription,
			Resource repositoryNode) {
		try {
			// Create a manager for local repositories and initialize it
			repositoryManager = new LocalRepositoryManager(new File("."));
			repositoryManager.initialize();
		} catch (RepositoryException e) {
			log("");
			System.exit(-3);
		}
		try {
			// Create a configuration object from the configuration file and
			// add it to the repositoryManager
			RepositoryConfig repositoryConfig = RepositoryConfig.create(
					repositoryRdfDescription, repositoryNode);
			repositoryManager.addRepositoryConfig(repositoryConfig);
		} catch (OpenRDFException e) {
			log("Unable to process the repository configuration: " + e.getMessage());
			System.exit(-4);
		}
	}

	private String getRepositoryId(Graph repositoryRdfDescription,
			String repositoryUri, String repositoryIdUri, Resource repositoryNode) {
		String result = null;
		// Get the repository ID (and ignore the one passed with the 'repository' parameter
		Iterator<Statement> iter = repositoryRdfDescription.match(repositoryNode, new URIImpl(repositoryIdUri), null);
		if (iter.hasNext()) {
			Statement st = iter.next();
			result = st.getObject().stringValue();
		} else {
			log("The turtle configuration file does not contain a" +
					" valid repository description, because it is missing a <"
					+ repositoryUri + "> with a property <" + repositoryIdUri + ">");
			System.exit(-2);
		}
		return result;
	}

	private Resource getRepositoryNode(Graph repositoryRdfDescription,
			final String repositoryUri) {
		Iterator<Statement> iter = repositoryRdfDescription.match(null, RDF.TYPE, new URIImpl(repositoryUri));
		Resource repositoryNode = null;
		if (iter.hasNext()) {
			Statement st = iter.next();
			repositoryNode = st.getSubject();
		}
		if (repositoryNode == null) {
			log("The turtle configuration file does not contain a" +
					" valid repository description, because it is missing" +
					" a resource with rdf:type <" + repositoryUri + ">");
			System.exit(-2);
		}
		return repositoryNode;
	}

	private Graph readRepositoryDescription(String configFilename) {
		File configFile = new File(configFilename);
		// Parse the configuration file, assuming it is in Turtle format
		Graph repositoryRdfDescription = null;
		try {
			repositoryRdfDescription = parseRdfFile(configFile,
					RDFFormat.TURTLE, "http://example.org#");
		} catch (OpenRDFException e) {
			log("There was an error reading/parsing the Turtle configuration file '"
					+ configFilename + "': " + e.getMessage());
		} catch (FileNotFoundException e) {
			log("The turtle configuration file '" + configFilename
					+ "' was not found, please check the '" + PARAM_CONFIG + "' parameter");
		} catch (IOException e) {
			log("An I/O error occurred while processing the configuration file '"
					+ configFilename + "': " + e.getMessage());
		}
		if (repositoryRdfDescription == null) {
			System.exit(-1);
		}
		return repositoryRdfDescription;
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
	private Graph parseRdfFile(File configurationFile, RDFFormat format, String defaultNamespace)
			throws RDFParseException, RDFHandlerException, IOException {
		Reader reader = new FileReader(configurationFile);

		final Graph graph = new GraphImpl();
		RDFParser parser = Rio.createParser(format);
		RDFHandler handler = new RDFHandler() {
			@Override public void endRDF() throws RDFHandlerException { }
			@Override public void handleComment(String arg0) throws RDFHandlerException { }
			@Override public void handleNamespace(String arg0, String arg1) throws RDFHandlerException { }

			@Override
			public void handleStatement(Statement statement) throws RDFHandlerException {
				graph.add(statement);
			}

			@Override public void startRDF() throws RDFHandlerException { }
		};
		parser.setRDFHandler(handler);
		parser.parse(reader, defaultNamespace);
		return graph;
	}

	/* (non-Javadoc)
	 * @see ru.msdchallenge.repository.IRepositoryWrapper#getRepository()
	 */
	@Override
	public Repository getRepository() {
		return repository;
	}

	/* (non-Javadoc)
	 * @see ru.msdchallenge.repository.IRepositoryWrapper#getRepositoryConnection()
	 */
	@Override
	public RepositoryConnection getRepositoryConnection() {
		return repositoryConnection;
	}

	/* (non-Javadoc)
	 * @see ru.msdchallenge.repository.IRepositoryWrapper#shutdown()
	 */
	@Override
	public void shutdown() {
		try {
			repositoryConnection.close();
			log("repository connection closed");
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			repository.shutDown();
			log("repository shut down");
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		repositoryManager.shutDown();
	}

}
