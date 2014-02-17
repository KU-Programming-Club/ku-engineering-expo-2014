package edu.kuacm.expo2014.adapters;

import java.text.DateFormat;
import java.util.Date;

import edu.kuacm.expo2014.R;
import edu.kuacm.expo2014.db.DatabaseManager;
import edu.kuacm.expo2014.model.Event;
import edu.kuacm.expo2014.utils.DateUtils;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EventsAdapter extends CursorAdapter {

	private static final DateFormat TIME_DATE_FORMAT = DateUtils.getTimeDateFormat();

	private final LayoutInflater mInflater;
	private final int mTitleTextSize;
	private final boolean mShowDay;

	public EventsAdapter(Context context) {
		this(context, true);
	}

	public EventsAdapter(Context context, boolean showDay) {
		super(context, null, 0);
		mInflater = LayoutInflater.from(context);
		mTitleTextSize = context.getResources().getDimensionPixelSize(R.dimen.list_item_title_text_size);
		mShowDay = showDay;
	}

	@Override
	public Event getItem(int position) {
		return DatabaseManager.toEvent((Cursor) super.getItem(position));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.item_event, parent, false);

		ViewHolder holder = new ViewHolder();
		holder.title = (TextView) view.findViewById(R.id.title);
		holder.titleSizeSpan = new AbsoluteSizeSpan(mTitleTextSize);
		holder.trackName = (TextView) view.findViewById(R.id.track_name);
		holder.details = (TextView) view.findViewById(R.id.details);
		view.setTag(holder);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		Event event = DatabaseManager.toEvent(cursor, holder.event);
		holder.event = event;

		String eventTitle = event.getTitle();
		SpannableString spannableString;
		String presentersSummary = event.getPresentersSummary();
		if (TextUtils.isEmpty(presentersSummary)) {
			spannableString = new SpannableString(eventTitle);
		} else {
			spannableString = new SpannableString(String.format("%1$s\n%2$s", eventTitle, event.getPresentersSummary()));
		}
		spannableString.setSpan(holder.titleSizeSpan, 0, eventTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		holder.title.setText(spannableString);
		int bookmarkDrawable = DatabaseManager.toBookmarkStatus(cursor) ? R.drawable.ic_small_starred : 0;
		holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, bookmarkDrawable, 0);

		holder.trackName.setText(event.getTrack().getName());

		Date startTime = event.getStartTime();
		Date endTime = event.getEndTime();
		String startTimeString = (startTime != null) ? TIME_DATE_FORMAT.format(startTime) : "?";
		String endTimeString = (endTime != null) ? TIME_DATE_FORMAT.format(endTime) : "?";
		String details;
		if (mShowDay) {
			details = String.format("%1$s, %2$s ― %3$s  |  %4$s", event.getDay().getShortName(), startTimeString, endTimeString, event.getRoomName());
		} else {
			details = String.format("%1$s ― %2$s  |  %3$s", startTimeString, endTimeString, event.getRoomName());
		}
		holder.details.setText(details);
	}

	private static class ViewHolder {
		TextView title;
		AbsoluteSizeSpan titleSizeSpan;
		TextView trackName;
		TextView details;
		Event event;
	}
}