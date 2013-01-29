package utils;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import chordest.configuration.Configuration.SpectrumProperties;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.io.spectrum.SpectrumFileWriter;
import chordest.spectrum.SpectrumData;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.transform.ScaleInfo;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class SpectrumAppender {

	public static void main(String[] args) {
		SpectrumAppender sa = new SpectrumAppender();
		sa.process();
	}

	public void process() {
		List<String> tracklist = TracklistCreator.readTrackList("work" + PathConstants.SEP + "all_wav.txt");
		for (String wavFileName : tracklist) {
			String binFileName = PathConstants.SPECTRUM_DIR + new File(wavFileName).getName() + PathConstants.EXT_BIN;
			String newBinFileName = "spectrum8-5" + PathConstants.SEP + new File(wavFileName).getName() + PathConstants.EXT_BIN;
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] source = sd.spectrum;
			
			SpectrumProperties s = new SpectrumProperties(
					1, // add 1 octave
					sd.scaleInfo.getNotesInOctaveCount(),
					sd.startNoteOffsetInSemitonesFromF0 + sd.scaleInfo.getOctavesCount() * 12, // at the end
					sd.framesPerBeat,
					4); // threadpool size
			WaveFileSpectrumDataProvider p = new WaveFileSpectrumDataProvider(wavFileName, s, sd.beatTimes);
			double[][] addition = p.getSpectrumData().spectrum;
			if (source.length != addition.length) {
				throw new RuntimeException("source.length != addition.length: " + source.length + ", " + addition.length);
			}
			double[][] result = new double[source.length][];
			for (int i = 0; i < source.length; i++) {
				result[i] = ArrayUtils.addAll(source[i], addition[i]);
			}
			sd.spectrum = result;
			sd.scaleInfo = new ScaleInfo(sd.scaleInfo.getOctavesCount() + 1, sd.scaleInfo.getNotesInOctaveCount());
			SpectrumFileWriter.write(newBinFileName, sd);
		}
	}

}
