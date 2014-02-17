package edu.kuacm.expo2014.fragments;

import edu.kuacm.expo2014.R;
import edu.kuacm.expo2014.activities.PresenterInfoActivity;
import edu.kuacm.expo2014.db.DatabaseManager;
import edu.kuacm.expo2014.loaders.SimpleCursorLoader;
import edu.kuacm.expo2014.model.Presenter;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class PresentersListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int PRESENTERS_LOADER_ID = 1;

	private PresentersAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new PresentersAdapter(getActivity());
		setListAdapter(mAdapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setFastScrollEnabled(true);
		setEmptyText(getString(R.string.no_data));
		setListShown(false);

		getLoaderManager().initLoader(PRESENTERS_LOADER_ID, null, this);
	}

	private static class PresentersLoader extends SimpleCursorLoader {

		public PresentersLoader(Context context) {
			super(context);
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getPresenters();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new PresentersLoader(getActivity());
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
		Presenter presenter = mAdapter.getItem(position);
		Intent intent = new Intent(getActivity(), PresenterInfoActivity.class).putExtra(PresenterInfoActivity.EXTRA_PRESENTER, presenter);
		startActivity(intent);
	}

	private static class PresentersAdapter extends CursorAdapter implements SectionIndexer {

		private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		private final LayoutInflater mmInflater;
		private final AlphabetIndexer mmIndexer;

		public PresentersAdapter(Context context) {
			super(context, null, 0);
			mmInflater = LayoutInflater.from(context);
			mmIndexer = new AlphabetIndexer(null, DatabaseManager.PRESENTER_NAME_COLUMN_INDEX, ALPHABET);
		}

		@Override
		public Presenter getItem(int position) {
			return DatabaseManager.toPresenter((Cursor) super.getItem(position));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = mmInflater.inflate(android.R.layout.simple_list_item_1, parent, false);

			ViewHolder holder = new ViewHolder();
			holder.textView = (TextView) view.findViewById(android.R.id.text1);
			view.setTag(holder);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.presenter = DatabaseManager.toPresenter(cursor, holder.presenter);
			holder.textView.setText(holder.presenter.getName());
		}

		@Override
		public Cursor swapCursor(Cursor newCursor) {
			mmIndexer.setCursor(newCursor);
			return super.swapCursor(newCursor);
		}

		@Override
		public int getPositionForSection(int sectionIndex) {
			return mmIndexer.getPositionForSection(sectionIndex);
		}

		@Override
		public int getSectionForPosition(int position) {
			return mmIndexer.getSectionForPosition(position);
		}

		@Override
		public Object[] getSections() {
			return mmIndexer.getSections();
		}

		private static class ViewHolder {
			public TextView textView;
			public Presenter presenter;
		}
	}
}