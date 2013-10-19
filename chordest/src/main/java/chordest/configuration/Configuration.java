package chordest.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chordest configuration. Possible keys and default values are defined here.
 * @author Nikolay
 *
 */
public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	private static final String SPECTRUM_OCTAVES_KEY = "spectrum.octaves";
	private static final String SPECTRUM_NOTES_PER_OCTAVE_KEY = "spectrum.notesPerOctave";
	private static final String SPECTRUM_OFFSET_FROM_F0_IN_SEMITONES_KEY = "spectrum.offsetFromF0InSemitones";
	private static final String SPECTRUM_FRAMES_PER_BEAT_KEY = "spectrum.framesPerBeat";
	private static final String SPECTRUM_THREAD_POOL_SIZE_KEY = "spectrum.threadPoolSize";
	private static final String SPECTRUM_BEAT_TIMES_DELAY_IN_MS = "spectrum.beatTimesDelayInMs";
	
	private static final String PROCESS_MEDIAN_FILTER_WINDOW_KEY = "process.medianFilterWindow";
	private static final String PROCESS_SELF_SIMILARITY_THETA_KEY = "process.selfSimilarityTheta";
	private static final String PROCESS_CRP_FIRST_NON_ZERO_KEY = "process.crpFirstNonZero";
	private static final String PROCESS_CRP_LOGARITHM_ETA_KEY = "process.crpLogEta";

	private static final String PRE_VAMP_HOST_PATH_KEY = "pre.vampHostPath";
	private static final String PRE_ESTIMATE_TUNING_FREQUENCY_KEY = "pre.estimateTuningFrequency";

	public static final String DEFAULT_CONFIG_FILE_LOCATION = "config" + File.separator + "chordest.properties";

	public final SpectrumProperties spectrum;
	public final ProcessProperties process;
	public final PreProcessProperties pre;

	public Configuration() {
		this(DEFAULT_CONFIG_FILE_LOCATION);
	}

	public Configuration(String propertiesFile) {
		Properties prop = new Properties();
		LOG.info("Trying to read configuration from " + propertiesFile);
		FileInputStream input;
		try {
			input = new FileInputStream(propertiesFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("File not found: " + propertiesFile, e);
		}
        try {
            prop.load(input);
        } catch (Exception e) {
        	e.printStackTrace();
			throw new IllegalArgumentException("Could not read file: " + propertiesFile, e);
        } finally {
        	try {
				input.close();
			} catch (IOException ignore) { }
        }
        
        int octaves = getIntValue(prop.getProperty(SPECTRUM_OCTAVES_KEY));
        int notesPerOctave = getIntValue(prop.getProperty(SPECTRUM_NOTES_PER_OCTAVE_KEY));
        int offsetFromF0 = getIntValue(prop.getProperty(SPECTRUM_OFFSET_FROM_F0_IN_SEMITONES_KEY));
        int framesPerBeat = getIntValue(prop.getProperty(SPECTRUM_FRAMES_PER_BEAT_KEY));
        int threadPoolSize = getIntValue(prop.getProperty(SPECTRUM_THREAD_POOL_SIZE_KEY));
        double beatTimesDelay = getDoubleValue(prop.getProperty(SPECTRUM_BEAT_TIMES_DELAY_IN_MS));
        this.spectrum = new SpectrumProperties(octaves, notesPerOctave, offsetFromF0, framesPerBeat, threadPoolSize, beatTimesDelay);

        int window = getIntValue(prop.getProperty(PROCESS_MEDIAN_FILTER_WINDOW_KEY));
        double theta = getDoubleValue(prop.getProperty(PROCESS_SELF_SIMILARITY_THETA_KEY));
        int logEta = getIntValue(prop.getProperty(PROCESS_CRP_LOGARITHM_ETA_KEY));
        int crpFirstNonZero = getIntValue(prop.getProperty(PROCESS_CRP_FIRST_NON_ZERO_KEY));
        this.process = new ProcessProperties(window, theta, logEta, crpFirstNonZero);
        
        String vampHost = prop.getProperty(PRE_VAMP_HOST_PATH_KEY);
        boolean estimateTuningFrequency = Boolean.parseBoolean(prop.getProperty(PRE_ESTIMATE_TUNING_FREQUENCY_KEY));
        this.pre = new PreProcessProperties(vampHost, estimateTuningFrequency);
        
        printConfiguration();
	};

	private double getDoubleValue(String value) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private int getIntValue(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return -10000;
		}
	}

	private void printConfiguration() {
		LOG.info("Following configuration will be used:");
		LOG.info("\t" + SPECTRUM_OCTAVES_KEY + " = " + spectrum.octaves);
		LOG.info("\t" + SPECTRUM_NOTES_PER_OCTAVE_KEY + " = " + spectrum.notesPerOctave);
		LOG.info("\t" + SPECTRUM_OFFSET_FROM_F0_IN_SEMITONES_KEY + " = " + spectrum.offsetFromF0InSemitones);
		LOG.info("\t" + SPECTRUM_FRAMES_PER_BEAT_KEY + " = " + spectrum.framesPerBeat);
		LOG.info("\t" + SPECTRUM_THREAD_POOL_SIZE_KEY + " = " + spectrum.threadPoolSize);
		LOG.info("\t" + SPECTRUM_BEAT_TIMES_DELAY_IN_MS + " = " + spectrum.beatTimesDelay);
		LOG.info("\t" + PROCESS_MEDIAN_FILTER_WINDOW_KEY + " = " + process.medianFilterWindow);
		LOG.info("\t" + PROCESS_SELF_SIMILARITY_THETA_KEY + " = " + process.selfSimilarityTheta);
		LOG.info("\t" + PROCESS_CRP_FIRST_NON_ZERO_KEY + " = " + process.crpFirstNonZero);
		LOG.info("\t" + PRE_VAMP_HOST_PATH_KEY + " = " + pre.vampHostPath);
		LOG.info("\t" + PRE_ESTIMATE_TUNING_FREQUENCY_KEY + " = " + pre.estimateTuningFrequency);
	}

	public static class SpectrumProperties {
		private static final int OCTAVES_DEFAULT = 4;
		private static final int NOTES_PER_OCTAVE_DEFAULT = 60;
		private static final int OFFSET_FROM_F0_IN_SEMITONES_DEFAULT = -33;
		private static final int FRAMES_PER_BEAT_DEFAULT = 8;
		private static final int THREAD_POOL_SIZE_DEFAULT = 4;
		private static final double BEAT_TIMES_DELAY_DEFAULT = 100;

		public final int octaves;
		public final int notesPerOctave;
		public final int offsetFromF0InSemitones;
		public final int framesPerBeat;
		public final int threadPoolSize;
		public final double beatTimesDelay;
		
		public SpectrumProperties(int octaves, int notesPerOctave,
				int offsetFromF0InSemitones, int framesPerBeat, int threadPoolSize, double beatTimesDelay) {
			this.octaves = octaves > 0 ? octaves : OCTAVES_DEFAULT;
			this.notesPerOctave = notesPerOctave > 0 ? notesPerOctave : NOTES_PER_OCTAVE_DEFAULT;
			this.offsetFromF0InSemitones = offsetFromF0InSemitones > -100 ?
					offsetFromF0InSemitones : OFFSET_FROM_F0_IN_SEMITONES_DEFAULT;
			this.framesPerBeat = framesPerBeat > 0 ? framesPerBeat : FRAMES_PER_BEAT_DEFAULT;
			this.threadPoolSize = threadPoolSize > 0 ? threadPoolSize : THREAD_POOL_SIZE_DEFAULT;
			this.beatTimesDelay = beatTimesDelay > 0 ? beatTimesDelay : BEAT_TIMES_DELAY_DEFAULT;
		}
	}

	public static class ProcessProperties {
		private static final int MEDIAN_FILTER_WINDOW_DEFAULT = 15;
		private static final double SELF_SIMILARITY_THETA_DEFAULT = 0.10;
		private static final int CRP_LOG_ETA_DEFAULT = 50000;
		private static final int CRP_FIRST_NON_ZERO_DEFAULT = 15;

		public final int medianFilterWindow;
		public final double selfSimilarityTheta;
		public final int crpLogEta;
		public final int crpFirstNonZero;

		private ProcessProperties(int window, double ssTheta, int crpLogEta, int crpFirstNonZero) {
			this.medianFilterWindow = window > 0 ? window : MEDIAN_FILTER_WINDOW_DEFAULT;
			this.selfSimilarityTheta = ssTheta > 0 ? ssTheta : SELF_SIMILARITY_THETA_DEFAULT;
			this.crpLogEta = crpLogEta > 0 ? crpLogEta : CRP_LOG_ETA_DEFAULT;
			this.crpFirstNonZero = crpFirstNonZero >= 0 ? crpFirstNonZero : CRP_FIRST_NON_ZERO_DEFAULT;
		}
	}

	public static class PreProcessProperties {
		private static final String VAMP_HOST_PATH_DEFAULT = "vamp-simple-host.exe";
		
		public final String vampHostPath;
		public final boolean estimateTuningFrequency;
		
		private PreProcessProperties(String vampHost, boolean estimateTuningFrequency) {
			this.vampHostPath = StringUtils.isNotEmpty(vampHost) ? vampHost : VAMP_HOST_PATH_DEFAULT;
			this.estimateTuningFrequency = estimateTuningFrequency;
		}
	}

}
