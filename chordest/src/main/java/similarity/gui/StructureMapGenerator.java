package similarity.gui;

import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;


public class StructureMapGenerator {

	private static final int DIM = 12;

	public static double[] getStructureMap(double[] features) {
		if (features == null || features.length % DIM != 0) {
			throw new IllegalArgumentException();
		}
		int sections = features.length / DIM;
		double max = 0;
		double[] prod = new double[sections * sections];
		for (int i = 0; i < sections; i++) {
			int index1 = getIndexForSection(i);
			for (int j = i; j < sections; j++) {
				int index2 = getIndexForSection(j);
				double distance = getDistance(features, index1, index2);
				prod[i * sections + j] = distance;
				prod[j * sections + i] = distance;
				max = Math.max(max, distance);
			}
		}
		normalize(prod, max);
		return prod;
	}

	public static XYZDataset toXYZDataset(double[] xArray, double[] zArray) {
		if (xArray == null) {
			throw new IllegalArgumentException("xArray is null");
		}
		if (zArray == null) {
			throw new IllegalArgumentException("zArray is null");
		}
		DefaultXYZDataset result = new DefaultXYZDataset();
		int cols = xArray.length;
		int numberOfElements = zArray.length;
		double[][] data = new double[3][];
		data[0] = new double[numberOfElements];
		data[1] = new double[numberOfElements];
		data[2] = new double[numberOfElements];
		for (int index = 0; index < numberOfElements; index++) {
			int col = index / cols;
			int row = index % cols;
			data[0][index] = xArray[col];	// X
			data[1][index] = xArray[row];	// Y
			data[2][index] = zArray[index];	// Z
		}
		result.addSeries(new Double(1), data);
		return result;

	}

	private static int getIndexForSection(int section) {
		if (section < 0) {
			throw new IllegalArgumentException("Section = " + section);
		}
		return section * DIM;
	}

	private static double getDistance(double[] features, int index1, int index2) {
		double result = 0;
		for (int i = 0; i < DIM; i++) {
			result += features[index1 + i] * features[index2 + i];
		}
		return Math.log(result * result);
	}

	private static void normalize(double[] array, double maxValue) {
		if (array == null) { throw new IllegalArgumentException("array == null"); }
		if (maxValue == 0) { throw new IllegalArgumentException("maxValue == 0"); }
		for (int i = 0; i < array.length; i++) {
			array[i] /= maxValue;
		}
	}

}
