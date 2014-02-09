package edu.kuacm.expo.loaders;

import edu.kuacm.expo.db.DatabaseManager;
import edu.kuacm.expo.model.Day;
import edu.kuacm.expo.model.Track;
import android.content.Context;
import android.database.Cursor;

public class TrackScheduleLoader extends SimpleCursorLoader {

	private final Day mDay;
	private final Track mTrack;

	public TrackScheduleLoader(Context context, Day day, Track track) {
		super(context);
		mDay = day;
		mTrack = track;
	}

	@Override
	protected Cursor getCursor() {
		return DatabaseManager.getInstance().getEvents(mDay, mTrack);
	}
}