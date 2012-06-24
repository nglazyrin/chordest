package chordest.gui;

import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.urls.StandardXYZURLGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;


public class JFreeChartUtils {

	public static JFreeChart createXYBlockChart(
			String title,
			String xAxisLabel,
			String yAxisLabel,
			XYZDataset dataset,
			PlotOrientation orientation,
			boolean legend,
			boolean tooltips,
			boolean urls) {
		if (orientation == null) {
			throw new IllegalArgumentException("Null 'orientation' argument.");
		}
		NumberAxis xAxis = new NumberAxis(xAxisLabel);
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = new NumberAxis(yAxisLabel);
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
		plot.setOrientation(orientation);
		
		XYBlockRenderer renderer = new XYBlockRenderer();
		if (tooltips) {
			renderer.setBaseToolTipGenerator(new StandardXYZToolTipGenerator());
		}
		if (urls) {
			renderer.setURLGenerator(new StandardXYZURLGenerator());
		}
		renderer.setPaintScale(new GrayPaintScale(0.0d, 1.0d));
		plot.setRenderer(renderer);
		plot.setOrientation(orientation);

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
		return chart;
	}

	/**
	 * Visualize 2D array of double values as grayscaled picture.
	 * @param title Window title
	 * @param xAxisLabel X values label
	 * @param yAxisLabel Y values label
	 * @param dataset XYZDataset
	 */
	public static void visualize(String title, String xAxisLabel,
			String yAxisLabel, XYZDataset dataset) {
		JFreeChart chart = JFreeChartUtils.createXYBlockChart(
				title,
				xAxisLabel,
				yAxisLabel,
				dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				false);
		showChart(chart, title, 1200, 400);
	}

	private static JFreeChart createXYStringBlockChart(
			String title,
			String xAxisLabel,
			String yAxisLabel,
			XYDataset dataset,
			PlotOrientation orientation,
			boolean legend,
			boolean tooltips,
			boolean urls,
			String[] yAxisLabels) {
		if (orientation == null) {
			throw new IllegalArgumentException("Null 'orientation' argument.");
		}
		NumberAxis xAxis = new NumberAxis(xAxisLabel);
		SymbolAxis yAxis = new SymbolAxis(yAxisLabel, yAxisLabels);
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
		plot.setOrientation(orientation);
		
		XYBlockRenderer renderer = new XYBlockRenderer();
		if (tooltips) {
			renderer.setBaseToolTipGenerator(new StandardXYZToolTipGenerator());
		}
		if (urls) {
			renderer.setURLGenerator(new StandardXYZURLGenerator());
		}
		renderer.setPaintScale(new GrayPaintScale(0.0d, 1.0d));
//		renderer.setBlockWidth(20);
		plot.setRenderer(renderer);
		plot.setOrientation(orientation);

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
		return chart;
	}

	/**
	 * Visualize 2D array of double values as grayscaled picture of 
	 * specified size.
	 * @param title Window title
	 * @param xAxisLabel X values label
	 * @param yAxisLabel Y values label
	 * @param dataset XYZDataset
	 * @param width Width of the picture
	 * @param height Height of the picture
	 */
	public static void visualize(String title, String xAxisLabel,
			String yAxisLabel, XYZDataset dataset, int width, int height) {
		JFreeChart chart = JFreeChartUtils.createXYBlockChart(
				title,
				xAxisLabel,
				yAxisLabel,
				dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				false);
		showChart(chart, title, width, height);
	}

	/**
	 * Visualize 2D array of double values as grayscaled picture of 
	 * specified size with logarithmic Y axis
	 * @param title Window title
	 * @param xAxisLabel X values label
	 * @param yAxisLabel Y values label
	 * @param dataset XYDataset or XYZDataset
	 * @param width Width of the picture
	 * @param height Height of the picture
	 */
	public static void visualizeStringY(String title, String xAxisLabel,
			String yAxisLabel, XYDataset dataset, int width, int height,
			String[] yAxisLabels) {
		JFreeChart chart = JFreeChartUtils.createXYStringBlockChart(
				title,
				xAxisLabel,
				yAxisLabel,
				dataset,
				PlotOrientation.VERTICAL,
				false,
				true,
				false,
				yAxisLabels);
		showChart(chart, title, width, height);
	}

	public static void visualize(String title, String xAxisLabel,
			String yAxisLabel, XYDataset dataset) {
		JFreeChart chart = ChartFactory.createXYLineChart(
				title,
				xAxisLabel,
				yAxisLabel,
				dataset,
				PlotOrientation.VERTICAL,
				true,
				true,
				true);
		showChart(chart, title, 1200, 400);
	}

	private static void showChart(JFreeChart chart, String title, int width, int height) {
		BufferedImage image = chart.createBufferedImage(width, height);
		JLabel lblChart = new JLabel();
		lblChart.setIcon(new ImageIcon(image));
		
		SimpleFrame frame = new SimpleFrame(lblChart, title);
		Thread st = new Thread(frame);
		st.start();
	}

}
