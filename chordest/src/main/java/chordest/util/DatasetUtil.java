package chordest.util;

import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Chord;
import chordest.model.Note;



public class DatasetUtil {

	static final Logger LOG = LoggerFactory.getLogger(DatasetUtil.class);

	/**
	 * Creates an XYZDataset from double[] array containing Z values column 
	 * by column and double[] array containing data for X axis (time).
	 * @param xArray A 1xN matrix
	 * @param yzArray A 1x(K*N) matrix of double Z values column by column
	 * @return XYZDataset with X=N, Y=K, Z is a double number
	 */
	public static XYZDataset toXYZDataset(double[] xArray, double[] yzArray) {
		if (xArray == null) {
			throw new IllegalArgumentException("xArray is null");
		}
		if (yzArray == null) {
			throw new IllegalArgumentException("array is null");
		}
		DefaultXYZDataset result = new DefaultXYZDataset();
		int cols = xArray.length;
		int numberOfElements = yzArray.length;
		double[][] data = new double[3][];
		data[0] = new double[numberOfElements];
		data[1] = new double[numberOfElements];
		data[2] = new double[numberOfElements];
		for (int index = 0; index < numberOfElements; index++) {
			int col = index % cols;
			int row = index / cols;
			data[0][index] = xArray[col];		// X
			data[1][index] = row;				// Y
			data[2][index] = yzArray[index];	// Z
		}
		result.addSeries(new Double(1), data);
		return result;
	}
	
	/**
	 * Creates an XYZDataset from double array containing X values 
	 * (timestamps), double[] array containing Y values (frequency) and
	 * double[][] array containing Z values (zData)
	 * @param xArray Array of timestamps
	 * @param yArray Array of note names corresponding to frequencies
	 * @param zData 2D array of results of Constant Q transform
	 * @return XYZDataset with X=N, Y=K, Z is a double number
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static XYZDataset toXYZDataset(double[] xArray, String[] yArray, 
			double[][] zData) throws InterruptedException, ExecutionException {
		if (xArray == null) {
			throw new IllegalArgumentException("xArray is null");
		}
		if (yArray == null) {
			throw new IllegalArgumentException("yArray is null");
		}
		if (zData == null) {
			throw new IllegalArgumentException("zData is null");
		}
		DefaultXYZDataset result = new DefaultXYZDataset();
		int cols = zData.length;
		int rows = yArray.length;
		int numberOfElements = cols * rows;
		double[][] data = new double[3][];
		data[0] = new double[numberOfElements];
		data[1] = new double[numberOfElements];
		data[2] = new double[numberOfElements];
		for (int col = 0; col < cols; col++) {
			double[] dataColumn = zData[col];
			if (dataColumn != null) {
				double timeStamp = xArray[col];
				for (int row = 0; row < rows; row++) {
					int index = row * cols + col;
					data[0][index] = timeStamp;			// X
					data[1][index] = row;				// Y
					data[2][index] = dataColumn[row];	// Z
				}
			}
		}
		result.addSeries(new Double(1), data);
		return result;
	}

	/**
	 * Creates an XYZDataset from double array containing X and Y values 
	 * (timestamps) and double[][] array containing Z values (zData)
	 * @param xyArray Array of timestamps
	 * @param zData 2D array of results of Constant Q transform
	 * @return XYZDataset with X=N, Y=N, Z is a double number
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static XYZDataset toXYZDataset(double[] xyArray,  
			double[][] zData) throws InterruptedException, ExecutionException {
		if (xyArray == null) {
			throw new IllegalArgumentException("xyArray is null");
		}
		if (zData == null) {
			throw new IllegalArgumentException("zData is null");
		}
		DefaultXYZDataset result = new DefaultXYZDataset();
		int cols = xyArray.length;
		int rows = xyArray.length;
		int numberOfElements = cols * rows;
		double[][] data = new double[3][];
		data[0] = new double[numberOfElements];
		data[1] = new double[numberOfElements];
		data[2] = new double[numberOfElements];
		for (int col = 0; col < cols; col++) {
			double[] dataColumn = zData[col];
			if (dataColumn != null) {
				double timeStamp1 = xyArray[col];
				for (int row = 0; row < rows; row++) {
					double timeStamp2 = xyArray[row];
					int index = row * cols + col;
					data[0][index] = timeStamp1;		// X
					data[1][index] = timeStamp2;		// Y
					data[2][index] = dataColumn[row];	// Z
				}
			}
		}
		result.addSeries(new Double(1), data);
		return result;
	}

	/**
	 * Creates an XYZDataset from double array containing X values 
	 * (timestamps) and Chord[] array containing chords, each consisting of a
	 * number of notes.
	 * @param xArray Array of timestamps
	 * @param chords Array of chords, 1 chord per each timestamp
	 * @return XYZDataset with X=N, Y=12, Z is 1 if chord contains this note 
	 * or 0 elsewhere 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static XYZDataset toXYZDataset(double[] xArray, 
			Chord[] chords, Note startNote) throws InterruptedException, ExecutionException {
		if (xArray == null) {
			throw new IllegalArgumentException("xArray is null");
		}
		if (chords == null) {
			throw new IllegalArgumentException("chords is null");
		}
		if (chords.length != xArray.length) {
			throw new IllegalArgumentException(String.format(
					"xArray and chords are of different size: %d != %d", xArray.length, chords.length));
		}
		DefaultXYZDataset result = new DefaultXYZDataset();
		int cols = chords.length;
		int rows = 12;
		int numberOfElements = cols * rows;
		double[][] data = new double[3][];
		data[0] = new double[numberOfElements];
		data[1] = new double[numberOfElements];
		data[2] = new double[numberOfElements];
		for (int col = 0; col < cols; col++) {
			Chord chord = chords[col];
			if (chord != null) {
				double timeStamp = xArray[col];
				for (int row = 0; row < rows; row++) {
					int index = row * cols + col;
					data[0][index] = timeStamp;			// X
					data[1][index] = row;				// Y
					data[2][index] = chord.hasNote(startNote.withOffset(row)) ? 0 : 1; // Z
				}
			}
		}
		result.addSeries(new Double(1), data);
		return result;
	}

	/**
	 * Creates an XYDataset from double[] array containing X values (e.g. time) 
	 * and double[] array containing Y values. Arrays must have same length.
	 * @param xArray 
	 * @param yArray
	 * @return XYDataset
	 */
	public static XYDataset toXYDataset(double[] xArray, double[] yArray) {
		if (xArray == null) {
			throw new IllegalArgumentException("xArray is null");
		}
		if (yArray == null) {
			throw new IllegalArgumentException("yArray is null");
		}
		if (xArray.length != yArray.length) {
			throw new IllegalArgumentException(String.format("xArray.length != yArray.length: %d != %d",
					xArray.length, yArray.length));
		}
		DefaultXYDataset result = new DefaultXYDataset();
		int cols = xArray.length;
		double[][] data = new double[2][];
		data[0] = new double[cols];
		data[1] = new double[cols];
		for (int col = 0; col < cols; col++) {
			data[0][col] = xArray[col];
			data[1][col] = yArray[col];
		}
		result.addSeries(new Double(1), data);
		return result;
	}

	/**
	 * Creates an XYDataset from double[] array containing Y values.
	 * X values are incremental from 0 to yArray.length
	 * @param yArray
	 * @return XYDataset
	 */
	public static XYDataset toXYDataset(double[] yArray) {
		if (yArray == null) {
			throw new IllegalArgumentException("yArray is null");
		}
		if (yArray.length != yArray.length) {
			throw new IllegalArgumentException(String.format("xArray.length != yArray.length: %d != %d",
					yArray.length, yArray.length));
		}
		DefaultXYDataset result = new DefaultXYDataset();
		int cols = yArray.length;
		double[][] data = new double[2][];
		data[0] = new double[cols];
		data[1] = new double[cols];
		for (int col = 0; col < cols; col++) {
			data[0][col] = yArray[col];
			data[1][col] = col;
		}
		result.addSeries(new Double(1), data);
		return result;
	}

	/**
	 * Creates an XYDataset from double[] array containing X values (time) 
	 * and double[] array containing Y values. Array must have same length.
	 * @param xArray 
	 * @param yArray
	 * @return XYDataset
	 */
	public static XYDataset toXYDataset(double[] xArray, int[] yArray) {
		if (xArray == null) {
			throw new IllegalArgumentException("xArray is null");
		}
		if (yArray == null) {
			throw new IllegalArgumentException("yArray is null");
		}
		if (xArray.length != yArray.length) {
			throw new IllegalArgumentException(String.format("xArray.length != yArray.length: %d != %d",
					xArray.length, yArray.length));
		}
		DefaultXYDataset result = new DefaultXYDataset();
		int cols = xArray.length;
		double[][] data = new double[2][];
		data[0] = new double[cols];
		data[1] = new double[cols];
		for (int col = 0; col < cols; col++) {
			data[0][col] = xArray[col];
			data[1][col] = yArray[col];
		}
		result.addSeries(new Double(1), data);
		return result;
	}

}
