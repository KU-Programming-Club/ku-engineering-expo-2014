package edu.kuacm.expo2014.fragments;

import edu.kuacm.expo2014.R;
import edu.kuacm.expo2014.activities.EventDetailsActivity;
import edu.kuacm.expo2014.adapters.EventsAdapter;
import edu.kuacm.expo2014.db.DatabaseManager;
import edu.kuacm.expo2014.loaders.SimpleCursorLoader;
import edu.kuacm.expo2014.model.Event;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

public class SearchResultListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int EVENTS_LOADER_ID = 1;
	private static final String ARG_QUERY = "query";

	private EventsAdapter mAdapter;

	public static SearchResultListFragment newInstance(String query) {
		SearchResultListFragment f = new SearchResultListFragment();
		Bundle args = new Bundle();
		args.putString(ARG_QUERY, query);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new EventsAdapter(getActivity());
		setListAdapter(mAdapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.no_search_result));
		setListShown(false);

		getLoaderManager().initLoader(EVENTS_LOADER_ID, null, this);
	}

	private static class TextSearchLoader extends SimpleCursorLoader {

		private final String mmQuery;

		public TextSearchLoader(Context context, String query) {
			super(context);
			mmQuery = query;
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getSearchResults(mmQuery);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String query = getArguments().getString(ARG_QUERY);
		return new TextSearchLoader(getActivity(), query);
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