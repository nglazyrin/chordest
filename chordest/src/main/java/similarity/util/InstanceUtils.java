package similarity.util;

import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import similarity.arff.WekaData;
import similarity.chord.Chord;
import similarity.spectrum.SpectrumData;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class InstanceUtils {

	private static final Logger LOG = LoggerFactory.getLogger(InstanceUtils.class);

	public static void appendInstances(Instances to, Instances from) {
		if (from == null || to == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		Enumeration<Instance> fromEnum = from.enumerateInstances();
		while (fromEnum.hasMoreElements()) {
			Instance next = fromEnum.nextElement();
			if (next != null) {
				to.add(next);
			}
		}
	}

	public static Instances subsetNotMuchThanFixedSize(Instances instances, int size) {
		if (size <= 0) { return instances; }
//		instances.sort(instances.classAttribute());
		FastVector chordLabels = WekaData.getChordLabels();
		int[] count = new int[chordLabels.size()];
		Instances subset = new Instances(size + " instances for each result",
				WekaData.getAttributes(), 0);
		@SuppressWarnings("unchecked")
		Enumeration<Instance> instanceEnum = instances.enumerateInstances();
		while (instanceEnum.hasMoreElements()) {
			Instance instance = instanceEnum.nextElement();
			// instance.stringValue(classAttribute) returns "D:maj", "D:min" etc
			String chordLabel = instance.stringValue(instances.classAttribute());
			int labelIndex = chordLabels.indexOf(chordLabel);
			if (count[labelIndex] < size) {
				subset.add(instance);
				instance.setDataset(subset);
			}
			count[labelIndex]++;
		}
		return subset;
	}

	public static Instances scaleInstances(Instances instances) {
		LOG.info(String.format("Scaling %d instances", instances.numInstances()));
		Normalize n = new Normalize();
		n.setScale(1.0);
		try {
			n.setInputFormat(instances);
			return Filter.useFilter(instances, n);
		} catch (Exception e) {
			LOG.error(String.format("Error scaling instances '%s'", instances.relationName()), e);
			return null;
		}
	}

	public static Instances scaleInstances(Instances instances, double divisor) {
		@SuppressWarnings("unchecked")
		Enumeration<Instance> enumeration =  instances.enumerateInstances();
		while (enumeration.hasMoreElements()) {
			Instance instance = enumeration.nextElement();
			for (int i = 0; i < instance.numAttributes(); i++) {
				if (instance.attribute(i).isNumeric() && instance.classIndex() != i) {
					double val = instance.value(i);
					instance.setValue(i, val / divisor);
				}
			}
		}
		return instances;
	}

	public static Instances scaleEach(Instances instances) {
		@SuppressWarnings("unchecked")
		Enumeration<Instance> enumeration = instances.enumerateInstances();
		while (enumeration.hasMoreElements()) {
			Instance instance = enumeration.nextElement();
			double max = 0;
			for (int i = 0; i < instance.numAttributes(); i++) {
				if (instance.attribute(i).isNumeric() && instance.classIndex() != i) {
					max = Math.max(max, instance.value(i));
				}
			}
			if (max > 0) {
				for (int i = 0; i < instance.numAttributes(); i++) {
					if (instance.attribute(i).isNumeric() && instance.classIndex() != i) {
						double val = instance.value(i);
						instance.setValue(i, val / max);
					}
				}
			}
		}
		return instances;
	}

	public static Instances buildClassified(SpectrumData spectrumData, double[] chordTimes,
			Chord[] chords) {
		if (chordTimes.length != chords.length) {
			throw new IllegalArgumentException("Lengths of chord time array and chord array " +
					"should be equal, but were: " + chordTimes.length + ", " + chords.length);
		}
		int notesInOctave = spectrumData.scaleInfo.getNotesInOctaveCount();
		Instances instances = new Instances("Data-set for " + spectrumData.wavFilePath,
				WekaData.getAttributes(), 0);
		instances.setClassIndex(instances.numAttributes() - 1);
		double[][] spectrum = spectrumData.spectrum;
		int step = 8;
		spectrum = DataUtil.shrink(spectrum, step);
		for (int i = 0; i < spectrum.length; i++) {
			double[] column = spectrum[i];
			double[] pcp = DataUtil.reduceTo12Notes(DataUtil.toPitchClassProfiles(column, notesInOctave));
			double beatTime = spectrumData.beatTimes[i * step];
			Chord chord = findChordForTime(beatTime, chordTimes, chords);
			Instance instance = WekaData.newClassifiedInstance(pcp, chord);
			if (instance != null) {
				instances.add(instance);
			}
		}
		return instances;
	}

	private static Chord findChordForTime(double time, double[] chordTimes, Chord[] chords) {
		double latestChordTime = chordTimes[chordTimes.length - 1];
		if (time >= latestChordTime) {
			return chords[chords.length - 1];
		} else {
			int chordIndex = 0;
			while (chordIndex < chordTimes.length - 1 && chordTimes[chordIndex] < time) {
				chordIndex++;
			}
			return chords[chordIndex - 1];
		}
	}

	public static Instances preserveWithValues(Instances instances, List<Chord> chords) {
		LOG.info(String.format("Scaling %d instances", instances.numInstances()));
		RemoveWithValues r = new RemoveWithValues();
		r.setInvertSelection(true);
		int[] indices = new int[chords.size()];
		for (int i = 0; i < chords.size(); i++) {
			indices[i] = WekaData.indexOf(chords.get(i));
		}
		r.setNominalIndicesArr(indices);
		try {
			r.setInputFormat(instances);
			return Filter.useFilter(instances, r);
		} catch (Exception e) {
			LOG.error(String.format("Error filtering instances '%s'", instances.relationName()), e);
			return null;
		}
	}

//	public static Instances buildUnclassified(CQTSpectrumHolder spectrum) {
//		int notesInOctave = spectrum.getNotesInOctaveCount();
//		Instances instances = new Instances("Training data-set", WekaData.getAttributes(), 0);
//		instances.setClassIndex(instances.numAttributes() - 1);
//		for (int i = 0; i < spectrum.getTransformsCount(); i++) {
//			double[] column = spectrum.getSpectrumForTransform(i);
//			double[] pcp = DataUtil.reduceTo12Notes(DataUtil.toPitchClassProfiles(column, notesInOctave));
//			Instance instance = WekaData.newUnclassifiedInstance(pcp);
//			if (instance != null) {
//				instances.add(instance);
//			}
//		}
//		return instances;
//	}

}
