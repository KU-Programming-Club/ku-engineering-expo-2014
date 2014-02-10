package edu.kuacm.expo.fragments;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.kuacm.expo.R;
import edu.kuacm.expo.activities.PresenterInfoActivity;
import edu.kuacm.expo.db.DatabaseManager;
import edu.kuacm.expo.loaders.BookmarkStatusLoader;
import edu.kuacm.expo.loaders.LocalCacheLoader;
import edu.kuacm.expo.model.Event;
import edu.kuacm.expo.model.Link;
import edu.kuacm.expo.model.Presenter;
import edu.kuacm.expo.utils.DateUtils;
import edu.kuacm.expo.utils.StringUtils;

public class EventDetailsFragment extends Fragment {

	private static class EventDetails {
		List<Presenter> presenters;
		List<Link> links;
	}

	private static class ViewHolder {
		LayoutInflater inflater;
		TextView presentersTextView;
		ViewGroup linksContainer;
	}

	private static final int BOOKMARK_STATUS_LOADER_ID = 1;
	private static final int EVENT_DETAILS_LOADER_ID = 2;

	private static final String ARG_EVENT = "event";

	private static final DateFormat TIME_DATE_FORMAT = DateUtils.getTimeDateFormat();

	private Event mEvent;
	private int mPresentersCount = 1;
	private Boolean mIsBookmarked;
	private ViewHolder mHolder;

	private MenuItem mBookmarkMenuItem;

	public static EventDetailsFragment newInstance(Event event) {
		EventDetailsFragment f = new EventDetailsFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_EVENT, event);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mEvent = getArguments().getParcelable(ARG_EVENT);
		setHasOptionsMenu(true);
	}

	public Event getEvent() {
		return mEvent;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_event_details, container, false);

		mHolder = new ViewHolder();
		mHolder.inflater = inflater;

		((TextView) view.findViewById(R.id.title)).setText(mEvent.getTitle());
		TextView textView = (TextView) view.findViewById(R.id.subtitle);
		String text = mEvent.getSubTitle();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(text);
		}

		MovementMethod linkMovementMethod = LinkMovementMethod.getInstance();

		// Set the presenters summary text first; replace it with the clickable text when the loader completes
		mHolder.presentersTextView = (TextView) view.findViewById(R.id.presenters);
		String presentersSummary = mEvent.getPresentersSummary();
		if (TextUtils.isEmpty(presentersSummary)) {
			mHolder.presentersTextView.setVisibility(View.GONE);
		} else {
			mHolder.presentersTextView.setText(presentersSummary);
			mHolder.presentersTextView.setMovementMethod(linkMovementMethod);
			mHolder.presentersTextView.setVisibility(View.VISIBLE);
		}

		((TextView) view.findViewById(R.id.track)).setText(mEvent.getTrack().getName());
		Date startTime = mEvent.getStartTime();
		Date endTime = mEvent.getEndTime();
		text = String.format("%1$s, %2$s ― %3$s", mEvent.getDay().toString(), (startTime != null) ? TIME_DATE_FORMAT.format(startTime) : "?",
				(endTime != null) ? TIME_DATE_FORMAT.format(endTime) : "?");
		((TextView) view.findViewById(R.id.time)).setText(text);
		final String roomName = mEvent.getRoomName();
		TextView roomTextView = (TextView) view.findViewById(R.id.room);
		Spannable roomText = new SpannableString(String.format("%1$s", roomName));
		final int roomImageResId = getResources().getIdentifier(StringUtils.roomNameToResourceName(roomName), "drawable", getActivity().getPackageName());
		// If the room image exists, make the room text clickable to display it
		if (roomImageResId != 0) {
			roomText.setSpan(new UnderlineSpan(), 0, roomText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			roomTextView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					RoomImageDialogFragment.newInstance(roomName, roomImageResId).show(getFragmentManager());
				}
			});
			roomTextView.setFocusable(true);
		}
		roomTextView.setText(roomText);

		textView = (TextView) view.findViewById(R.id.abstract_text);
		text = mEvent.getAbstractText();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(StringUtils.trimEnd(Html.fromHtml(text)));
			textView.setMovementMethod(linkMovementMethod);
		}
		textView = (TextView) view.findViewById(R.id.description);
		text = mEvent.getDescription();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(StringUtils.trimEnd(Html.fromHtml(text)));
			textView.setMovementMethod(linkMovementMethod);
		}

		mHolder.linksContainer = (ViewGroup) view.findViewById(R.id.links_container);
		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mHolder = null;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		LoaderManager loaderManager = getLoaderManager();
		loaderManager.initLoader(BOOKMARK_STATUS_LOADER_ID, null, bookmarkStatusLoaderCallbacks);
		loaderManager.initLoader(EVENT_DETAILS_LOADER_ID, null, eventDetailsLoaderCallbacks);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.event, menu);
		menu.findItem(R.id.share).setIntent(getShareChooserIntent());
		mBookmarkMenuItem = menu.findItem(R.id.bookmark);
		updateOptionsMenu();
	}

	private Intent getShareChooserIntent() {
		return ShareCompat.IntentBuilder.from(getActivity()).setSubject(String.format("%1$s (KU Engineering Expo)", mEvent.getTitle())).setType("text/plain")
				.setText(String.format("%1$s %2$s #expo", mEvent.getTitle(), mEvent.getUrl())).setChooserTitle(R.string.share).createChooserIntent();
	}

	private void updateOptionsMenu() {
		if (mBookmarkMenuItem != null) {
			if (mIsBookmarked == null) {
				mBookmarkMenuItem.setEnabled(false);
			} else {
				mBookmarkMenuItem.setEnabled(true);

				if (mIsBookmarked) {
					mBookmarkMenuItem.setTitle(R.string.remove_bookmark);
					mBookmarkMenuItem.setIcon(R.drawable.ic_action_important);
				} else {
					mBookmarkMenuItem.setTitle(R.string.add_bookmark);
					mBookmarkMenuItem.setIcon(R.drawable.ic_action_not_important);
				}
			}
		}
	}

	@Override
	public void onDestroyOptionsMenu() {
		super.onDestroyOptionsMenu();
		mBookmarkMenuItem = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.bookmark:
			if (mIsBookmarked != null) {
				new UpdateBookmarkAsyncTask(mEvent).execute(mIsBookmarked);
			}
			return true;
		case R.id.add_to_agenda:
			addToAgenda();
			return true;
		}
		return false;
	}

	private static class UpdateBookmarkAsyncTask extends AsyncTask<Boolean, Void, Void> {

		private final Event mmEvent;

		public UpdateBookmarkAsyncTask(Event event) {
			mmEvent = event;
		}

		@Override
		protected Void doInBackground(Boolean... remove) {
			if (remove[0]) {
				DatabaseManager.getInstance().removeBookmark(mmEvent);
			} else {
				DatabaseManager.getInstance().addBookmark(mmEvent);
			}
			return null;
		}
	}

	@SuppressLint("InlinedApi")
	private void addToAgenda() {
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");
		intent.putExtra(CalendarContract.Events.TITLE, mEvent.getTitle());
		intent.putExtra(CalendarContract.Events.EVENT_LOCATION, "ULB - " + mEvent.getRoomName());
		String description = mEvent.getAbstractText();
		if (TextUtils.isEmpty(description)) {
			description = mEvent.getDescription();
		}
		// Strip HTML
		description = StringUtils.trimEnd(Html.fromHtml(description)).toString();
		// Add presenter info if available
		if (mPresentersCount > 0) {
			description = String.format("%1$s: %2$s\n\n%3$s", getResources().getQuantityString(R.plurals.presenters, mPresentersCount), mEvent.getPresentersSummary(),
					description);
		}
		intent.putExtra(CalendarContract.Events.DESCRIPTION, description);
		Date time = mEvent.getStartTime();
		if (time != null) {
			intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, time.getTime());
		}
		time = mEvent.getEndTime();
		if (time != null) {
			intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, time.getTime());
		}
		startActivity(intent);
	}

	private final LoaderCallbacks<Boolean> bookmarkStatusLoaderCallbacks = new LoaderCallbacks<Boolean>() {

		@Override
		public Loader<Boolean> onCreateLoader(int id, Bundle args) {
			return new BookmarkStatusLoader(getActivity(), mEvent);
		}

		@Override
		public void onLoadFinished(Loader<Boolean> loader, Boolean data) {
			mIsBookmarked = data;
			updateOptionsMenu();
		}

		@Override
		public void onLoaderReset(Loader<Boolean> loader) {
		}
	};

	private static class EventDetailsLoader extends LocalCacheLoader<EventDetails> {

		private final Event mmEvent;

		public EventDetailsLoader(Context context, Event event) {
			super(context);
			mmEvent = event;
		}

		@Override
		public EventDetails loadInBackground() {
			EventDetails result = new EventDetails();
			DatabaseManager dbm = DatabaseManager.getInstance();
			result.presenters = dbm.getPresenters(mmEvent);
			result.links = dbm.getLinks(mmEvent);
			return result;
		}
	}

	private final LoaderCallbacks<EventDetails> eventDetailsLoaderCallbacks = new LoaderCallbacks<EventDetails>() {

		@Override
		public Loader<EventDetails> onCreateLoader(int id, Bundle args) {
			return new EventDetailsLoader(getActivity(), mEvent);
		}

		@Override
		public void onLoadFinished(Loader<EventDetails> loader, EventDetails data) {
			// 1. Presenters
			if (data.presenters != null) {
				mPresentersCount = data.presenters.size();
				if (mPresentersCount > 0) {
					// Build a list of clickable presenters
					SpannableStringBuilder sb = new SpannableStringBuilder();
					int length = 0;
					for (Presenter presenter : data.presenters) {
						if (length != 0) {
							sb.append(", ");
						}
						String name = presenter.getName();
						sb.append(name);
						length = sb.length();
						sb.setSpan(new PresenterClickableSpan(presenter), length - name.length(), length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					mHolder.presentersTextView.setText(sb);
					mHolder.presentersTextView.setVisibility(View.VISIBLE);
				}
			}

			// 2. Links
			// Keep the first 2 views in links container (titles) only
			int linkViewCount = mHolder.linksContainer.getChildCount();
			if (linkViewCount > 2) {
				mHolder.linksContainer.removeViews(2, linkViewCount - 2);
			}
			if ((data.links != null) && (data.links.size() > 0)) {
				mHolder.linksContainer.setVisibility(View.VISIBLE);
				for (Link link : data.links) {
					View view = mHolder.inflater.inflate(R.layout.item_link, mHolder.linksContainer, false);
					TextView tv = (TextView) view.findViewById(R.id.description);
					tv.setText(link.getDescription());
					view.setOnClickListener(new LinkClickListener(link));
					mHolder.linksContainer.addView(view);
					// Add a list divider
					mHolder.inflater.inflate(R.layout.list_divider, mHolder.linksContainer, true);
				}
			} else {
				mHolder.linksContainer.setVisibility(View.GONE);
			}
		}

		@Override
		public void onLoaderReset(Loader<EventDetails> loader) {
		}
	};

	private static class PresenterClickableSpan extends ClickableSpan {

		private final Presenter mmPresenter;

		public PresenterClickableSpan(Presenter presenter) {
			mmPresenter = presenter;
		}

		@Override
		public void onClick(View v) {
			Context context = v.getContext();
			Intent intent = new Intent(context, PresenterInfoActivity.class).putExtra(PresenterInfoActivity.EXTRA_PRESENTER, mmPresenter);
			context.startActivity(intent);
		}
	}

	private static class LinkClickListener implements View.OnClickListener {

		private final Link mmLink;

		public LinkClickListener(Link link) {
			mmLink = link;
		}

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mmLink.getUrl()));
			v.getContext().startActivity(intent);
		}
	}
}