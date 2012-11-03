package chordest.chord.templates;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import chordest.model.Chord;
import chordest.util.DataUtil;

public class TemplatesSmoother {

	public static Map<Chord, double[]> smoothTemplates(double[][] pcp, Chord[] chords) {
		if (pcp == null) {
			throw new NullPointerException("pcp array is null");
		}
		if (chords == null) {
			throw new NullPointerException("chords array is null");
		}
		if (pcp.length != chords.length) {
			throw new IllegalArgumentException("pcp.length != chords.length: " + pcp.length + " != " + chords.length);
		}
		Map<Chord, double[]> result = new HashMap<Chord, double[]>();
		int length = pcp.length;
		for (int i = 0; i < length; i++) {
			double[] v = Arrays.copyOf(pcp[i], pcp[i].length);
//			DataUtil.scaleTo01(v);
			Chord c = chords[i];
			double[] t = result.get(c);
			if (t == null) {
				t = v;
			} else {
				t = DataUtil.add(t, v);
			}
			result.put(c, t);
		}
		for (Chord c : result.keySet()) {
			DataUtil.scaleTo01(result.get(c));
		}
		return result;
	}

}
