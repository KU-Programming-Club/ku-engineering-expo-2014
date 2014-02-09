package edu.kuacm.expo.api;

import java.util.Locale;
import java.util.Map;

/**
 * This class contains all Expo URLs
 * 
 * @author Christophe Beyls
 * 
 */
public class ExpoUrls {

	private static final Map<String, String> mPresenterUrlMap = buildPresenterUrlMap();
	
	private static final String SCHEDULE_URL = "https://raw.github.com/RyanGlScott/ku-engineering-expo-base/master/assets/events.txt";
	private static final String EVENT_URL_FORMAT = "https://fosdem.org/%1$d/schedule/event/%2$s/";
	private static final String PRESENTER_URL_FORMAT = "https://fosdem.org/%1$d/schedule/speaker/%2$s/";

	public static String getScheduleUrl() {
		return SCHEDULE_URL;
	}

	public static String getEventUrl(String slug, int year) {
		return String.format(Locale.US, EVENT_URL_FORMAT, year, slug);
	}

	public static String getPresenterUrl(String slug, int year) {
		return String.format(Locale.US, PRESENTER_URL_FORMAT, year, slug);
	}
	
	private static Map<String, String> buildPresenterUrlMap() {
		return null;
	}
}