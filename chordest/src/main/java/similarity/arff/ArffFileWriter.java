package similarity.arff;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;

public class ArffFileWriter {

	private static final Logger LOG = LoggerFactory.getLogger(ArffFileWriter.class);

	public static boolean write(String fileName, Instances instances) {
		File file = new File(fileName);
		try {
			FileUtils.forceMkdir(file.getParentFile());
			DataSink.write(fileName, instances);
		} catch (Exception e) {
			LOG.error(String.format("Error saving instances to file '%s'",
					fileName), e);
			return false;
		}
		LOG.info(String.format("%d instances saved successfully to '%s'",
				instances.numInstances(), fileName));
		return true;
	}

}
