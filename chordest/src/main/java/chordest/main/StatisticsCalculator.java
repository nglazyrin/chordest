package chordest.main;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import chordest.chord.comparison.IEvaluationMetric;
import chordest.chord.comparison.Tetrads;
import chordest.chord.comparison.Triads;
import chordest.io.lab.LabFileReader;
import chordest.model.Chord;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class StatisticsCalculator {

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.createTracklist(
				new File(PathConstants.LAB_DIR), PathConstants.LAB_DIR, PathConstants.EXT_LAB);
		IEvaluationMetric[] metrics = new IEvaluationMetric[] { new Triads(), new Tetrads() };
		String[][] chordTypes = new String[metrics.length][];
		double[][] totalTimes = new double[metrics.length][];
		double[] totalLengths = new double[metrics.length];
		for (int i = 0; i < metrics.length; i++) {
			chordTypes[i] = metrics[i].getOutputTypes();
			totalTimes[i] = new double[chordTypes[i].length + 2];
		}
		double totalLength = 0;
		for (String labFileName : tracklist) {
			LabFileReader reader = new LabFileReader(new File(labFileName));
			Chord[] chords = reader.getChords();
			double[] timestamps = reader.getTimestamps();
			for (int i = 0; i < chords.length; i++) {
				Chord chord = chords[i];
				double length = timestamps[i+1] - timestamps[i];
				totalLength += length;
				for (int m = 0; m < metrics.length; m++) {
					if (chord == null || chord.isEmpty()) {
						totalTimes[m][totalTimes[m].length - 2] += length;
					} else if (metrics[m].map(chord) == null) {
						totalTimes[m][totalTimes[m].length - 1] += length;
					} else {
						boolean found = false;
						for (int j = 0; j < chordTypes[m].length; j++) {
							if (chord.isOfType(chordTypes[m][j]) || metrics[m].map(chord).isOfType(chordTypes[m][j])) {
								totalTimes[m][j] += length;
								found = true;
							}
						}
						if (! found) {
							totalTimes[m][totalTimes.length - 1] += length;
						}
					}
				}
			}
		}
		System.out.println(String.format("Total length: %.2f s", totalLength));
		for (int i = 0; i < metrics.length; i++) {
			totalLengths[i] = totalLength - totalTimes[i][totalTimes[i].length - 1];
			System.out.println(String.format("\r\n%s\r\nEffective length: %.2f s", metrics[i], totalLengths[i]));
			chordTypes[i] = ArrayUtils.addAll(chordTypes[i], new String[] { "N", "Others" });
			for (int j = 0; j < totalTimes[i].length - 1; j++) {
				System.out.println(String.format("%s - %f s, %,.2f%%", chordTypes[i][j], totalTimes[i][j], 100 * totalTimes[i][j] / totalLengths[i]));
			}
			System.out.println(String.format("%s - %f s, %,.2f%%", chordTypes[i][totalTimes[i].length - 1], totalTimes[i][totalTimes[i].length - 1], 0.0));
		}
	}

}
