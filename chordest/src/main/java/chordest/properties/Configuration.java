package chordest.properties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Configuration {

	private static final String SPECTRUM_OCTAVES_KEY = "spectrum.octaves";
	private static final String SPECTRUM_NOTES_PER_OCTAVE_KEY = "spectrum.notesPerOctave";
	private static final String SPECTRUM_OFFSET_FROM_F0_IN_SEMITONES_KEY = "spectrum.offsetFromF0InSemitones";
	private static final String SPECTRUM_FRAMES_PER_BEAT_KEY = "spectrum.framesPerBeat";

	private static final String PROCESS_THREAD_POOL_SIZE_KEY = "process.threadPoolSize";
	private static final String PROCESS_MEDIAN_FILTER_1_WINDOW_KEY = "process.medianFilter1Window";
	private static final String PROCESS_MEDIAN_FILTER_2_WINDOW_KEY = "process.medianFilter2Window";
	private static final String PROCESS_SELF_SIMILARITY_THETA_KEY = "process.recurrencePlotTheta";
	private static final String PROCESS_SELF_SIMILARITY_MIN_LENGTH_KEY = "process.recurrencePlotMinLength";

	public final SpectrumProperties spectrum;
	public final ProcessProperties process;

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
        this.spectrum = new SpectrumProperties(octaves, notesPerOctave, offsetFromF0, framesPerBeat);

        int threadPoolSize = Integer.parseInt(prop.getProperty(PROCESS_THREAD_POOL_SIZE_KEY));
        int window1 = Integer.parseInt(prop.getProperty(PROCESS_MEDIAN_FILTER_1_WINDOW_KEY));
        int window2 = Integer.parseInt(prop.getProperty(PROCESS_MEDIAN_FILTER_2_WINDOW_KEY));
        double theta = Double.parseDouble(prop.getProperty(PROCESS_SELF_SIMILARITY_THETA_KEY));
        int minLength = Integer.parseInt(prop.getProperty(PROCESS_SELF_SIMILARITY_MIN_LENGTH_KEY));
        this.process = new ProcessProperties(threadPoolSize, window1, window2, theta, minLength);
	};

	public class SpectrumProperties {
		private static final int OCTAVES_DEFAULT = 4;
		private static final int NOTES_PER_OCTAVE_DEFAULT = 60;
		private static final int OFFSET_FROM_F0_IN_SEMITONES_DEFAULT = -33;
		private static final int FRAMES_PER_BEAT_DEFAULT = 8;

		public final int octaves;
		public final int notesPerOctave;
		public final int offsetFromF0InSemitones;
		public final int framesPerBeat;
		
		private SpectrumProperties(int octaves, int notesPerOctave,
				int offsetFromF0InSemitones, int framesPerBeat) {
			this.octaves = octaves > 0 ? octaves : OCTAVES_DEFAULT;
			this.notesPerOctave = notesPerOctave > 0 ? notesPerOctave : NOTES_PER_OCTAVE_DEFAULT;
			this.offsetFromF0InSemitones = offsetFromF0InSemitones > -100 ?
					offsetFromF0InSemitones : OFFSET_FROM_F0_IN_SEMITONES_DEFAULT;
			this.framesPerBeat = framesPerBeat > 0 ? framesPerBeat : FRAMES_PER_BEAT_DEFAULT;
		}
	}

	public class ProcessProperties {
		private static final int THREAD_POOL_SIZE_DEFAULT = 4;
		private static final int MEDIAN_FILTER_1_WINDOW_DEFAULT = 17;
		private static final int MEDIAN_FILTER_2_WINDOW_DEFAULT = 3;
		private static final double SELF_SIMILARITY_THETA_DEFAULT = 0.15;
		private static final int SELF_SIMILARITY_MIN_LENGTH_DEFAULT = 3;

		public final int threadPoolSize;
		public final int medianFilter1Window;
		public final int medianFilter2Window;
		public final double selfSimilarityTheta;
		public final int selfSimilarityMinLength;

		private ProcessProperties(int threadPoolSize, int window1, int window2,
				double rpTheta, int rpMinLength) {
			this.threadPoolSize = threadPoolSize > 0 ? threadPoolSize : THREAD_POOL_SIZE_DEFAULT;
			this.medianFilter1Window = window1 > 0 ? window1 : MEDIAN_FILTER_1_WINDOW_DEFAULT;
			this.medianFilter2Window = window2 > 0 ? window2 : MEDIAN_FILTER_2_WINDOW_DEFAULT;
			this.selfSimilarityTheta = rpTheta > 0 ? rpTheta : SELF_SIMILARITY_THETA_DEFAULT;
			this.selfSimilarityMinLength = rpMinLength > 0 ? rpMinLength : SELF_SIMILARITY_MIN_LENGTH_DEFAULT;
		}
	}

}
