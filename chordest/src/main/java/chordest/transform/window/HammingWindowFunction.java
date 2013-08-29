package chordest.transform.window;

/** http://en.wikipedia.org/wiki/Window_function */
public class HammingWindowFunction implements IWindowFunction {

	private static final long serialVersionUID = 3L;
	public final int size;
	public final double[] values;

	public HammingWindowFunction(int size) {
		this.size = size;
		this.values = new double[size];
		initialize();
	}

	private void initialize() {
		final int LEN_1 = values.length - 1;
		final double alpha = 25.0 / 46.0;
		final double alpha1 = 1 - alpha;
		final double PHI = 2 * Math.PI / LEN_1;
		for (int i = 0; i <= LEN_1; i++) {
			values[i] = alpha - alpha1 * Math.cos(i * PHI);
		}
	}

	public int getSize() {
		return this.size;
	}

	public double getValue(int i) {
		if (i < 0 || i >= values.length) {
			return 0;
		}
		return values[i];
	}
}
