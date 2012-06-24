package similarity.wave;

public class Buffer {

	private final double[] data;
	private final double timeStamp;
	private final int size;
	private int position = 0;

	public Buffer(int size, double timeStamp) {
		this.size = size;
		this.data = new double[size];
		this.timeStamp = timeStamp;
	}

	protected Buffer(double[] data, double timeStamp) {
		this.data = data;
		this.size = data.length;
		this.timeStamp = timeStamp;
	}

	public void append(double[] frame) {
		if (! isFull()) {
			int available = size - position;
			int toCopy = Math.min(frame.length, available);
			System.arraycopy(frame, 0, data, position, toCopy);
			position += toCopy;
		} else {
			throw new IllegalStateException(String.format(
					"Buffer for timestamp %f is already full", getTimeStamp()));
		}
	}

	public boolean isFull() {
		return position >= size;
	}

	public Buffer close() {
		position = size;
		return this;
	}

	public double[] getData() {
		if (! isFull()) {
			throw new IllegalStateException(String.format(
					"Buffer for timestamp %f is not full yet", getTimeStamp()));
		}
		return data;
	}

	public double getTimeStamp() {
		return timeStamp;
	}

}
