package similarity.transform.window;


public class RectangleWindowFunction implements IWindowFunction {

	private static final long serialVersionUID = 2L;

	public int getSize() {
		return 4096 * 2;
	}

	public double getValue(int i) {
		return 1;
	}

}
