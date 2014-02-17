package edu.kuacm.expo2014.loaders;

import edu.kuacm.expo2014.db.DatabaseManager;
import edu.kuacm.expo2014.model.Day;
import edu.kuacm.expo2014.model.Track;
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