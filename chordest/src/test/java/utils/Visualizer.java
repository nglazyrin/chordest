package utils;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import similarity.chord.Chord;
import similarity.chord.Note;
import similarity.gui.JFreeChartUtils;
import similarity.transform.ScaleInfo;
import similarity.util.DatasetUtil;
import similarity.util.NoteLabelProvider;

public class Visualizer {

	private static final Logger LOG = LoggerFactory.getLogger(Visualizer.class);

	private Visualizer() { }

	public static final void visualizeChords(Chord[] chords, double[] beatTimes, String filename, int startNoteOffset) {
		try {
			XYZDataset cs = DatasetUtil.toXYZDataset(
					Arrays.copyOfRange(beatTimes, 0, chords.length),
					chords, Note.byNumber(startNoteOffset));
			JFreeChartUtils.visualizeStringY("Chords for " + filename, "Time", "Notes", cs, 1000, 400,
					NoteLabelProvider.getNoteLabels(startNoteOffset, new ScaleInfo(1,12)));
		} catch (InterruptedException e) {
			LOG.error("Error when creating dataset", e);
		} catch (ExecutionException e) {
			LOG.error("Error when creating dataset", e);
		}
	}

	public static final void visualizeSpectrum(double[][] spectrum, double[] beatTimes, String[] labels, String title) {
		try {
			XYZDataset d3 = DatasetUtil.toXYZDataset(beatTimes, labels, spectrum);
			JFreeChartUtils.visualizeStringY(title, "Time", "Frequency", d3, 1000, 400, labels);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public static final void visualizeSpectrumEnergy(double[] energy, double[] beatTimes) {
		XYDataset ds = DatasetUtil.toXYDataset(beatTimes, energy);
		JFreeChartUtils.visualize("Energy", "Time", "energy", ds);
	}

	public static final void visualizeSelfSimilarity(double[][] matrix, double[] beatTimes) {
		try {
			XYZDataset ds = DatasetUtil.toXYZDataset(beatTimes, matrix);
			JFreeChartUtils.visualize("Self-similarity", "Time", "Time", ds, 800, 800);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static final void visualizeXByFrequencyDistribution(double[] array, ScaleInfo scaleInfo, int startNoteOffset) {
		XYDataset ds = DatasetUtil.toXYDataset(array);
		JFreeChartUtils.visualizeStringY("Energy by frequency distribution", "Energy", 
				"Frequency", ds, 1000, 800, NoteLabelProvider.getNoteLabels(startNoteOffset, scaleInfo));
	}

}
