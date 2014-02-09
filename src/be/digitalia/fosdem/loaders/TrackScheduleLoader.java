package be.digitalia.fosdem.loaders;

import android.content.Context;
import android.database.Cursor;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Track;

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