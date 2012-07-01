package msdchallenge.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import msdchallenge.input.UsersFileReader;
import msdchallenge.input.UsersFileReader.IUserProcessor;
import msdchallenge.query.QueryCollector;
import msdchallenge.query.QueryEvaluator;
import msdchallenge.repository.IRepositoryWrapper;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Processor implements IUserProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(Processor.class);
	
	private static final String NS = "http://example.org/owlim#";
	private static final String RESULT_FILE_NAME = "result/result.txt";

	private final String similarUsersQuery;
	private final String trackNumberQuery;
	private final String userTracksQuery;

	private final QueryEvaluator evaluator;

	private final FileWriter fw;
	private final PrintWriter pw;

	private int processedUsers = 0;

	private static String readQuery(String fileName) {
		String query = QueryCollector.collectQueries(fileName)[0];
		return query.substring(query.indexOf(": ") + 2).trim();
	}

	public Processor(IRepositoryWrapper wrapper) {
		this.evaluator = new QueryEvaluator(wrapper.getRepositoryConnection(), false);
		this.similarUsersQuery = readQuery("query_templates/similar_users.sparql");
		this.trackNumberQuery = readQuery("query_templates/track_number.sparql");
		this.userTracksQuery = readQuery("query_templates/user_tracks.sparql");
		try {
			fw = new FileWriter(RESULT_FILE_NAME);
			pw = new PrintWriter(fw);
		} catch (IOException e) {
			LOG.error("Error when opening file " + RESULT_FILE_NAME, e);
			throw new IllegalArgumentException("Error opening file " + RESULT_FILE_NAME);
		}
	}

	public void doWork() {
		try {
			File users = new File("kaggle/kaggle_users.txt");
			UsersFileReader.process(users, this);
		} catch (Exception e) {
			try {
				fw.close();
			} catch (IOException ignore) { }
			pw.close();
		}
	}

	@Override
	public void process(String kaggleUser) {
		String userId = NS + kaggleUser;
		List<String> tracks = getListOfTracks(userId);
		List<String> similarUsers = getUsersWith2CommonTracks(userId);
		Set<String> similarTracks = getUsersTracks(similarUsers);
		similarTracks.removeAll(tracks);
		writeResultLine(similarTracks);
		if (++processedUsers % 100 == 0) {
			LOG.info(processedUsers + " users processed");
		}
	}

	private List<String> getListOfTracks(String kaggleUser) {
		String query = userTracksQuery.replace("{0}", kaggleUser);
		List<String> list = new ArrayList<String>();
		TupleQueryResult result = (TupleQueryResult) evaluator.executeSingleQuery(query);
		if (result != null) {
			try {
				while (result.hasNext()) {
					BindingSet set = result.next();
					Value value = set.getValue("track");
					list.add(value.toString());
				}
			} catch (QueryEvaluationException e) {
				LOG.error("Error during query evaluation", e);
			} finally {
				try {
					result.close();
				} catch (QueryEvaluationException e) {
					LOG.error("Error during query result close", e);
				}
			}
		}
		return list;
	}

	private List<String> getUsersWith2CommonTracks(String kaggleUser) {
		String query = similarUsersQuery.replace("{0}", kaggleUser);
		List<String> list = new ArrayList<String>();
		TupleQueryResult result = (TupleQueryResult) evaluator.executeSingleQuery(query);
		if (result != null) {
			try {
				while (result.hasNext()) {
					BindingSet set = result.next();
					Value value = set.getValue("user");
					list.add(value.toString());
				}
			} catch (QueryEvaluationException e) {
				LOG.error("Error during query evaluation", e);
			} finally {
				try {
					result.close();
				} catch (QueryEvaluationException e) {
					LOG.error("Error during query result close", e);
				}
			}
		}
		return list;
	}

	private Set<String> getUsersTracks(List<String> users) {
		Set<String> result = new HashSet<String>();
		for (String user : users) {
			List<String> tracks = getListOfTracks(user);
			result.addAll(tracks);
		}
		return result;
	}

	private int getTrackNumber(String trackId) {
		String query = trackNumberQuery.replace("{0}", trackId);
		TupleQueryResult result = (TupleQueryResult) evaluator.executeSingleQuery(query);
		if (result != null) {
			try {
				if (result.hasNext()) {
					BindingSet set = result.next();
					Value value = set.getValue("num");
					return ((Literal) value).intValue();
				}
			} catch (QueryEvaluationException e) {
				LOG.error("Error during query evaluation", e);
			} finally {
				try {
					result.close();
				} catch (QueryEvaluationException e) {
					LOG.error("Error during query result close", e);
				}
			}
		}
		return -1;
	}

	private void writeResultLine(Set<String> tracks) {
		StringBuilder sb = new StringBuilder();
		for (String track : tracks) {
			sb.append(getTrackNumber(track));
			sb.append(' ');
		}
		pw.println(sb.toString());
		pw.flush();
	}

}
