package edu.kuacm.expo.fragments;

import edu.kuacm.expo.R;
import edu.kuacm.expo.activities.TrackScheduleActivity;
import edu.kuacm.expo.db.DatabaseManager;
import edu.kuacm.expo.loaders.SimpleCursorLoader;
import edu.kuacm.expo.model.Day;
import edu.kuacm.expo.model.Track;
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
import android.widget.ListView;
import android.widget.TextView;

public class TracksListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int TRACKS_LOADER_ID = 1;
	private static final String ARG_DAY = "day";

	private Day mDay;
	private TracksAdapter mAdapter;

	public static TracksListFragment newInstance(Day day) {
		TracksListFragment f = new TracksListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_DAY, day);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new TracksAdapter(getActivity());
		mDay = getArguments().getParcelable(ARG_DAY);
		setListAdapter(mAdapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.no_data));
		setListShown(false);

		getLoaderManager().initLoader(TRACKS_LOADER_ID, null, this);
	}

	private static class TracksLoader extends SimpleCursorLoader {

		private final Day mmDay;

		public TracksLoader(Context context, Day day) {
			super(context);
			mmDay = day;
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getTracks(mmDay);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new TracksLoader(getActivity(), mDay);
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
		Track track = mAdapter.getItem(position);
		Intent intent = new Intent(getActivity(), TrackScheduleActivity.class).putExtra(TrackScheduleActivity.EXTRA_DAY, mDay).putExtra(
				TrackScheduleActivity.EXTRA_TRACK, track);
		startActivity(intent);
	}

	private static class TracksAdapter extends CursorAdapter {

		private final LayoutInflater mmInflater;

		public TracksAdapter(Context context) {
			super(context, null, 0);
			mmInflater = LayoutInflater.from(context);
		}

		@Override
		public Track getItem(int position) {
			return DatabaseManager.toTrack((Cursor) super.getItem(position));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = mmInflater.inflate(android.R.layout.simple_list_item_2, parent, false);

			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(android.R.id.text1);
			holder.type = (TextView) view.findViewById(android.R.id.text2);
			view.setTag(holder);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.track = DatabaseManager.toTrack(cursor, holder.track);
			holder.name.setText(holder.track.getName());
			holder.type.setText(holder.track.getType().getNameResId());
		}

		private static class ViewHolder {
			TextView name;
			TextView type;
			Track track;
		}
	}
}
