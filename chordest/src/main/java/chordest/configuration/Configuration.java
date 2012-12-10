package chordest.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Configuration {

	private static final String SPECTRUM_OCTAVES_KEY = "spectrum.octaves";
	private static final String SPECTRUM_NOTES_PER_OCTAVE_KEY = "spectrum.notesPerOctave";
	private static final String SPECTRUM_OFFSET_FROM_F0_IN_SEMITONES_KEY = "spectrum.offsetFromF0InSemitones";
	private static final String SPECTRUM_FRAMES_PER_BEAT_KEY = "spectrum.framesPerBeat";
	private static final String SPECTRUM_THREAD_POOL_SIZE_KEY = "spectrum.threadPoolSize";
	
	private static final String PROCESS_MEDIAN_FILTER_WINDOW_KEY = "process.medianFilterWindow";
	private static final String PROCESS_SELF_SIMILARITY_THETA_KEY = "process.selfSimilarityTheta";
	private static final String PROCESS_CRP_FIRST_NON_ZERO_KEY = "process.crpFirstNonZero";

	public static final String DEFAULT_CONFIG_FILE_LOCATION = "config" + File.separator + "chordest.properties";

	public final SpectrumProperties spectrum;
	public final ProcessProperties process;

	public Configuration() {
		this(DEFAULT_CONFIG_FILE_LOCATION);
	}

	public Configuration(String propertiesFile) {
		Properties prop = new Properties();
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
        
        int octaves = Integer.parseInt(prop.getProperty(SPECTRUM_OCTAVES_KEY));
        int notesPerOctave = Integer.parseInt(prop.getProperty(SPECTRUM_NOTES_PER_OCTAVE_KEY));
        int offsetFromF0 = Integer.parseInt(prop.getProperty(SPECTRUM_OFFSET_FROM_F0_IN_SEMITONES_KEY));
        int framesPerBeat = Integer.parseInt(prop.getProperty(SPECTRUM_FRAMES_PER_BEAT_KEY));
        int threadPoolSize = Integer.parseInt(prop.getProperty(SPECTRUM_THREAD_POOL_SIZE_KEY));
        this.spectrum = new SpectrumProperties(octaves, notesPerOctave, offsetFromF0, framesPerBeat, threadPoolSize);

        int window = Integer.parseInt(prop.getProperty(PROCESS_MEDIAN_FILTER_WINDOW_KEY));
        double theta = Double.parseDouble(prop.getProperty(PROCESS_SELF_SIMILARITY_THETA_KEY));
        int crpFirstNonZero = Integer.parseInt(prop.getProperty(PROCESS_CRP_FIRST_NON_ZERO_KEY));
        this.process = new ProcessProperties(window, theta, crpFirstNonZero);
	};

	public class SpectrumProperties {
		private static final int OCTAVES_DEFAULT = 4;
		private static final int NOTES_PER_OCTAVE_DEFAULT = 60;
		private static final int OFFSET_FROM_F0_IN_SEMITONES_DEFAULT = -33;
		private static final int FRAMES_PER_BEAT_DEFAULT = 8;
		private static final int THREAD_POOL_SIZE_DEFAULT = 4;

		public final int octaves;
		public final int notesPerOctave;
		public final int offsetFromF0InSemitones;
		public final int framesPerBeat;
		public final int threadPoolSize;
		
		private SpectrumProperties(int octaves, int notesPerOctave,
				int offsetFromF0InSemitones, int framesPerBeat, int threadPoolSize) {
			this.octaves = octaves > 0 ? octaves : OCTAVES_DEFAULT;
			this.notesPerOctave = notesPerOctave > 0 ? notesPerOctave : NOTES_PER_OCTAVE_DEFAULT;
			this.offsetFromF0InSemitones = offsetFromF0InSemitones > -100 ?
					offsetFromF0InSemitones : OFFSET_FROM_F0_IN_SEMITONES_DEFAULT;
			this.framesPerBeat = framesPerBeat > 0 ? framesPerBeat : FRAMES_PER_BEAT_DEFAULT;
			this.threadPoolSize = threadPoolSize > 0 ? threadPoolSize : THREAD_POOL_SIZE_DEFAULT;
		}
	}

	public class ProcessProperties {
		private static final int MEDIAN_FILTER_WINDOW_DEFAULT = 17;
		private static final double SELF_SIMILARITY_THETA_DEFAULT = 0.10;
		private static final int CRP_FIRST_NON_ZERO_DEFAULT = 10;

		public final int medianFilterWindow;
		public final double selfSimilarityTheta;
		public final int crpFirstNonZero;

		private ProcessProperties(int window, double ssTheta, int crpFirstNonZero) {
			this.medianFilterWindow = window > 0 ? window : MEDIAN_FILTER_WINDOW_DEFAULT;
			this.selfSimilarityTheta = ssTheta > 0 ? ssTheta : SELF_SIMILARITY_THETA_DEFAULT;
			this.crpFirstNonZero = crpFirstNonZero >= 0 ? crpFirstNonZero : CRP_FIRST_NON_ZERO_DEFAULT;
		}
	}

}
