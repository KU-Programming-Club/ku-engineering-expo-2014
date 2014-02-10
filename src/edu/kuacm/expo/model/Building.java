package edu.kuacm.expo.model;

import android.text.TextUtils;

public enum Building {
	EATON, LEARNED, M2SEC, SPAHR_LIBRARY, UNKNOWN;

	public static Building fromRoomName(String roomName) {
		if (!TextUtils.isEmpty(roomName)) {
			switch (Character.toUpperCase(roomName.charAt(0))) {
			case 'E':
				return EATON;
			case 'L':
				return LEARNED;
			case 'M':
				return M2SEC;
			case 'S':
				return SPAHR_LIBRARY;
			default:
				return UNKNOWN;
			}
		} else {
			return UNKNOWN;
		}
	}

	@Override
	public String toString() {
		switch (this) {
		case EATON:
			return "Eaton";
		case LEARNED:
			return "Learned";
		case M2SEC:
			return "M2SEC";
		case SPAHR_LIBRARY:
			return "Spahr Library";
		default:
			return "Unknown";
		}
	}
}