package edu.kuacm.expo2014.activities;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;

import com.viewpagerindicator.PageIndicator;

import edu.kuacm.expo2014.R;
import edu.kuacm.expo2014.db.DatabaseManager;
import edu.kuacm.expo2014.fragments.EventDetailsFragment;
import edu.kuacm.expo2014.loaders.TrackScheduleLoader;
import edu.kuacm.expo2014.model.Day;
import edu.kuacm.expo2014.model.Track;

/**
 * Event view of the track schedule; allows to slide between events of the same track using a ViewPager.
 * 
 * @author Christophe Beyls
 * 
 */
public class TrackScheduleEventActivity extends ActionBarActivity implements LoaderCallbacks<Cursor> {

	public static final String EXTRA_DAY = "day";
	public static final String EXTRA_TRACK = "track";
	public static final String EXTRA_POSITION = "position";

	private static final int EVENTS_LOADER_ID = 1;

	private Day mDay;
	private Track mTrack;
	private int mInitialPosition = -1;
	private View mProgress;
	private ViewPager mPager;
	private PageIndicator mPageIndicator;
	private TrackScheduleEventAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.track_schedule_event);

		Bundle extras = getIntent().getExtras();
		mDay = extras.getParcelable(EXTRA_DAY);
		mTrack = extras.getParcelable(EXTRA_TRACK);

		mProgress = findViewById(R.id.progress);
		mPager = (ViewPager) findViewById(R.id.pager);
		mAdapter = new TrackScheduleEventAdapter(getSupportFragmentManager());
		mPageIndicator = (PageIndicator) findViewById(R.id.indicator);

		if (savedInstanceState == null) {
			mInitialPosition = extras.getInt(EXTRA_POSITION, -1);
			mPager.setAdapter(mAdapter);
			mPageIndicator.setViewPager(mPager);
		}

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.event_details);
		bar.setSubtitle(mTrack.getName());

		setCustomProgressVisibility(true);
		getSupportLoaderManager().initLoader(EVENTS_LOADER_ID, null, this);
	}

	private void setCustomProgressVisibility(boolean isVisible) {
		mProgress.setVisibility(isVisible ? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new TrackScheduleLoader(this, mDay, mTrack);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		setCustomProgressVisibility(false);

		if (data != null) {
			mAdapter.setCursor(data);

			// Delay setting the adapter when the instance state is restored
			// to ensure the current position is restored properly
			if (mPager.getAdapter() == null) {
				mPager.setAdapter(mAdapter);
				mPageIndicator.setViewPager(mPager);
			}

			if (mInitialPosition != -1) {
				mPager.setCurrentItem(mInitialPosition, false);
				mInitialPosition = -1;
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.setCursor(null);
	}

	public static class TrackScheduleEventAdapter extends FragmentStatePagerAdapter {

		private Cursor mCursor;

		public TrackScheduleEventAdapter(FragmentManager fm) {
			super(fm);
		}

		public Cursor getCursor() {
			return mCursor;
		}

		public void setCursor(Cursor cursor) {
			mCursor = cursor;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return (mCursor == null) ? 0 : mCursor.getCount();
		}

		@Override
		public Fragment getItem(int position) {
			mCursor.moveToPosition(position);
			return EventDetailsFragment.newInstance(DatabaseManager.toEvent(mCursor));
		}
	}
}