package io.mosip.credentialstore.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Position {
	RIGHT_THUMB("Right Thumb", 1), RIGHT_INDEXFINGER("Right IndexFinger", 2),
	RIGHT_MIDDLEFINGER("Right MiddleFinger", 3), RIGHT_RINGFINGER("Right RingFinger", 4),
	RIGHT_LITTLEFINGER("Right LittleFinger", 5), LEFT_THUMB("Left Thumb", 6), LEFT_INDEXFINGER("Left IndexFinger", 7),
	LEFT_MIDDLEFINGER("Left MiddleFinger", 8), LEFT_RINGFINGER("Left RingFinger", 9),
	LEFT_LITTLEFINGER("Left LittleFinger", 10);

	private String key;
	private int value;

	private static final Map<String, Integer> positionMap = Collections.unmodifiableMap(initializeMapping());

	private static Map<String, Integer> initializeMapping() {
		Map<String, Integer> failureMap = new HashMap<String, Integer>();
		for (Position s : Position.values()) {
			failureMap.put(s.key, s.value);
		}
		return failureMap;
	}

	Position(String key, int value) {
		this.key = key;
		this.value = value;
	}

	public static int getValueFromKey(String key) {
		return positionMap.get(key);
	}

}