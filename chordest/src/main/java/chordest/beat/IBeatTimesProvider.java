package chordest.beat;

public interface IBeatTimesProvider {

	/**
	 * @return An array of beat times detected for a given sound recording.
	 * Array values have ascending order.
	 */
	public double[] getBeatTimes();
}
