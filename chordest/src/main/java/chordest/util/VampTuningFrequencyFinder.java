package chordest.util;

import java.io.File;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.configuration.Configuration.PreProcessProperties;

public class VampTuningFrequencyFinder {

	private static Logger LOG = LoggerFactory.getLogger(VampTuningFrequencyFinder.class);

	private static final String TUNING_PLUGIN_NAME = "nnls-chroma:tuning";

	private double freq;

	public VampTuningFrequencyFinder(String wavFilePath, String tempFilePath, PreProcessProperties pre) {
		LOG.info("Performing tuning frequency detection for " + wavFilePath + " ...");
		String[] cmd = { pre.vampHostPath, TUNING_PLUGIN_NAME, wavFilePath, "-o", tempFilePath };
		try {
			Process p = Runtime.getRuntime().exec(cmd);
	        p.waitFor();
	        LOG.error(IOUtils.toString(p.getErrorStream()));
	        Scanner scanner = new Scanner(new File(tempFilePath));
			String s = scanner.nextLine();
			String[] strings = s.split(" ");
			freq = Double.parseDouble(strings[3]);
		} catch (Exception e) {
			LOG.warn("Error when detecting tuning frequency, default value 440 Hz will be used", e);
			freq = 440;
		}
		LOG.info(String.format("Tuning frequency: %f Hz", freq));
	}

	public double getTuningFrequency() {
		return freq;
	}

}
