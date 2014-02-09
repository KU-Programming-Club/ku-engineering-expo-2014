package be.digitalia.fosdem.loaders;

import android.content.Context;
import android.os.Handler;

/**
 * A cursor loader which also automatically refreshes its data at a specified interval.
 * 
 * @author Christophe Beyls
 * 
 */
public abstract class BaseLiveLoader extends SimpleCursorLoader {

	private static final long REFRESH_INTERVAL = 60L * 1000L; // 1 minute

	private final Handler mHandler;
	private final Runnable mTimeoutRunnable = new Runnable() {

		@Override
		public void run() {
			onContentChanged();
		}
	};

	public BaseLiveLoader(Context context) {
		super(context);
		mHandler = new Handler();
	}

	@Override
	protected void onForceLoad() {
		super.onForceLoad();
		mHandler.removeCallbacks(mTimeoutRunnable);
		mHandler.postDelayed(mTimeoutRunnable, REFRESH_INTERVAL);
	}

	@Override
	protected void onReset() {
		super.onReset();
		mHandler.removeCallbacks(mTimeoutRunnable);
	}
}