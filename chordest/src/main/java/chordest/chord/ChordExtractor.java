package chordest.chord;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.labbookpages.WavFile;
import uk.co.labbookpages.WavFileException;
import chordest.beat.BeatRootAdapter;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.properties.Configuration;
import chordest.spectrum.SpectrumData;
import chordest.spectrum.SpectrumFileReader;
import chordest.transform.CQConstants;
import chordest.transform.DiscreteCosineTransform;
import chordest.transform.PooledTransformer;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.TuningFrequencyFinder;
import chordest.wave.WaveReader;

/**
 * This class incapsulates all the chord extraction logic. All you need is to
 * pass a name of the wave file as a constructor parameter. When the
 * constructor finishes, all the extracted information is available. It
 * includes the positions of all the beats in the file (extracted with
 * BeatRoot), the positions that were used for recognition (which are
 * essentially the original beat positions supplemented with intermediate
 * points), the array of recognized chords, the recognized mode and the
 * resulting spectrum as a double[][]. 
 * @author Nikolay
 *
 */
public class ChordExtractor {

	private static final Logger LOG = LoggerFactory.getLogger(ChordExtractor.class);
	
	private final double[] originalBeatTimes;
	private final double[] expandedBeatTimes;
	private final double totalSeconds;
	private final int startNoteOffsetInSemitonesFromF0;
	private final SpectrumData spectrum;
	private Chord[] chords;
	private Key key;

	public ChordExtractor(Configuration c, String wavFilePath, BeatRootAdapter beatRoot) {
		originalBeatTimes = beatRoot.getBeatTimes();
		expandedBeatTimes = DataUtil.makeMoreFrequent(originalBeatTimes, c.spectrum.framesPerBeat);
		LOG.debug("Transforms: " + expandedBeatTimes.length);
		
		spectrum = readSpectrum(c, wavFilePath, expandedBeatTimes);
		totalSeconds = spectrum.totalSeconds;
		startNoteOffsetInSemitonesFromF0 = spectrum.startNoteOffsetInSemitonesFromF0;
		doChordExtraction(c);
	}

	public ChordExtractor(Configuration c, String spectrumFilePath) {
		spectrum = readSpectrum(spectrumFilePath);
		totalSeconds = spectrum.totalSeconds;
		startNoteOffsetInSemitonesFromF0 = spectrum.startNoteOffsetInSemitonesFromF0;
		expandedBeatTimes = restoreBeatTimes();
		
		int framesPerBeat = c.spectrum.framesPerBeat;
		originalBeatTimes = new double[expandedBeatTimes.length / framesPerBeat + 1];
		for (int i = 0; i < originalBeatTimes.length; i++) {
			originalBeatTimes[i] = expandedBeatTimes[framesPerBeat * i];
		}
		
		doChordExtraction(c);
	}

	/**
	 * Beat times in spectrum are really the start positions of constant-Q
	 * transform analysis windows. But those windows are centered at the
	 * real beat time positions, which we need to restore. So we add half of
	 * the longest constant-Q window to each position
	 * @return
	 */
	private double[] restoreBeatTimes() {
		double[] result = new double[spectrum.beatTimes.length];
		double shift = getWindowsShift(spectrum);
		for (int i = 0; i < result.length; i++) {
			result[i] = spectrum.beatTimes[i] + shift;
		}
		return result;
	}

	private double getWindowsShift(SpectrumData data) {
		CQConstants cqConstants = CQConstants.getInstance(data.samplingRate,
				data.scaleInfo, data.f0, data.startNoteOffsetInSemitonesFromF0);
		int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
		double shift = windowSize / (data.samplingRate * 2.0);
		return shift;
	}

	private void doChordExtraction(Configuration c) {
		double[][] result = spectrum.spectrum;
		
//		String[] labels = NoteLabelProvider.getNoteLabels(startNoteOffsetInSemitonesFromF0, spectrum.scaleInfo);
//		String[] labels1 = NoteLabelProvider.getNoteLabels(startNoteOffsetInSemitonesFromF0, new ScaleInfo(1, 12));
//		whitened = DataUtil.whitenSpectrum(result, scaleInfo.getNotesInOctaveCount());

		result = DataUtil.smoothHorizontallyMedian(result, c.process.medianFilterWindow);
		result = DataUtil.shrink(result, c.spectrum.framesPerBeat);
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Spectrum as is");
//		XYDataset dataset = DatasetUtil.toXYDataset(originalBeatTimes, DataUtil.getSpectralFlatness(result));
//		JFreeChartUtils.visualize("Spectral variance", "Time", "Value", dataset);

//		double[] e = DataUtil.getSoundEnergyByFrequencyDistribution(result);
//		Visualizer.visualizeXByFrequencyDistribution(e, scaleInfo, startNoteOffsetInSemitonesFromF0);

		result = DataUtil.toLogSpectrum(result);
		result = DiscreteCosineTransform.doChromaReduction(result, c.process.crpFirstNonZero);
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Chroma reduced spectrum");
		
		double[][] selfSim = DataUtil.getSelfSimilarity(result);
		selfSim = DataUtil.removeDissimilar(selfSim, c.process.selfSimilarityTheta);
		result = DataUtil.smoothWithSelfSimilarity(result, selfSim);
		
		result = DataUtil.toSingleOctave(result, c.spectrum.notesPerOctave);
		double[][] chromas = DataUtil.reduceTo12Notes(result);

//		key = Key.recognizeKey(getTonalProfile(pcp, 0, pcp.length), startNote);
		key = null;
		Note startNote = Note.byNumber(startNoteOffsetInSemitonesFromF0);
		TemplatesRecognition first = new TemplatesRecognition(startNote, key);
		Chord[] temp = first.recognize(chromas, new ScaleInfo(1, 12));
		
//		Map<Chord, double[]> newTemplates = TemplatesSmoother.smoothTemplates(chromas, temp);
//		for (Entry<Chord, double[]> entry : newTemplates.entrySet()) {
//			LOG.info(entry.getKey().toString() + " - " + Arrays.toString(entry.getValue()));
//		}
//		TemplatesRecognition second = new TemplatesRecognition(startNote, newTemplates.keySet(),
//				new SimpleTemplateProducer(newTemplates));
//		temp = second.recognize(chromas, new ScaleInfo(1, 12));
		
//		chords = temp;
		chords = Harmony.smoothUsingHarmony(chromas, temp, new ScaleInfo(1, 12), startNote);
	}

	private SpectrumData readSpectrum(Configuration c, String waveFileName,
			 double[] expandedBeatTimes) {
		SpectrumData result = new SpectrumData();
		result.beatTimes = expandedBeatTimes;
		result.scaleInfo = new ScaleInfo(c.spectrum.octaves, c.spectrum.notesPerOctave);
		result.startNoteOffsetInSemitonesFromF0 = c.spectrum.offsetFromF0InSemitones;
		result.wavFilePath = waveFileName;
		WavFile wavFile = null;
		try {
			wavFile = WavFile.openWavFile(new File(waveFileName));
			result.samplingRate = (int) wavFile.getSampleRate();
			int frames = (int) wavFile.getNumFrames();
			result.totalSeconds = frames * 1.0 / result.samplingRate;
			
			result.f0 = TuningFrequencyFinder.getTuningFrequency(waveFileName, c.process.threadPoolSize);
//				result.f0 = CQConstants.F0_DEFAULT;
			CQConstants cqConstants = CQConstants.getInstance(result.samplingRate,
					result.scaleInfo, result.f0, result.startNoteOffsetInSemitonesFromF0);
			int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
			// need to make windows centered at the beat positions, so shift them to the left
			double shift = getWindowsShift(result);
			for (int i = 0; i < result.beatTimes.length; i++) {
				result.beatTimes[i] -= shift;
			}
			WaveReader reader = new WaveReader(wavFile, result.beatTimes, windowSize);
			PooledTransformer transformer = new PooledTransformer(
					reader, c.process.threadPoolSize, result.beatTimes.length, result.scaleInfo, cqConstants);
			result.spectrum = transformer.run();
		} catch (WavFileException e) {
			LOG.error("Error when reading wave file " + waveFileName, e);
		} catch (IOException e) {
			LOG.error("Error when reading wave file " + waveFileName, e);
		} catch (InterruptedException e) {
			LOG.error("Error when reading wave file " + waveFileName, e);
		} finally {
			if (wavFile != null) { try {
				wavFile.close();
			} catch (IOException e) {
				LOG.error("Error when closing file " + waveFileName, e);
			} }
		}
		return result;
	}

	private SpectrumData readSpectrum(String spectrumFileName) {
		SpectrumData serialized = SpectrumFileReader.read(spectrumFileName);
		if (serialized != null) {
			LOG.info("Spectrum was read from " + spectrumFileName);
		}
		return serialized;
	}

	public double[] getOriginalBeatTimes() {
		return ArrayUtils.add(originalBeatTimes, totalSeconds);
//		return originalBeatTimes;
	}

	public double[] getBeatTimes() {
		return expandedBeatTimes;
	}

	public Chord[] getChords() {
		return chords;
	}

	public Key getKey() {
		return key;
	}

	public SpectrumData getSpectrum() {
		return spectrum;
	}

	public double getTotalSeconds() {
		return totalSeconds;
	}

	public int getStartNoteOffsetInSemitonesFromF0() {
		return startNoteOffsetInSemitonesFromF0;
	}

}
