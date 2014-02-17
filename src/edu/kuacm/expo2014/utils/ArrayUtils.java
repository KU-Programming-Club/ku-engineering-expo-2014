package edu.kuacm.expo2014.utils;

public final class ArrayUtils {
	
	private ArrayUtils() {}

	public static int indexOf(long[] array, long value) {
		for (int i = 0; i < array.length; ++i) {
			if (array[i] == value) {
				return i;
			}
		}
		return -1;
	}
}