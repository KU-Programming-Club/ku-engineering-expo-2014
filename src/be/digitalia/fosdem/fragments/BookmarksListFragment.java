package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.widgets.BookmarksMultiChoiceModeListener;

/**
 * Bookmarks list, optionally filterable.
 * 
 * @author Christophe Beyls
 * 
 */
public class BookmarksListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int BOOKMARKS_LOADER_ID = 1;
	private static final String PREF_UPCOMING_ONLY = "bookmarks_upcoming_only";

	private EventsAdapter mAdapter;
	private boolean mUpcomingOnly;

	private MenuItem mFilterMenuItem;
	private MenuItem mUpcomingOnlyMenuItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new EventsAdapter(getActivity());
		setListAdapter(mAdapter);

		mUpcomingOnly = getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_UPCOMING_ONLY, false);

		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			BookmarksMultiChoiceModeListener.register(getListView());
		}

		setEmptyText(getString(R.string.no_bookmark));
		setListShown(false);

		getLoaderManager().initLoader(BOOKMARKS_LOADER_ID, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.bookmarks, menu);
		mFilterMenuItem = menu.findItem(R.id.filter);
		mUpcomingOnlyMenuItem = menu.findItem(R.id.upcoming_only);
		updateOptionsMenu();
	}

	private void updateOptionsMenu() {
		if (mFilterMenuItem != null) {
			mFilterMenuItem.setIcon(mUpcomingOnly ? R.drawable.ic_action_filter_selected : R.drawable.ic_action_filter);
			mUpcomingOnlyMenuItem.setChecked(mUpcomingOnly);
		}
	}

	@Override
	public void onDestroyOptionsMenu() {
		super.onDestroyOptionsMenu();
		mFilterMenuItem = null;
		mUpcomingOnlyMenuItem = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.upcoming_only:
			mUpcomingOnly = !mUpcomingOnly;
			updateOptionsMenu();
			getActivity().getPreferences(Context.MODE_PRIVATE).edit().putBoolean(PREF_UPCOMING_ONLY, mUpcomingOnly).commit();
			getLoaderManager().restartLoader(BOOKMARKS_LOADER_ID, null, this);
			return true;
		}
		return false;
	}

	private static class BookmarksLoader extends SimpleCursorLoader {

		// Events that just started are still shown for 5 minutes
		private static final long TIME_OFFSET = 5L * 60L * 1000L;

		private final boolean upcomingOnly;
		private final Handler handler;
		private final Runnable timeoutRunnable = new Runnable() {

			@Override
			public void run() {
				onContentChanged();
			}
		};

		public BookmarksLoader(Context context, boolean upcomingOnly) {
			super(context);
			this.upcomingOnly = upcomingOnly;
			this.handler = new Handler();
		}

		@Override
		public void deliverResult(Cursor cursor) {
			if (upcomingOnly && !isReset()) {
				handler.removeCallbacks(timeoutRunnable);
				// The loader will be refreshed when the start time of the first bookmark in the list is reached
				if ((cursor != null) && cursor.moveToFirst()) {
					long startTime = DatabaseManager.toEventStartTimeMillis(cursor);
					if (startTime != -1L) {
						long delay = startTime - (System.currentTimeMillis() - TIME_OFFSET);
						if (delay > 0L) {
							handler.postDelayed(timeoutRunnable, delay);
						} else {
							onContentChanged();
						}
					}
				}
			}
			super.deliverResult(cursor);
		}

		@Override
		protected void onReset() {
			super.onReset();
			if (upcomingOnly) {
				handler.removeCallbacks(timeoutRunnable);
			}
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getBookmarks(upcomingOnly ? System.currentTimeMillis() - TIME_OFFSET : -1L);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new BookmarksLoader(getActivity(), mUpcomingOnly);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			mAdapter.swapCursor(data);
		}

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Event event = mAdapter.getItem(position);
		Intent intent = new Intent(getActivity(), EventDetailsActivity.class).putExtra(EventDetailsActivity.EXTRA_EVENT, event);
		startActivity(intent);
	}
}