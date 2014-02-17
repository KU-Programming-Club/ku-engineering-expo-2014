package edu.kuacm.expo2014.activities;

import edu.kuacm.expo2014.R;
import edu.kuacm.expo2014.fragments.MessageDialogFragment;
import edu.kuacm.expo2014.fragments.SearchResultListFragment;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

public class SearchResultActivity extends ActionBarActivity {

	public static final int MIN_SEARCH_LENGTH = 3;

	private static final String STATE_CURRENT_QUERY = "current_query";

	private String mCurrentQuery;
	private SearchView mSearchView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.search_events);

		if (savedInstanceState == null) {
			handleIntent(getIntent(), false);
		} else {
			mCurrentQuery = savedInstanceState.getString(STATE_CURRENT_QUERY);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(STATE_CURRENT_QUERY, mCurrentQuery);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntent(intent, true);
	}

	private void handleIntent(Intent intent, boolean isNewIntent) {
		String intentAction = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(intentAction)) {
			// Normal search, results are displayed here
			String query = intent.getStringExtra(SearchManager.QUERY);
			if (query != null) {
				query = query.trim();
			}
			if ((query == null) || (query.length() < MIN_SEARCH_LENGTH)) {
				MessageDialogFragment.newInstance(R.string.error_title, R.string.search_length_error).show(getSupportFragmentManager());
				return;
			}

			mCurrentQuery = query;
			if (mSearchView != null) {
				// Force loosing the focus to prevent the suggestions from appearing
				mSearchView.clearFocus();
				mSearchView.setFocusable(false);
				mSearchView.setFocusableInTouchMode(false);

				mSearchView.setQuery(query, false);
			}

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
				// Legacy search mode for Eclair
				getSupportActionBar().setSubtitle(query);
			}

			SearchResultListFragment f = SearchResultListFragment.newInstance(query);
			getSupportFragmentManager().beginTransaction().replace(R.id.content, f).commit();

		} else if (Intent.ACTION_VIEW.equals(intentAction)) {
			// Search suggestion, dispatch to EventDetailsActivity
			Intent dispatchIntent = new Intent(this, EventDetailsActivity.class).setData(intent.getData());
			startActivity(dispatchIntent);

			if (!isNewIntent) {
				finish();
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search, menu);

		MenuItem searchMenuItem = menu.findItem(R.id.search);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			// Associate searchable configuration with the SearchView
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
			mSearchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
			mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			mSearchView.setIconifiedByDefault(false); // Always show the search view
			mSearchView.setQuery(mCurrentQuery, false);
		} else {
			// Legacy search mode for Eclair
			MenuItemCompat.setActionView(searchMenuItem, null);
			getSupportActionBar().setSubtitle(mCurrentQuery);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.search:
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
				return false;
			} else {
				// Legacy search mode for Eclair
				onSearchRequested();
				return true;
			}
		}
		return false;
	}
}