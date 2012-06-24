package chordest.wave;

public class ReadOnlyBuffer extends Buffer {

	public static ReadOnlyBuffer newEmptyInstance() {
		return new ReadOnlyBuffer(new double[0], 0);
	}

	public ReadOnlyBuffer(double[] data, double timeStamp) {
		super(data, timeStamp);
		close();
	}

}
