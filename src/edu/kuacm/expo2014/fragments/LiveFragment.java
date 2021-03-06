package edu.kuacm.expo2014.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import edu.kuacm.expo2014.R;

public class LiveFragment extends Fragment {

	private LivePagerAdapter mLivePagerAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLivePagerAdapter = new LivePagerAdapter(getChildFragmentManager(), getResources());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live, container, false);

		ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
		pager.setAdapter(mLivePagerAdapter);
		PagerSlidingTabStrip indicator = (PagerSlidingTabStrip) view.findViewById(R.id.indicator);
		indicator.setViewPager(pager);

		return view;
	}

	private static class LivePagerAdapter extends FragmentPagerAdapter {

		private final Resources mmResources;

		public LivePagerAdapter(FragmentManager fm, Resources resources) {
			super(fm);
			mmResources = resources;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return new NextLiveListFragment();
			case 1:
				return new NowLiveListFragment();
			}
			return null;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return mmResources.getString(R.string.next);
			case 1:
				return mmResources.getString(R.string.now);
			}
			return null;
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