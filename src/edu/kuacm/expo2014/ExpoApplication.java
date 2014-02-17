package edu.kuacm.expo2014;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Application;
import android.preference.PreferenceManager;
import edu.kuacm.expo2014.alarms.ExpoAlarmManager;
import edu.kuacm.expo2014.db.DatabaseManager;

public class ExpoApplication<A extends Activity> extends Application {
	
	/**
	 * An {@link Activity} can spawn any number of {@link android.os.AsyncTask AsyncTasks}
	 * simultaneously, so use a {@link Map} to connect an {@code Activity}'s name and
	 * its {@link AsyncActivityTask AsyncActivityTasks}.
	 */
	private Map<String, List<AsyncActivityTask<A,?,?,?>>> mActivityTaskMap =
			new HashMap<String, List<AsyncActivityTask<A,?,?,?>>>();

	@Override
	public void onCreate() {
		super.onCreate();

		DatabaseManager.init(this);
		// Initialize settings
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		// Alarms (requires settings)
		ExpoAlarmManager.init(this);
	}

	/**
	 * Removes unused {@link AsyncActivityTask AsyncActivityTasks} after they have
	 * completed execution.
	 * @param activity The {@link Activity} that spawned the {@code AsyncActivityTask}.
	 * @param task The {@code AsyncActivityTask} to remove.
	 */
	public void removeTask(A activity, AsyncActivityTask<A,?,?,?> task) {
		String key = activity.getClass().getName();
		List<AsyncActivityTask<A,?,?,?>> tasks = mActivityTaskMap.get(key);
		tasks.remove(task);
		if (tasks.isEmpty()) {
			mActivityTaskMap.remove(key);
		}
	}

	/**
	 * Establishes a connection between an {@link Activity} and a {@link AsyncActivityTask}
	 * that will persist through device rotation or standby.
	 * @param activity The {@code Activity} that spawned the {@code AsyncActivityTask}.
	 * @param task The {@code AsyncActivityTask} to connect.
	 */
	public void addTask(A activity, AsyncActivityTask<A,?,?,?> task) {
		String key = activity.getClass().getName();
		List<AsyncActivityTask<A,?,?,?>> tasks = mActivityTaskMap.get(key);
		if (tasks == null) {
			tasks = new ArrayList<AsyncActivityTask<A,?,?,?>>();
			mActivityTaskMap.put(key, tasks);
		}

		tasks.add(task);
	}

	/**
	 * While an {@link Activity} rotates or is in standby, attempting to call an {@code Activity}
	 * method from one of its {@link AsyncActivityTask AsyncActivityTasks} can produce
	 * unexpected results. Use this method to set all of an {@code Activity}'s references
	 * in its tasks to {@code null} so that the tasks can work around rotation or standby.
	 * @param activity The {@code Activity} whose references should be set to null.
	 */
	public void detachActivity(A activity) {
		List<AsyncActivityTask<A,?,?,?>> tasks = mActivityTaskMap.get(activity.getClass().getName());
		if (tasks != null) {
			for (AsyncActivityTask<A,?,?,?> task : tasks) {
				task.setActivity(null);
			}
		}
	}

	/**
	 * Reestablishes the connection between an {@link Activity} and its {@link AsyncActivityTask
	 * AsyncActivityTasks} after the {@code Activity} is resumed.
	 * @param activity The {@code Activity} whose references should be reestablished.
	 */
	public void attachActivity(A activity) {
		List<AsyncActivityTask<A,?,?,?>> tasks = mActivityTaskMap.get(activity.getClass().getName());
		if (tasks != null) {
			for (AsyncActivityTask<A,?,?,?> task : tasks) {
				task.setActivity(activity);
			}
		}
	}
	
}