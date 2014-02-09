package edu.kuacm.expo;

import edu.kuacm.expo.alarms.ExpoAlarmManager;
import edu.kuacm.expo.db.DatabaseManager;
import android.app.Application;
import android.preference.PreferenceManager;

public class ExpoApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		DatabaseManager.init(this);
		// Initialize settings
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		// Alarms (requires settings)
		ExpoAlarmManager.init(this);
	}
}