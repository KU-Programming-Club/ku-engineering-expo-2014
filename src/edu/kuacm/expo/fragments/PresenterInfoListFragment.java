package edu.kuacm.expo.fragments;

import edu.kuacm.expo.R;
import edu.kuacm.expo.activities.EventDetailsActivity;
import edu.kuacm.expo.adapters.EventsAdapter;
import edu.kuacm.expo.db.DatabaseManager;
import edu.kuacm.expo.loaders.SimpleCursorLoader;
import edu.kuacm.expo.model.Event;
import edu.kuacm.expo.model.Presenter;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class PresenterInfoListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int PRESENTER_EVENTS_LOADER_ID = 1;
	private static final String ARG_PRESENTER = "presenter";

	private Presenter mPresenter;
	private EventsAdapter mAdapter;

	public static PresenterInfoListFragment newInstance(Presenter presenter) {
		PresenterInfoListFragment f = new PresenterInfoListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_PRESENTER, presenter);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new EventsAdapter(getActivity());
		mPresenter = getArguments().getParcelable(ARG_PRESENTER);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.presenter, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.more_info:
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mPresenter.getUrl()));
			startActivity(intent);
			return true;
		}
		return false;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.no_data));

		int contentMargin = getResources().getDimensionPixelSize(R.dimen.content_margin);
		getListView().setPadding(contentMargin, contentMargin, contentMargin, contentMargin);

		View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.header_presenter_info, null);
		((TextView) headerView.findViewById(R.id.title)).setText(mPresenter.getName());
		getListView().addHeaderView(headerView, null, false);

		setListAdapter(mAdapter);
		setListShown(false);

		getLoaderManager().initLoader(PRESENTER_EVENTS_LOADER_ID, null, this);
	}

	private static class PresenterEventsLoader extends SimpleCursorLoader {

		private final Presenter mmPresenter;

		public PresenterEventsLoader(Context context, Presenter presenter) {
			super(context);
			mmPresenter = presenter;
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getEvents(mmPresenter);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new PresenterEventsLoader(getActivity(), mPresenter);
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
		Event event = mAdapter.getItem(position - 1);
		Intent intent = new Intent(getActivity(), EventDetailsActivity.class).putExtra(EventDetailsActivity.EXTRA_EVENT, event);
		startActivity(intent);
	}
}