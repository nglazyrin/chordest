package similarity.util;

import junit.framework.Assert;

import org.junit.Test;

public class DataUtilTest {

	@Test
	public void testShrinkSmoke() {
		double[][] array = new double[8][];
		array[0] = new double[] { 1, 2 };
		array[1] = new double[] { 3, 4 };
		array[2] = new double[] { 5, 6 };
		array[3] = new double[] { 7, 8 };
		array[4] = new double[] { 1, 2 };
		array[5] = new double[] { 3, 4 };
		array[6] = new double[] { 5, 6 };
		array[7] = new double[] { 7, 8 };
		double[][] s1 = DataUtil.shrink(array, 4);
		Assert.assertEquals(2, s1.length);
		double eps = 0.00001;
		Assert.assertTrue(Math.abs(4 - s1[0][0]) < eps);
		Assert.assertTrue(Math.abs(5 - s1[0][1]) < eps);
		Assert.assertTrue(Math.abs(4 - s1[1][0]) < eps);
		Assert.assertTrue(Math.abs(5 - s1[1][1]) < eps);
	}

	@Test
	public void testShrink9by4() {
		double[][] array = new double[9][];
		array[0] = new double[] { 1, 2 };
		array[1] = new double[] { 3, 4 };
		array[2] = new double[] { 5, 6 };
		array[3] = new double[] { 7, 8 };
		array[4] = new double[] { 1, 2 };
		array[5] = new double[] { 3, 4 };
		array[6] = new double[] { 5, 6 };
		array[7] = new double[] { 7, 8 };
		array[8] = new double[] { 1, 2 };
		double[][] s1 = DataUtil.shrink(array, 4);
		Assert.assertEquals(3, s1.length);
		double eps = 0.00001;
		Assert.assertTrue(Math.abs(4 - s1[0][0]) < eps);
		Assert.assertTrue(Math.abs(5 - s1[0][1]) < eps);
		Assert.assertTrue(Math.abs(4 - s1[1][0]) < eps);
		Assert.assertTrue(Math.abs(5 - s1[1][1]) < eps);
		Assert.assertTrue(Math.abs(1 - s1[2][0]) < eps);
		Assert.assertTrue(Math.abs(2 - s1[2][1]) < eps);
	}

	@Test
	public void testShrink10by4() {
		double[][] array = new double[10][];
		array[0] = new double[] { 1, 2 };
		array[1] = new double[] { 3, 4 };
		array[2] = new double[] { 5, 6 };
		array[3] = new double[] { 7, 8 };
		array[4] = new double[] { 1, 2 };
		array[5] = new double[] { 3, 4 };
		array[6] = new double[] { 5, 6 };
		array[7] = new double[] { 7, 8 };
		array[8] = new double[] { 1, 2 };
		array[9] = new double[] { 3, 4 };
		double[][] s1 = DataUtil.shrink(array, 4);
		Assert.assertEquals(3, s1.length);
		double eps = 0.00001;
		Assert.assertTrue(Math.abs(4 - s1[0][0]) < eps);
		Assert.assertTrue(Math.abs(5 - s1[0][1]) < eps);
		Assert.assertTrue(Math.abs(4 - s1[1][0]) < eps);
		Assert.assertTrue(Math.abs(5 - s1[1][1]) < eps);
		Assert.assertTrue(Math.abs(2 - s1[2][0]) < eps);
		Assert.assertTrue(Math.abs(3 - s1[2][1]) < eps);
	}

}
