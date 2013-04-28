package chordest.main.experimental;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.io.spectrum.SpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.spectrum.SpectrumData;
import chordest.util.TracklistCreator;
import chordest.util.Viterbi;

public class ChordTransitionStats {

	private static final Logger LOG = LoggerFactory.getLogger(ChordTransitionStats.class);

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList(TrainDataGenerator.TRAIN_FILE_LIST);
		int filesProcessed = 0;
		ChordTransitionStats cts = new ChordTransitionStats();
		for (final String binFileName : tracklist) {
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			Chord[] chords = TrainDataGenerator.prepareChords(binFileName, sd, 0);
			cts.process(chords);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info("Done. " + tracklist.size() + " files were processed.");
		cts.printResult();
	}

	private int[] accMajor = new int[Viterbi.totalChords];
	private int[] accMinor = new int[Viterbi.totalChords];

	private void process(Chord[] chords) {
		if (chords == null) {
			LOG.error("chords is null");
			return;
		}
		for (int i = 0; i < chords.length - 1; i++) {
			Chord c1 = chords[i];
			Chord c2 = chords[i+1];
			if (c1 != null && c2 != null) {
				if ((c1.isMajor() || c1.isMinor()) && (c2.isMajor() || c2.isMinor())) {
					Note c1r = c1.getRoot();
					Note c2r = c2.getRoot();
					int offset = c2r.offsetFrom(c1r);
					if (offset < 0) { offset += 12; }
					if (c2.isMinor()) { offset += 12; }
					if (c1.isMajor()) {
						accMajor[offset]++;
					} else {
						accMinor[offset]++;
					}
				}
			}
		}
	}

	private void printResult() {
		LOG.info(Arrays.toString(accMajor));
		LOG.info(Arrays.toString(accMinor));
	}

}
