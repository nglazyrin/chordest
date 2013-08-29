package chordest.transform.window;

import java.io.Serializable;

public interface IWindowFunction extends Serializable {

	public double getValue(int i);
}
