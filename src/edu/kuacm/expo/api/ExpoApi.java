package edu.kuacm.expo.api;

import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.kuacm.expo.db.DatabaseManager;
import edu.kuacm.expo.model.Event;
import edu.kuacm.expo.parsers.EventsParser;
import edu.kuacm.expo.utils.HttpUtils;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Main API entry point.
 * 
 * @author Christophe Beyls
 * 
 */
public class ExpoApi {

	// Local broadcasts parameters
	public static final String ACTION_DOWNLOAD_SCHEDULE_PROGRESS = "edu.kuacm.expo.action.DOWNLOAD_SCHEDULE_PROGRESS";
	public static final String EXTRA_PROGRESS = "PROGRESS";
	public static final String ACTION_DOWNLOAD_SCHEDULE_RESULT = "edu.kuacm.expo.action.DOWNLOAD_SCHEDULE_RESULT";
	public static final String EXTRA_RESULT = "RESULT";

	public static final int RESULT_ERROR = -1;

	private static final Lock sScheduleLock = new ReentrantLock();

	/**
	 * Download & store the schedule to the database. Only one thread at a time will perform the actual action, the other ones will return immediately. The
	 * result will be sent back in the form of a local broadcast with an ACTION_DOWNLOAD_SCHEDULE_RESULT action.
	 * 
	 */
	public static void downloadSchedule(Context context) {
		if (!sScheduleLock.tryLock()) {
			// If a download is already in progress, return immediately
			return;
		}

		int result = RESULT_ERROR;
		try {
			InputStream is = HttpUtils.get(context, ExpoUrls.getSchedule(), ACTION_DOWNLOAD_SCHEDULE_PROGRESS, EXTRA_PROGRESS);
			try {
				Iterable<Event> events = new EventsParser().parse(is);
				result = DatabaseManager.getInstance().storeSchedule(events);
			} finally {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_DOWNLOAD_SCHEDULE_RESULT).putExtra(EXTRA_RESULT, result));
			sScheduleLock.unlock();
		}
	}
}