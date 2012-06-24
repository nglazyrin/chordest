package similarity.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MapUtil {

	public static <K,V> List<Entry<K, V>> sortMapByValue(Map<K, V> map, boolean ascending) {
		List<Entry<K, V>> sorted = new ArrayList<Entry<K, V>>(map.entrySet());
		final int sign = ascending ? 1 : -1;
		Collections.sort(sorted, new Comparator<Entry<K, V>>() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return sign * ((Comparable<V>)o1.getValue()).compareTo(o2.getValue());
			}}
		);
		return sorted;
	}

	public static <K,V> List<K> getKeysSortedByValue(Map<K, V> map, boolean ascending) {
		List<Entry<K, V>> sorted = sortMapByValue(map, ascending);
		List<K> result = new ArrayList<K>(sorted.size());
		for (Entry<K, V> entry : sorted) {
			result.add(entry.getKey());
		}
		return result;
	}

	public static <K, V> List<Entry<K, V>> getEntriesWithLargestValue(
			Map<K, V> notesPerMode) {
		List<Entry<K, V>> sorted = sortMapByValue(notesPerMode, false);
		Entry<K, V> current = sorted.get(0);
		V topResult = current.getValue();
		int modesWithTopValue = 0;
		while (current.getValue().equals(topResult)) {
			modesWithTopValue++;
			current = sorted.get(modesWithTopValue);
		}
		return sorted.subList(0, modesWithTopValue);
	}

}
