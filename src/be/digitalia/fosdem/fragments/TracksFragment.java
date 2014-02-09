package be.digitalia.fosdem.fragments;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.GlobalCacheLoader;
import be.digitalia.fosdem.model.Day;

import com.astuetz.PagerSlidingTabStrip;

public class TracksFragment extends Fragment implements LoaderCallbacks<List<Day>> {

	private static class ViewHolder {
		View contentView;
		View emptyView;
		ViewPager pager;
		PagerSlidingTabStrip indicator;
	}

	private static final int DAYS_LOADER_ID = 1;
	private static final String PREF_CURRENT_PAGE = "tracks_current_page";

	private DaysAdapter mDaysAdapter;
	private ViewHolder mHolder;
	private int mSavedCurrentPage = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDaysAdapter = new DaysAdapter(getChildFragmentManager());

		if (savedInstanceState == null) {
			// Restore the current page from preferences
			mSavedCurrentPage = getActivity().getPreferences(Context.MODE_PRIVATE).getInt(PREF_CURRENT_PAGE, -1);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_tracks, container, false);

		mHolder = new ViewHolder();
		mHolder.contentView = view.findViewById(R.id.content);
		mHolder.emptyView = view.findViewById(android.R.id.empty);
		mHolder.pager = (ViewPager) view.findViewById(R.id.pager);
		mHolder.indicator = (PagerSlidingTabStrip) view.findViewById(R.id.indicator);

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mHolder = null;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().initLoader(DAYS_LOADER_ID, null, this);
	}

	@Override
	public void onStop() {
		super.onStop();
		// Save the current page to preferences if it has changed
		final int page = mHolder.pager.getCurrentItem();
		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		if (prefs.getInt(PREF_CURRENT_PAGE, -1) != page) {
			prefs.edit().putInt(PREF_CURRENT_PAGE, page).commit();
		}
	}

	private static class DaysLoader extends GlobalCacheLoader<List<Day>> {

		private final BroadcastReceiver scheduleRefreshedReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				onContentChanged();
			}
		};

		public DaysLoader(Context context) {
			super(context);
			// Reload days list when the schedule has been refreshed
			LocalBroadcastManager.getInstance(context).registerReceiver(scheduleRefreshedReceiver, new IntentFilter(DatabaseManager.ACTION_SCHEDULE_REFRESHED));
		}

		@Override
		protected void onReset() {
			super.onReset();
			LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(scheduleRefreshedReceiver);
		}

		@Override
		protected List<Day> getCachedResult() {
			return DatabaseManager.getInstance().getCachedDays();
		}

		@Override
		public List<Day> loadInBackground() {
			return DatabaseManager.getInstance().getDays();
		}
	}

	@Override
	public Loader<List<Day>> onCreateLoader(int id, Bundle args) {
		return new DaysLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<Day>> loader, List<Day> data) {
		mDaysAdapter.setDays(data);

		final int totalPages = mDaysAdapter.getCount();
		if (totalPages == 0) {
			mHolder.contentView.setVisibility(View.GONE);
			mHolder.emptyView.setVisibility(View.VISIBLE);
			mHolder.pager.setOnPageChangeListener(null);
		} else {
			mHolder.contentView.setVisibility(View.VISIBLE);
			mHolder.emptyView.setVisibility(View.GONE);
			if (mHolder.pager.getAdapter() == null) {
				mHolder.pager.setAdapter(mDaysAdapter);
			}
			mHolder.indicator.setViewPager(mHolder.pager);
			if (mSavedCurrentPage != -1) {
				mHolder.pager.setCurrentItem(Math.min(mSavedCurrentPage, totalPages - 1), false);
				mSavedCurrentPage = -1;
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<List<Day>> loader) {
	}

	private static class DaysAdapter extends FragmentStatePagerAdapter {

		private List<Day> mDays;

		public DaysAdapter(FragmentManager fm) {
			super(fm);
		}

		public void setDays(List<Day> days) {
			if (mDays != days) {
				mDays = days;
				notifyDataSetChanged();
			}
		}

		@Override
		public int getCount() {
			return (mDays == null) ? 0 : mDays.size();
		}

		@Override
		public Fragment getItem(int position) {
			return TracksListFragment.newInstance(mDays.get(position));
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return mDays.get(position).toString();
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
			super.setPrimaryItem(container, position, object);
			// Hack to allow the non-primary fragments to start properly
			if (object != null) {
				((Fragment) object).setUserVisibleHint(false);
			}
		}
	}
}