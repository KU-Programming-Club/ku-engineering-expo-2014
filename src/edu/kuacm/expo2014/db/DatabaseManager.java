package edu.kuacm.expo2014.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.kuacm.expo2014.model.Day;
import edu.kuacm.expo2014.model.Event;
import edu.kuacm.expo2014.model.Link;
import edu.kuacm.expo2014.model.Presenter;
import edu.kuacm.expo2014.model.Track;
import edu.kuacm.expo2014.utils.DateUtils;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

/**
 * Here comes the badass SQL.
 * 
 * @author Christophe Beyls
 * 
 */
public class DatabaseManager {

	public static final String ACTION_SCHEDULE_REFRESHED = "edu.kuacm.expo2014.action.SCHEDULE_REFRESHED";
	public static final String ACTION_ADD_BOOKMARK = "edu.kuacm.expo2014.action.ADD_BOOKMARK";
	public static final String EXTRA_EVENT_ID = "event_id";
	public static final String EXTRA_EVENT_START_TIME = "event_start";
	public static final String ACTION_REMOVE_BOOKMARKS = "edu.kuacm.expo2014.action.REMOVE_BOOKMARKS";
	public static final String EXTRA_EVENT_IDS = "event_ids";

	private static final Uri URI_TRACKS = Uri.parse("sqlite://edu.kuacm.expo2014/tracks");
	private static final Uri URI_EVENTS = Uri.parse("sqlite://edu.kuacm.expo2014/events");

	private static final String DB_PREFS_FILE = "database";
	private static final String LAST_UPDATE_TIME_PREF = "last_update_time";

	private static DatabaseManager sInstance;

	private Context mContext;
	private DatabaseHelper mHelper;

	private List<Day> mCachedDays;
	private int mYear = -1;

	public static void init(Context context) {
		if (sInstance == null) {
			sInstance = new DatabaseManager(context);
		}
	}

	public static DatabaseManager getInstance() {
		return sInstance;
	}

	private DatabaseManager(Context context) {
		mContext = context;
		mHelper = new DatabaseHelper(context);
	}

	private static final String[] COUNT_PROJECTION = new String[] { "count(*)" };

	private static long queryNumEntries(SQLiteDatabase db, String table, String selection, String[] selectionArgs) {
		Cursor cursor = db.query(table, COUNT_PROJECTION, selection, selectionArgs, null, null, null);
		try {
			cursor.moveToFirst();
			return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	private static final String TRACK_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.TRACKS_TABLE_NAME + " (id, name, type) VALUES (?, ?, ?);";
	private static final String EVENT_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.EVENTS_TABLE_NAME
			+ " (id, day_index, start_time, end_time, room_name, slug, track_id, abstract, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
	private static final String EVENT_TITLES_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.EVENTS_TITLES_TABLE_NAME
			+ " (rowid, title, subtitle) VALUES (?, ?, ?);";
	private static final String EVENT_PRESENTER_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
			+ " (event_id, presenter_id) VALUES (?, ?);";
	// Ignore conflicts in case of existing presenter
	private static final String PRESENTER_INSERT_STATEMENT = "INSERT OR IGNORE INTO " + DatabaseHelper.PRESENTERS_TABLE_NAME + " (rowid, name) VALUES (?, ?);";
	private static final String LINK_INSERT_STATEMENT = "INSERT INTO " + DatabaseHelper.LINKS_TABLE_NAME + " (event_id, url, description) VALUES (?, ?, ?);";

	private static void bindString(SQLiteStatement statement, int index, String value) {
		if (value == null) {
			statement.bindNull(index);
		} else {
			statement.bindString(index, value);
		}
	}

	private SharedPreferences getSharedPreferences() {
		return mContext.getSharedPreferences(DB_PREFS_FILE, Context.MODE_PRIVATE);
	}

	/**
	 * 
	 * @return The last update time in milliseconds since EPOCH, or -1 if not available.
	 */
	public long getLastUpdateTime() {
		return getSharedPreferences().getLong(LAST_UPDATE_TIME_PREF, -1L);
	}

	/**
	 * Stores the schedule to the database.
	 * 
	 * @param events
	 * @return The number of events processed.
	 */
	public int storeSchedule(Iterable<Event> events) {
		boolean isComplete = false;

		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			// 1: Delete the previous schedule
			clearSchedule(db);

			// Compile the insert statements for the big tables
			final SQLiteStatement trackInsertStatement = db.compileStatement(TRACK_INSERT_STATEMENT);
			final SQLiteStatement eventInsertStatement = db.compileStatement(EVENT_INSERT_STATEMENT);
			final SQLiteStatement eventTitlesInsertStatement = db.compileStatement(EVENT_TITLES_INSERT_STATEMENT);
			final SQLiteStatement eventPresenterInsertStatement = db.compileStatement(EVENT_PRESENTER_INSERT_STATEMENT);
			final SQLiteStatement presenterInsertStatement = db.compileStatement(PRESENTER_INSERT_STATEMENT);
			final SQLiteStatement linkInsertStatement = db.compileStatement(LINK_INSERT_STATEMENT);

			// 2: Insert the events
			int totalEvents = 0;
			Map<Track, Long> tracks = new HashMap<Track, Long>();
			long nextTrackId = 0L;
			Set<Day> days = new HashSet<Day>(2);

			for (Event event : events) {
				// 2a: Retrieve or insert Track
				Track track = event.getTrack();
				Long trackId = tracks.get(track);
				if (trackId == null) {
					// New track
					nextTrackId++;
					trackId = nextTrackId;
					trackInsertStatement.clearBindings();
					trackInsertStatement.bindLong(1, nextTrackId);
					bindString(trackInsertStatement, 2, track.getName());
					bindString(trackInsertStatement, 3, track.getType().name());
					if (trackInsertStatement.executeInsert() != -1L) {
						tracks.put(track, trackId);
					}
				}

				// 2b: Insert main event
				eventInsertStatement.clearBindings();
				long eventId = event.getId();
				eventInsertStatement.bindLong(1, eventId);
				Day day = event.getDay();
				days.add(day);
				eventInsertStatement.bindLong(2, day.getIndex());
				Date time = event.getStartTime();
				if (time == null) {
					eventInsertStatement.bindNull(3);
				} else {
					eventInsertStatement.bindLong(3, time.getTime());
				}
				time = event.getEndTime();
				if (time == null) {
					eventInsertStatement.bindNull(4);
				} else {
					eventInsertStatement.bindLong(4, time.getTime());
				}
				bindString(eventInsertStatement, 5, event.getRoomName());
				bindString(eventInsertStatement, 6, event.getSlug());
				eventInsertStatement.bindLong(7, trackId);
				bindString(eventInsertStatement, 8, event.getAbstractText());
				bindString(eventInsertStatement, 9, event.getDescription());

				if (eventInsertStatement.executeInsert() != -1L) {
					// 2c: Insert fulltext fields
					eventTitlesInsertStatement.clearBindings();
					eventTitlesInsertStatement.bindLong(1, eventId);
					bindString(eventTitlesInsertStatement, 2, event.getTitle());
					bindString(eventTitlesInsertStatement, 3, event.getSubTitle());
					eventTitlesInsertStatement.executeInsert();

					// 2d: Insert presenters
					for (Presenter presenter : event.getPresenters()) {
						eventPresenterInsertStatement.clearBindings();
						eventPresenterInsertStatement.bindLong(1, eventId);
						long presenterId = presenter.getId();
						eventPresenterInsertStatement.bindLong(2, presenterId);
						eventPresenterInsertStatement.executeInsert();

						presenterInsertStatement.clearBindings();
						presenterInsertStatement.bindLong(1, presenterId);
						bindString(presenterInsertStatement, 2, presenter.getName());
						try {
							presenterInsertStatement.executeInsert();
						} catch (SQLiteConstraintException e) {
							// Older Android versions may not ignore an existing presenter
						}
					}

					// 2e: Insert links
					for (Link link : event.getLinks()) {
						linkInsertStatement.clearBindings();
						linkInsertStatement.bindLong(1, eventId);
						bindString(linkInsertStatement, 2, link.getUrl());
						bindString(linkInsertStatement, 3, link.getDescription());
						linkInsertStatement.executeInsert();
					}
				}

				totalEvents++;
			}

			// 3: Insert collected days
			ContentValues values = new ContentValues();
			for (Day day : days) {
				values.clear();
				values.put("_index", day.getIndex());
				Date date = day.getDate();
				values.put("date", (date == null) ? 0L : date.getTime());
				db.insert(DatabaseHelper.DAYS_TABLE_NAME, null, values);
			}

			// TODO purge outdated bookmarks ?

			db.setTransactionSuccessful();
			trackInsertStatement.close();
			eventInsertStatement.close();
			eventTitlesInsertStatement.close();
			eventPresenterInsertStatement.close();
			presenterInsertStatement.close();
			linkInsertStatement.close();
			isComplete = true;

			return totalEvents;
		} finally {
			db.endTransaction();
			db.close();

			if (isComplete) {
				// Clear cache
				mCachedDays = null;
				mYear = -1;
				// Set last update time
				getSharedPreferences().edit().putLong(LAST_UPDATE_TIME_PREF, System.currentTimeMillis()).commit();

				mContext.getContentResolver().notifyChange(URI_TRACKS, null);
				mContext.getContentResolver().notifyChange(URI_EVENTS, null);
				LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_SCHEDULE_REFRESHED));
			}
		}
	}

	public void clearSchedule() {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			clearSchedule(db);

			db.setTransactionSuccessful();

			mCachedDays = null;
			mYear = -1;
			getSharedPreferences().edit().remove(LAST_UPDATE_TIME_PREF).commit();
		} finally {
			db.endTransaction();
			db.close();

			mContext.getContentResolver().notifyChange(URI_TRACKS, null);
			mContext.getContentResolver().notifyChange(URI_EVENTS, null);
			LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_SCHEDULE_REFRESHED));
		}
	}

	private static void clearSchedule(SQLiteDatabase db) {
		db.delete(DatabaseHelper.EVENTS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.EVENTS_TITLES_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.PRESENTERS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.LINKS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.TRACKS_TABLE_NAME, null, null);
		db.delete(DatabaseHelper.DAYS_TABLE_NAME, null, null);
	}

	/**
	 * Returns the cached days list or null. Can be safely called on the main thread without blocking it.
	 * 
	 * @return
	 */
	public List<Day> getCachedDays() {
		return mCachedDays;
	}

	/**
	 * 
	 * @return The Days the events span to.
	 */
	public List<Day> getDays() {
		Cursor cursor = mHelper.getReadableDatabase().query(DatabaseHelper.DAYS_TABLE_NAME, new String[] { "_index", "date" }, null, null, null, null,
				"_index ASC");
		try {
			List<Day> result = new ArrayList<Day>(cursor.getCount());
			while (cursor.moveToNext()) {
				Day day = new Day();
				day.setIndex(cursor.getInt(0));
				day.setDate(new Date(cursor.getLong(1)));
				result.add(day);
			}
			mCachedDays = result;
			return result;
		} finally {
			cursor.close();
		}
	}

	public int getYear() {
		// Try to get the cached value first
		if (mYear != -1) {
			return mYear;
		}

		Calendar cal = Calendar.getInstance(DateUtils.getBelgiumTimeZone(), Locale.US);

		// Compute from cachedDays if available
		if (mCachedDays != null) {
			if (mCachedDays.size() > 0) {
				cal.setTime(mCachedDays.get(0).getDate());
			}
		} else {
			// Perform a quick DB query to retrieve the time of the first day
			Cursor cursor = mHelper.getReadableDatabase().query(DatabaseHelper.DAYS_TABLE_NAME, new String[] { "date" }, null, null, null, null,
					"_index ASC LIMIT 1");
			try {
				if (cursor.moveToFirst()) {
					cal.setTimeInMillis(cursor.getLong(0));
				}
			} finally {
				cursor.close();
			}
		}

		// If the calendar has not been set at this point, it will simply return the current year
		mYear = cal.get(Calendar.YEAR);
		return mYear;
	}

	public Cursor getTracks(Day day) {
		String[] selectionArgs = new String[] { String.valueOf(day.getIndex()) };
		Cursor cursor = mHelper.getReadableDatabase().rawQuery(
				"SELECT t.id AS _id, t.name, t.type" + " FROM " + DatabaseHelper.TRACKS_TABLE_NAME + " t" + " JOIN " + DatabaseHelper.EVENTS_TABLE_NAME
				+ " e ON t.id = e.track_id" + " WHERE e.day_index = ?" + " GROUP BY t.id" + " ORDER BY t.name ASC", selectionArgs);
		cursor.setNotificationUri(mContext.getContentResolver(), URI_EVENTS);
		return cursor;
	}

	public static Track toTrack(Cursor cursor, Track track) {
		if (track == null) {
			track = new Track();
		}
		track.setName(cursor.getString(1));
		track.setType(Enum.valueOf(Track.Type.class, cursor.getString(2)));

		return track;
	}

	public static Track toTrack(Cursor cursor) {
		return toTrack(cursor, null);
	}

	public long getEventsCount() {
		return queryNumEntries(mHelper.getReadableDatabase(), DatabaseHelper.EVENTS_TABLE_NAME, null, null);
	}

	/**
	 * Returns the event with the specified id.
	 */
	public Event getEvent(long id) {
		String[] selectionArgs = new String[] { String.valueOf(id) };
		Cursor cursor = mHelper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " LEFT JOIN "
								+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " LEFT JOIN "
								+ DatabaseHelper.PRESENTERS_TABLE_NAME
								+ " p ON ep.presenter_id = p.rowid"
								+ " WHERE e.id = ?" + " GROUP BY e.id ORDER BY et.title", selectionArgs);
		try {
			if (cursor.moveToFirst()) {
				return toEvent(cursor);
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	/**
	 * Returns the events for a specified track.
	 * 
	 * @param day
	 * @param track
	 * @return A cursor to Events
	 */
	public Cursor getEvents(Day day, Track track) {
		String[] selectionArgs = new String[] { String.valueOf(day.getIndex()), track.getName(), track.getType().name() };
		Cursor cursor = mHelper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, b.event_id"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " LEFT JOIN "
								+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " LEFT JOIN "
								+ DatabaseHelper.PRESENTERS_TABLE_NAME
								+ " p ON ep.presenter_id = p.rowid"
								+ " LEFT JOIN "
								+ DatabaseHelper.BOOKMARKS_TABLE_NAME
								+ " b ON e.id = b.event_id"
								+ " WHERE e.day_index = ? AND t.name = ? AND t.type = ?"
								+ " GROUP BY e.id" + " ORDER BY e.start_time,et.title ASC", selectionArgs);
		cursor.setNotificationUri(mContext.getContentResolver(), URI_EVENTS);
		return cursor;
	}

	/**
	 * Returns the events in the specified time window, ordered by start time. All parameters are optional but at least one must be provided.
	 * 
	 * @param minStartTime
	 *            Minimum start time, or -1
	 * @param maxStartTime
	 *            Maximum start time, or -1
	 * @param minEndTime
	 *            Minimum end time, or -1
	 * @param ascending
	 *            If true, order results from start time ascending, else order from start time descending
	 * @return
	 */
	public Cursor getEvents(long minStartTime, long maxStartTime, long minEndTime, boolean ascending) {
		ArrayList<String> selectionArgs = new ArrayList<String>(3);
		StringBuilder whereCondition = new StringBuilder();

		if (minStartTime > 0L) {
			whereCondition.append("e.start_time > ?");
			selectionArgs.add(String.valueOf(minStartTime));
		}
		if (maxStartTime > 0L) {
			if (whereCondition.length() > 0) {
				whereCondition.append(" AND ");
			}
			whereCondition.append("e.start_time < ?");
			selectionArgs.add(String.valueOf(maxStartTime));
		}
		if (minEndTime > 0L) {
			if (whereCondition.length() > 0) {
				whereCondition.append(" AND ");
			}
			whereCondition.append("e.end_time > ?");
			selectionArgs.add(String.valueOf(minEndTime));
		}
		if (whereCondition.length() == 0) {
			throw new IllegalArgumentException("At least one filter must be provided");
		}
		String ascendingString = ascending ? "ASC" : "DESC";

		Cursor cursor = mHelper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, b.event_id"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " LEFT JOIN "
								+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " LEFT JOIN "
								+ DatabaseHelper.PRESENTERS_TABLE_NAME
								+ " p ON ep.presenter_id = p.rowid"
								+ " LEFT JOIN "
								+ DatabaseHelper.BOOKMARKS_TABLE_NAME
								+ " b ON e.id = b.event_id"
								+ " WHERE "
								+ whereCondition.toString()
								+ " GROUP BY e.id"
								+ " ORDER BY e.start_time,et.title " + ascendingString, selectionArgs.toArray(new String[selectionArgs.size()]));
		cursor.setNotificationUri(mContext.getContentResolver(), URI_EVENTS);
		return cursor;
	}

	/**
	 * Returns the events presented by the specified presenter.
	 * 
	 * @param presenter
	 * @return A cursor to Events
	 */
	public Cursor getEvents(Presenter presenter) {
		String[] selectionArgs = new String[] { String.valueOf(presenter.getId()) };
		Cursor cursor = mHelper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, b.event_id"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " LEFT JOIN "
								+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " LEFT JOIN "
								+ DatabaseHelper.PRESENTERS_TABLE_NAME
								+ " p ON ep.presenter_id = p.rowid"
								+ " LEFT JOIN "
								+ DatabaseHelper.BOOKMARKS_TABLE_NAME
								+ " b ON e.id = b.event_id"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
								+ " ep2 ON e.id = ep2.event_id" + " WHERE ep2.presenter_id = ?" + " GROUP BY e.id" + " ORDER BY e.start_time,et.title ASC", selectionArgs);
		cursor.setNotificationUri(mContext.getContentResolver(), URI_EVENTS);
		return cursor;
	}

	/**
	 * Returns the bookmarks.
	 * 
	 * @param minStartTime
	 *            When positive, only return the events starting after this time.
	 * @return A cursor to Events
	 */
	public Cursor getBookmarks(long minStartTime) {
		String whereCondition;
		String[] selectionArgs;
		if (minStartTime > 0L) {
			whereCondition = " WHERE e.start_time > ?";
			selectionArgs = new String[] { String.valueOf(minStartTime) };
		} else {
			whereCondition = "";
			selectionArgs = null;
		}

		Cursor cursor = mHelper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, 1"
								+ " FROM "
								+ DatabaseHelper.BOOKMARKS_TABLE_NAME
								+ " b"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e ON b.event_id = e.id"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " LEFT JOIN "
								+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " LEFT JOIN "
								+ DatabaseHelper.PRESENTERS_TABLE_NAME
								+ " p ON ep.presenter_id = p.rowid" + whereCondition + " GROUP BY e.id" + " ORDER BY e.start_time ASC", selectionArgs);
		cursor.setNotificationUri(mContext.getContentResolver(), URI_EVENTS);
		return cursor;
	}

	/**
	 * Search through matching titles, subtitles, track names, presenter names. We need to use an union of 3 sub-queries because a "match" condition can not be
	 * accompanied by other conditions in a "where" statement.
	 * 
	 * @param query
	 * @return A cursor to Events
	 */
	public Cursor getSearchResults(String query) {
		final String matchQuery = query + "*";
		String[] selectionArgs = new String[] { matchQuery, "%" + query + "%", matchQuery };
		Cursor cursor = mHelper
				.getReadableDatabase()
				.rawQuery(
						"SELECT e.id AS _id, e.start_time, e.end_time, e.room_name, e.slug, et.title, et.subtitle, e.abstract, e.description, GROUP_CONCAT(p.name, ', '), e.day_index, d.date, t.name, t.type, b.event_id"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " et ON e.id = et.rowid"
								+ " JOIN "
								+ DatabaseHelper.DAYS_TABLE_NAME
								+ " d ON e.day_index = d._index"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " LEFT JOIN "
								+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
								+ " ep ON e.id = ep.event_id"
								+ " LEFT JOIN "
								+ DatabaseHelper.PRESENTERS_TABLE_NAME
								+ " p ON ep.presenter_id = p.rowid"
								+ " LEFT JOIN "
								+ DatabaseHelper.BOOKMARKS_TABLE_NAME
								+ " b ON e.id = b.event_id"
								+ " WHERE e.id IN ( "
								+ "SELECT rowid"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " WHERE "
								+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME
								+ " MATCH ?"
								+ " UNION "
								+ "SELECT e.id"
								+ " FROM "
								+ DatabaseHelper.EVENTS_TABLE_NAME
								+ " e"
								+ " JOIN "
								+ DatabaseHelper.TRACKS_TABLE_NAME
								+ " t ON e.track_id = t.id"
								+ " WHERE t.name LIKE ?"
								+ " UNION "
								+ "SELECT ep.event_id"
								+ " FROM "
								+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
								+ " ep"
								+ " JOIN "
								+ DatabaseHelper.PRESENTERS_TABLE_NAME
								+ " p ON ep.presenter_id = p.rowid" + " WHERE p.name MATCH ?" + " )" + " GROUP BY e.id" + " ORDER BY e.start_time ASC",
								selectionArgs);
		cursor.setNotificationUri(mContext.getContentResolver(), URI_EVENTS);
		return cursor;
	}

	/**
	 * Method called by SearchSuggestionProvider to return search results in the format expected by the search framework.
	 * 
	 */
	public Cursor getSearchSuggestionResults(String query, int limit) {
		final String matchQuery = query + "*";
		String[] selectionArgs = new String[] { matchQuery, "%" + query + "%", matchQuery, String.valueOf(limit) };
		// Query is similar to getSearchResults but returns different columns, does not join the Day table or the Bookmark table and limits the result set.
		Cursor cursor = mHelper.getReadableDatabase().rawQuery(
				"SELECT e.id AS " + BaseColumns._ID + ", et.title AS " + SearchManager.SUGGEST_COLUMN_TEXT_1
				+ ", IFNULL(GROUP_CONCAT(p.name, ', '), '') || ' - ' || t.name AS " + SearchManager.SUGGEST_COLUMN_TEXT_2 + ", e.id AS "
				+ SearchManager.SUGGEST_COLUMN_INTENT_DATA + " FROM " + DatabaseHelper.EVENTS_TABLE_NAME + " e" + " JOIN "
				+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME + " et ON e.id = et.rowid" + " JOIN " + DatabaseHelper.TRACKS_TABLE_NAME
				+ " t ON e.track_id = t.id" + " LEFT JOIN " + DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME + " ep ON e.id = ep.event_id" + " LEFT JOIN "
				+ DatabaseHelper.PRESENTERS_TABLE_NAME + " p ON ep.presenter_id = p.rowid" + " WHERE e.id IN ( " + "SELECT rowid" + " FROM "
				+ DatabaseHelper.EVENTS_TITLES_TABLE_NAME + " WHERE " + DatabaseHelper.EVENTS_TITLES_TABLE_NAME + " MATCH ?" + " UNION "
				+ "SELECT e.id" + " FROM " + DatabaseHelper.EVENTS_TABLE_NAME + " e" + " JOIN " + DatabaseHelper.TRACKS_TABLE_NAME
				+ " t ON e.track_id = t.id" + " WHERE t.name LIKE ?" + " UNION " + "SELECT ep.event_id" + " FROM "
				+ DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME + " ep" + " JOIN " + DatabaseHelper.PRESENTERS_TABLE_NAME + " p ON ep.presenter_id = p.rowid"
				+ " WHERE p.name MATCH ?" + " )" + " GROUP BY e.id" + " ORDER BY e.start_time ASC LIMIT ?", selectionArgs);
		return cursor;
	}

	public static Event toEvent(Cursor cursor, Event event) {
		Day day;
		Track track;
		Date startTime;
		Date endTime;
		if (event == null) {
			event = new Event();
			day = new Day();
			event.setDay(day);
			track = new Track();
			event.setTrack(track);

			startTime = null;
			endTime = null;

			day.setDate(new Date(cursor.getLong(11)));
		} else {
			day = event.getDay();
			track = event.getTrack();

			startTime = event.getStartTime();
			endTime = event.getEndTime();

			day.getDate().setTime(cursor.getLong(11));
		}
		event.setId(cursor.getLong(0));
		if (cursor.isNull(1)) {
			event.setStartTime(null);
		} else {
			if (startTime == null) {
				event.setStartTime(new Date(cursor.getLong(1)));
			} else {
				startTime.setTime(cursor.getLong(1));
			}
		}
		if (cursor.isNull(2)) {
			event.setEndTime(null);
		} else {
			if (endTime == null) {
				event.setEndTime(new Date(cursor.getLong(2)));
			} else {
				endTime.setTime(cursor.getLong(2));
			}
		}

		event.setRoomName(cursor.getString(3));
		event.setSlug(cursor.getString(4));
		event.setTitle(cursor.getString(5));
		event.setSubTitle(cursor.getString(6));
		event.setAbstractText(cursor.getString(7));
		event.setDescription(cursor.getString(8));
		event.setPresentersSummary(cursor.getString(9));

		day.setIndex(cursor.getInt(10));

		track.setName(cursor.getString(12));
		track.setType(Enum.valueOf(Track.Type.class, cursor.getString(13)));

		return event;
	}

	public static Event toEvent(Cursor cursor) {
		return toEvent(cursor, null);
	}

	public static long toEventId(Cursor cursor) {
		return cursor.getLong(0);
	}

	public static long toEventStartTimeMillis(Cursor cursor) {
		return cursor.isNull(1) ? -1L : cursor.getLong(1);
	}

	public static boolean toBookmarkStatus(Cursor cursor) {
		return !cursor.isNull(14);
	}

	/**
	 * Returns all presenters in alphabetical order.
	 */
	public Cursor getPresenters() {
		Cursor cursor = mHelper.getReadableDatabase().rawQuery(
				"SELECT rowid AS _id, name" + " FROM " + DatabaseHelper.PRESENTERS_TABLE_NAME + " ORDER BY name COLLATE NOCASE", null);
		cursor.setNotificationUri(mContext.getContentResolver(), URI_EVENTS);
		return cursor;
	}

	public static final int PRESENTER_NAME_COLUMN_INDEX = 1;

	/**
	 * Returns presenters presenting the specified event.
	 */
	public List<Presenter> getPresenters(Event event) {
		String[] selectionArgs = new String[] { String.valueOf(event.getId()) };
		Cursor cursor = mHelper.getReadableDatabase().rawQuery(
				"SELECT p.rowid AS _id, p.name" + " FROM " + DatabaseHelper.PRESENTERS_TABLE_NAME + " p" + " JOIN " + DatabaseHelper.EVENTS_PRESENTERS_TABLE_NAME
				+ " ep ON p.rowid = ep.presenter_id" + " WHERE ep.event_id = ?", selectionArgs);
		try {
			List<Presenter> result = new ArrayList<Presenter>(cursor.getCount());
			while (cursor.moveToNext()) {
				result.add(toPresenter(cursor));
			}
			return result;
		} finally {
			cursor.close();
		}
	}

	public static Presenter toPresenter(Cursor cursor, Presenter presenter) {
		if (presenter == null) {
			presenter = new Presenter();
		}
		presenter.setId(cursor.getLong(0));
		presenter.setName(cursor.getString(1));

		return presenter;
	}

	public static Presenter toPresenter(Cursor cursor) {
		return toPresenter(cursor, null);
	}

	public List<Link> getLinks(Event event) {
		String[] selectionArgs = new String[] { String.valueOf(event.getId()) };
		Cursor cursor = mHelper.getReadableDatabase().rawQuery(
				"SELECT url, description" + " FROM " + DatabaseHelper.LINKS_TABLE_NAME + " WHERE event_id = ?" + " ORDER BY rowid ASC", selectionArgs);
		try {
			List<Link> result = new ArrayList<Link>(cursor.getCount());
			while (cursor.moveToNext()) {
				Link link = new Link();
				link.setUrl(cursor.getString(0));
				link.setDescription(cursor.getString(1));
				result.add(link);
			}
			return result;
		} finally {
			cursor.close();
		}
	}

	public boolean isBookmarked(Event event) {
		String[] selectionArgs = new String[] { String.valueOf(event.getId()) };
		return queryNumEntries(mHelper.getReadableDatabase(), DatabaseHelper.BOOKMARKS_TABLE_NAME, "event_id = ?", selectionArgs) > 0L;
	}

	public boolean addBookmark(Event event) {
		boolean complete = false;

		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("event_id", event.getId());
			long result = db.insert(DatabaseHelper.BOOKMARKS_TABLE_NAME, null, values);

			// If the bookmark is already present
			if (result == -1L) {
				return false;
			}

			db.setTransactionSuccessful();
			complete = true;
			return true;
		} finally {
			db.endTransaction();
			db.close();

			if (complete) {
				mContext.getContentResolver().notifyChange(URI_EVENTS, null);

				Intent intent = new Intent(ACTION_ADD_BOOKMARK).putExtra(EXTRA_EVENT_ID, event.getId());
				Date startTime = event.getStartTime();
				if (startTime != null) {
					intent.putExtra(EXTRA_EVENT_START_TIME, startTime.getTime());
				}
				LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
			}
		}
	}

	public boolean removeBookmark(Event event) {
		return removeBookmarks(new long[] { event.getId() });
	}

	public boolean removeBookmark(long eventId) {
		return removeBookmarks(new long[] { eventId });
	}

	public boolean removeBookmarks(long[] eventIds) {
		int length = eventIds.length;
		if (length == 0) {
			throw new IllegalArgumentException("At least one bookmark id to remove must be passed");
		}
		String[] stringEventIds = new String[length];
		for (int i = 0; i < length; ++i) {
			stringEventIds[i] = String.valueOf(eventIds[i]);
		}

		boolean isComplete = false;

		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			String whereClause = "event_id IN (" + TextUtils.join(",", stringEventIds) + ")";
			int count = db.delete(DatabaseHelper.BOOKMARKS_TABLE_NAME, whereClause, null);

			if (count == 0) {
				return false;
			}

			db.setTransactionSuccessful();
			isComplete = true;
			return true;
		} finally {
			db.endTransaction();
			db.close();

			if (isComplete) {
				mContext.getContentResolver().notifyChange(URI_EVENTS, null);

				Intent intent = new Intent(ACTION_REMOVE_BOOKMARKS).putExtra(EXTRA_EVENT_IDS, eventIds);
				LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
			}
		}
	}
}