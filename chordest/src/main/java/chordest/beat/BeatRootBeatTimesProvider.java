package chordest.beat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ofai.music.beatroot.AudioPlayer;
import at.ofai.music.beatroot.AudioProcessor;
import at.ofai.music.beatroot.BeatTrackDisplay;
import at.ofai.music.beatroot.GUI;
import at.ofai.music.util.EventList;
import chordest.wave.WaveFileInfo;

/**
 * A class that runs Beatroot to obtain beat sequence for a given wave file
 * @author Nikolay
 *
 */
public class BeatRootBeatTimesProvider implements IBeatTimesProvider {

	private static Logger LOG = LoggerFactory.getLogger(BeatRootBeatTimesProvider.class);

	private final double bpm;
	private final double[] beatTimes;
	private double[] correctedBeatTimes = null;
	private double epsilon;
	private double step;

	private class AudioProcessor1 extends AudioProcessor {
		public EventList getOnsetList() {
			return this.onsetList;
		}
	}

	public static double getMeanBeatLengthInSeconds(double[] beats) {
		double sum = beats[beats.length-1] - beats[0];
		return sum / (beats.length - 1);
	}

	public BeatRootBeatTimesProvider(String wavFilePath) {
		LOG.info("Performing beat detection for " + wavFilePath + " ...");
		this.beatTimes = findBeats(wavFilePath);
		double mean = getMeanBeatLengthInSeconds(this.beatTimes);
		this.bpm = 60 / mean;
	}

	private double[] findBeats(String wavFilePath) {
		AudioProcessor1 audioProcessor = new AudioProcessor1();
		audioProcessor.setInputFile(wavFilePath);
		audioProcessor.processFile();
		EventList onsetList = audioProcessor.getOnsetList();
		AudioPlayer player = new AudioPlayer(null, null);
		GUI gui = new GUI(player, audioProcessor, null);
		BeatTrackDisplay beatTrackDisplay = new BeatTrackDisplay(gui, new EventList());
		beatTrackDisplay.setOnsetList(onsetList);
		beatTrackDisplay.beatTrack();
		EventList beats = gui.getBeatData();
		gui.dispose();
		audioProcessor.closeStreams();
		double[] result = beats.toOnsetArray();
//		if (result.length == 0) {
//			result = generateDefaultBeats(wavFilePath);
//		}
		return result;
	}

	public static double[] generateTimeScale(String wavFilePath, double step) {
		LOG.warn("Beat detection error, generating a dummy sequence of beats");
		WaveFileInfo wfi = new WaveFileInfo(wavFilePath);
		double totalSeconds = wfi.seconds;
		int length = (int) (Math.floor(totalSeconds / step));
		double[] result = new double[length];
		for (int i = 0; i < length; i++) {
			result[i] = step * i;
		}
		return result;
	}

	public double getBPM() {
		return this.bpm;
	}

	@Override
	public double[] getBeatTimes() {
		return this.beatTimes;
	}

	/**
	 * Хотим по расстояниям между битами построить равномерную сетку, наиболее близкую к битам.
	 * То есть чтобы сумма квадратов расстояний от каждого бита до ближайшего узла сетки была
	 * минимальной. Для этого сначала ищем матожидание расстояний между битами, предполагая, что
	 * выдаваемые BeatRoot'ом расстояния имеют нормальное распределение. Это матожидание берем
	 * за шаг сетки и ищем, при каком сдвиге 1-й точки сетки относительно 1-го бита минимизируется
	 * сумма квадратов расстояний между узлами сетки и битами
	 * @return
	 */
	public double[] getCorrectedBeatTimes() {
		if (this.correctedBeatTimes == null) {
			double sumk = 0;
			double sumk2 = 0;
			double sumyk = 0;
			double sumkyk = 0;
			int n = beatTimes.length - 1;
			double y0 = beatTimes[0];
			for (int i = 0; i < n; i++) {
				double yk = beatTimes[i];
				double k = i;
				sumk += k;
				sumk2 += k*k;
				sumyk += yk;
				sumkyk += (k*yk);
			}
			step = (sumkyk - sumk*sumyk/n) / (sumk2 - sumk*sumk/n);
			epsilon = sumyk/n - y0 - sumk*step/n;

			double[] result = buildSequence(step, epsilon);
			this.correctedBeatTimes = result;
		}
		return this.correctedBeatTimes;
	}

	private double[] buildSequence(double h, double eps) {
		double firstGridNode = beatTimes[0] + eps;
		if (firstGridNode < 0) { firstGridNode += h; }
		int stepsFromStart = (int)Math.floor(firstGridNode / h);
		double start = firstGridNode - stepsFromStart * h;
		int totalSteps = (int)Math.floor((beatTimes[beatTimes.length - 1] + h - start) / h);
		double[] result = new double[totalSteps + 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = i*h + start;
		}
		return result;
	}

	public double getEpsilon() {
		return this.epsilon;
	}

	public double getStep() {
		return this.step;
	}

}
