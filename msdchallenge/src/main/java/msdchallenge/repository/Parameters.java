package msdchallenge.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Utility to read parameters from a string of name-value pairs or an array
 * of string, each containing a name-value pair.
 */
public class Parameters {

	/**
	 * Construct the parameters from a string
	 * 
	 * @param allNameValuePairs
	 *            A string of the form "param1=name1 param2=name2".
	 * @param pairSeparators
	 *            A list of characters that separate the name-value pairs,
	 *            e.g. <space><tab><cr><lf>
	 * @param nameValueSeparator
	 *            The character that separates the name from the values,
	 *            e.g. '='
	 */
	public Parameters(String allNameValuePairs, String pairSeparators,
			char nameValueSeparator) {
		StringTokenizer tokeniser = new StringTokenizer(allNameValuePairs,
				pairSeparators);

		int numTokens = tokeniser.countTokens();
		String[] nameValuePairs = new String[numTokens];
		for (int i = 0; i < numTokens; ++i)
			nameValuePairs[i] = tokeniser.nextToken();
		parseNameValuePairs(nameValuePairs, nameValueSeparator, true);
	}

	/**
	 * Construct the parameters from an array of name-value pairs, e.g. from
	 * "main( String[] args )"
	 * 
	 * @param nameValuePairs
	 *            The array of name-value pairs
	 * @param separator
	 *            The character that separates the name from its value
	 */
	public Parameters(String[] nameValuePairs, char separator) {
		parseNameValuePairs(nameValuePairs, separator, true);
	}

	/**
	 * Construct the parameters from an array of name-value pairs using
	 * equals '=' as the separator.
	 * 
	 * @param nameValuePairs
	 *            The array of name-value pairs
	 */
	public Parameters(String[] nameValuePairs) {
		parseNameValuePairs(nameValuePairs, '=', true);
	}

	/**
	 * Get the value associated with a parameter.
	 * 
	 * @param name
	 *            The name of the parameter.
	 * @return The value associated with the parameter.
	 */
	public String getValue(String name) {
		return mParameters.get(name);
	}

	/**
	 * Get the value associated with a parameter or return the given default
	 * if it is not available.
	 * 
	 * @param name
	 *            The name of the parameter.
	 * @param defaultValue
	 *            The default value to return.
	 * @return The value associated with the parameter.
	 */
	public String getValue(String name, String defaultValue) {
		String value = getValue(name);

		if (value == null)
			value = defaultValue;

		return value;
	}

	/**
	 * Associate the given value with the given parameter name.
	 * 
	 * @param name
	 *            The name of the parameter.
	 * @param value
	 *            The value of the parameter.
	 */
	public void setValue(String name, String value) {
		mParameters.put(name.trim().toLowerCase(), value);
	}

	/**
	 * Set a default value, i.e. set this parameter to have the given value
	 * ONLY if it has not already been set.
	 * 
	 * @param name
	 *            The name of the parameter.
	 * @param value
	 *            The value of the parameter.
	 */
	public void setDefaultValue(String name, String value) {
		if (getValue(name) == null)
			setValue(name, value);
	}

	/**
	 * The parse method that accepts an array of name-value pairs.
	 * 
	 * @param nameValuePairs
	 *            An array of name-value pairs, where each string is of the
	 *            form: "<name>'separator'<value>"
	 * @param separator
	 *            The character that separates the name from the value
	 * @param overWrite
	 *            true if the parsed values should overwrite existing value
	 */
	public void parseNameValuePairs(String[] nameValuePairs,
			char separator, boolean overWrite) {
		for (String pair : nameValuePairs) {
			int pos = pair.indexOf(separator);
			if (pos < 0)
				throw new IllegalArgumentException(
						"Invalid name-value pair '" + pair
								+ "', expected <name>" + separator
								+ "<value>");
			String name = pair.substring(0, pos).toLowerCase();
			String value = pair.substring(pos + 1);
			if (overWrite)
				setValue(name, value);
			else
				setDefaultValue(name, value);
		}
	}

	/**
	 * Get the name-value pairs as a Map<String,String>
	 * 
	 * @return
	 */
	public Map<String, String> getParameters() {
		return mParameters;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		List<String> keys = new ArrayList<String>(mParameters.keySet());
		Collections.sort(keys);

		for (String key : keys) {
			result.append(key).append('=').append(mParameters.get(key))
					.append(System.getProperties().get("line.separator"));
		}

		return result.toString();
	}

	private final Map<String, String> mParameters = new HashMap<String, String>();
}
