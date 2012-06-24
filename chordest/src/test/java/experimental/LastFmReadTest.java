package experimental;
import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import similarity.lastfm.Track;


public class LastFmReadTest {

	private final static String FILENAME = "src/main/resources/TRAAAAW128F429D538.json";

	public static void main(String[] args) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Track track = mapper.readValue(new File(FILENAME), Track.class);
			System.out.println("Artist: " + track.artist);
			System.out.println("Timestamp: " + track.timestamp);
			System.out.println("Title: " + track.title);
			System.out.println("track_id: " + track.track_id);
			System.out.println("Similars:");
			for (String[] similar : track.similars) {
				System.out.println("    " + similar[0] + " " + similar[1]);
			}
			System.out.println("Tags:");
			for (String[] tag : track.tags) {
				System.out.println("    " + tag[0] + " " + tag[1]);
			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
