package chordest.main;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordExtractor.IExternalProcessor;
import chordest.configuration.Configuration;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.spectrum.SpectrumData;
import chordest.util.DataUtil;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class TrainDataGenerator implements IExternalProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(TrainDataGenerator.class);
	private static final String DELIMITER = ",";
	private static final String ENCODING = "utf-8";
	private static final String CSV_FILE = PathConstants.OUTPUT_DIR + "train.csv";

	private OutputStream csvOut;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: TrainDataGenerator /path/to/trainFileList.txt");
			System.exit(-1);
		}
		List<String> tracklist = TracklistCreator.readTrackList(args[0]);
		TrainDataGenerator.deleteIfExists(CSV_FILE);
		int filesProcessed = 0;
		for (final String binFileName : tracklist) {
			TrainDataGenerator tdg = new TrainDataGenerator(CSV_FILE, true);
			double[][] result = TrainDataGenerator.prepareSpectrum(binFileName);
			tdg.process(result);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info("Done. " + tracklist.size() + " files were processed. Result was saved to " + CSV_FILE);
	}

	public static void deleteIfExists(String fileName) {
		File resultFile = new File(fileName);
		if (resultFile.exists()) {
			try {
				FileUtils.forceDelete(resultFile);
			} catch (IOException e) {
				LOG.warn("Error when deleting file " + fileName, e);
			}
		}
	}

	public static double[][] prepareSpectrum(final String binFileName) {
		SpectrumData sd = SpectrumFileReader.read(binFileName);
		double[][] result = sd.spectrum;
		int window = new Configuration().process.medianFilterWindow;
		result = DataUtil.smoothHorizontallyMedian(result, window);
		result = DataUtil.shrink(result, sd.framesPerBeat);
		result = DataUtil.toLogSpectrum(result);
		DataUtil.scaleTo01(result);
		return result;
	}

	public TrainDataGenerator(String outputCsvFileName, boolean append) {
		File file = new File(outputCsvFileName);
		try {
			csvOut = FileUtils.openOutputStream(file, append);
		} catch (IOException e) {
			LOG.error("Error when creating resulting .csv file", e);
			System.exit(-2);
		}
	}

	@Override
	public double[][] process(double[][] data) {
		if (data == null) {
			return null;
		}
		try {
			for (int i = 0; i < data.length; i++) {
				csvOut.write(toByteArray(data[i]));
			}
		} catch (IOException e) {
			LOG.error("Error when writing result", e);
		} finally {
			try {
				csvOut.close();
			} catch (IOException e) {
				LOG.error("Error when closing output stream for the resulting file", e);
			}
		}
		return data;
	}

	private byte[] toByteArray(double[] ds) throws UnsupportedEncodingException {
		if (ds == null || ds.length == 0) {
			return new byte[0];
		}
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < ds.length - 1; j++) {
			sb.append(ds[j]);
			sb.append(DELIMITER);
		}
		sb.append(ds[ds.length - 1]);
		sb.append("\r\n");
		return sb.toString().getBytes(ENCODING);
	}

}
