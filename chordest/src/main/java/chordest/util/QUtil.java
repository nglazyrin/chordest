package chordest.util;


public class QUtil {

	public static int minWindowLength(double Q, double minFrequency, int samplingRate) {
		double value = Q * samplingRate / minFrequency;
		return (int)value + 1;
	}

	public static double calculateQ(int notesInOctave) {
		return 1.0 / (Math.pow(2, 1.0/notesInOctave) - 1);
	}

}
