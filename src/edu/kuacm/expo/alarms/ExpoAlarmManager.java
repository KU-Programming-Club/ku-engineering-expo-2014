package edu.kuacm.expo.alarms;

import edu.kuacm.expo.db.DatabaseManager;
import edu.kuacm.expo.fragments.SettingsFragment;
import edu.kuacm.expo.services.AlarmIntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

/**
 * This class monitors bookmarks and preferences changes to dispatch alarm update work to AlarmIntentService.
 * 
 * @author Christophe Beyls
 * 
 */
public class ExpoAlarmManager implements OnSharedPreferenceChangeListener {

	private static ExpoAlarmManager sInstance;

	private Context mContext;
	private boolean mIsEnabled;

	private final BroadcastReceiver mScheduleRefreshedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// When the schedule DB is updated, update the alarms too
			startUpdateAlarms();
		}
	};

	private final BroadcastReceiver mBookmarksReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Dispatch the Bookmark broadcasts to the service
			Intent serviceIntent = new Intent(context, AlarmIntentService.class);
			serviceIntent.setAction(intent.getAction());
			serviceIntent.putExtras(intent.getExtras());
			context.startService(serviceIntent);
		}
	};

	public static void init(Context context) {
		if (sInstance == null) {
			sInstance = new ExpoAlarmManager(context);
		}
	}

	public static ExpoAlarmManager getInstance() {
		return sInstance;
	}

	private ExpoAlarmManager(Context context) {
		mContext = context;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		mIsEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED, false);
		if (mIsEnabled) {
			registerReceivers();
		}
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public boolean isEnabled() {
		return mIsEnabled;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED.equals(key)) {
			mIsEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED, false);
			if (mIsEnabled) {
				registerReceivers();
				startUpdateAlarms();
			} else {
				unregisterReceivers();
				startDisableAlarms();
			}
		} else if (SettingsFragment.KEY_PREF_NOTIFICATIONS_DELAY.equals(key)) {
			startUpdateAlarms();
		}
	}

	private void registerReceivers() {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
		lbm.registerReceiver(mScheduleRefreshedReceiver, new IntentFilter(DatabaseManager.ACTION_SCHEDULE_REFRESHED));
		IntentFilter filter = new IntentFilter();
		filter.addAction(DatabaseManager.ACTION_ADD_BOOKMARK);
		filter.addAction(DatabaseManager.ACTION_REMOVE_BOOKMARKS);
		lbm.registerReceiver(mBookmarksReceiver, filter);
	}

	private void unregisterReceivers() {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
		lbm.unregisterReceiver(mScheduleRefreshedReceiver);
		lbm.unregisterReceiver(mBookmarksReceiver);
	}

	private void startUpdateAlarms() {
		Intent serviceIntent = new Intent(mContext, AlarmIntentService.class);
		serviceIntent.setAction(AlarmIntentService.ACTION_UPDATE_ALARMS);
		mContext.startService(serviceIntent);
	}

	private void startDisableAlarms() {
		Intent serviceIntent = new Intent(mContext, AlarmIntentService.class);
		serviceIntent.setAction(AlarmIntentService.ACTION_DISABLE_ALARMS);
		mContext.startService(serviceIntent);
	}
}