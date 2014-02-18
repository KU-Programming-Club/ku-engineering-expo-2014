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

public class ExpoApplication extends Application {

	/**
	 * An {@link Activity} can spawn any number of {@link android.os.AsyncTask AsyncTasks}
	 * simultaneously, so use a {@link Map} to connect an {@code Activity}'s name and
	 * its {@link AsyncActivityTask AsyncActivityTasks}.
	 */
	private final Map<String, List<? extends AsyncActivityTask<? extends Activity,?,?,?>>> mActivityTaskMap =
			new HashMap<String, List<? extends AsyncActivityTask<? extends Activity,?,?,?>>>();

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
	public <A extends Activity> void removeTask(A activity, AsyncActivityTask<A,?,?,?> task) {
		String key = activity.getClass().getName();
		List<? extends AsyncActivityTask<? extends Activity,?,?,?>> tasks = mActivityTaskMap.get(key);
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
	public <A extends Activity> void addTask(A activity, AsyncActivityTask<A,?,?,?> task) {
		String key = activity.getClass().getName();
		@SuppressWarnings("unchecked")
		List<AsyncActivityTask<A,?,?,?>> tasks = (List<AsyncActivityTask<A, ?, ?, ?>>) mActivityTaskMap.get(key);
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
	public void detachActivity(Activity activity) {
		List<? extends AsyncActivityTask<? extends Activity,?,?,?>> tasks = mActivityTaskMap.get(activity.getClass().getName());
		if (tasks != null) {
			for (AsyncActivityTask<? extends Activity,?,?,?> task : tasks) {
				task.detachActivity();
			}
		}
	}

	/**
	 * Reestablishes the connection between an {@link Activity} and its {@link AsyncActivityTask
	 * AsyncActivityTasks} after the {@code Activity} is resumed.
	 * @param activity The {@code Activity} whose references should be reestablished.
	 */
	public <A extends Activity> void attachActivity(A activity) {
		List<? extends AsyncActivityTask<? extends Activity,?,?,?>> tasks = mActivityTaskMap.get(activity.getClass().getName());
		if (tasks != null) {
			for (AsyncActivityTask<? extends Activity,?,?,?> task : tasks) {
				@SuppressWarnings("unchecked")
				AsyncActivityTask<A,?,?,?> castTask = (AsyncActivityTask<A, ?, ?, ?>) task;
				castTask.attachActivity(activity);
			}
		}
	}
}