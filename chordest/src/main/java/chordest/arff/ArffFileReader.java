package chordest.arff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class ArffFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(ArffFileReader.class);

	public static Instances read(String fileName) {
		Instances trainingData = null;
		try {
			trainingData = DataSource.read(fileName);
			trainingData.setClassIndex(trainingData.numAttributes() - 1);
		} catch (Exception e) {
			LOG.error("Error reading '" + fileName + "': ", e);
		}
		return trainingData;
	}

}
