package edu.kuacm.expo.fragments;

import edu.kuacm.expo.R;
import edu.kuacm.expo.activities.EventDetailsActivity;
import edu.kuacm.expo.adapters.EventsAdapter;
import edu.kuacm.expo.db.DatabaseManager;
import edu.kuacm.expo.loaders.SimpleCursorLoader;
import edu.kuacm.expo.model.Event;
import edu.kuacm.expo.model.Person;
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

public class PersonInfoListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int PERSON_EVENTS_LOADER_ID = 1;
	private static final String ARG_PERSON = "person";

	private Person mPerson;
	private EventsAdapter mAdapter;

	public static PersonInfoListFragment newInstance(Person person) {
		PersonInfoListFragment f = new PersonInfoListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_PERSON, person);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new EventsAdapter(getActivity());
		mPerson = getArguments().getParcelable(ARG_PERSON);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.person, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.more_info:
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mPerson.getUrl()));
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

		View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.header_person_info, null);
		((TextView) headerView.findViewById(R.id.title)).setText(mPerson.getName());
		getListView().addHeaderView(headerView, null, false);

		setListAdapter(mAdapter);
		setListShown(false);

		getLoaderManager().initLoader(PERSON_EVENTS_LOADER_ID, null, this);
	}

	private static class PersonEventsLoader extends SimpleCursorLoader {

		private final Person mmPerson;

		public PersonEventsLoader(Context context, Person person) {
			super(context);
			mmPerson = person;
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getEvents(mmPerson);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new PersonEventsLoader(getActivity(), mPerson);
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