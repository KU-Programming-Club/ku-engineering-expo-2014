package edu.kuacm.expo2014.model;

import java.util.Date;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import edu.kuacm.expo2014.api.ExpoUrls;

public class Event implements Parcelable {

	private long mId;
	private Day mDay;
	private Date mStartTime;
	private Date mEndTime;
	private String mRoomName;
	private String mSlug;
	private String mTitle;
	private String mSubTitle;
	private Track mTrack;
	private String mAbstractText;
	private String mDescription;
	private String mPresentersSummary;
	private List<Presenter> mPresenters; // Optional
	private List<Link> mLinks; // Optional

	public Event() {
	}

	public long getId() {
		return mId;
	}

	public void setId(long id) {
		mId = id;
	}

	public Day getDay() {
		return mDay;
	}

	public void setDay(Day day) {
		mDay = day;
	}

	public Date getStartTime() {
		return mStartTime;
	}

	public void setStartTime(Date startTime) {
		mStartTime = startTime;
	}

	public Date getEndTime() {
		return mEndTime;
	}

	public void setEndTime(Date endTime) {
		mEndTime = endTime;
	}

	/**
	 * 
	 * @return The event duration in minutes
	 */
	public int getDuration() {
		if ((mStartTime == null) || (mEndTime == null)) {
			return 0;
		}
		return (int) ((mEndTime.getTime() - mStartTime.getTime()) / 1000L);
	}

	public String getRoomName() {
		return (mRoomName == null) ? "" : mRoomName;
	}

	public void setRoomName(String roomName) {
		mRoomName = roomName;
	}

	public String getSlug() {
		return mSlug;
	}

	public void setSlug(String slug) {
		mSlug = slug;
	}

	public String getUrl() {
		return ExpoUrls.getEventUrl(mTitle, mTrack);
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String title) {
		mTitle = title;
	}

	public String getSubTitle() {
		return mSubTitle;
	}

	public void setSubTitle(String subTitle) {
		mSubTitle = subTitle;
	}

	public Track getTrack() {
		return mTrack;
	}

	public void setTrack(Track track) {
		mTrack = track;
	}

	public String getAbstractText() {
		return mAbstractText;
	}

	public void setAbstractText(String abstractText) {
		mAbstractText = abstractText;
	}

	public String getDescription() {
		return mDescription;
	}

	public void setDescription(String description) {
		mDescription = description;
	}

	public String getPresentersSummary() {
		if (mPresentersSummary != null) {
			return mPresentersSummary;
		}
		if (mPresenters != null) {
			return TextUtils.join(", ", mPresenters);
		}
		return "";
	}

	public void setPresentersSummary(String presentersSummary) {
		mPresentersSummary = presentersSummary;
	}

	public List<Presenter> getPresenters() {
		return mPresenters;
	}

	public void setPresenters(List<Presenter> presenters) {
		mPresenters = presenters;
	}

	public List<Link> getLinks() {
		return mLinks;
	}

	public void setLinks(List<Link> links) {
		mLinks = links;
	}

	@Override
	public String toString() {
		return mTitle;
	}

	@Override
	public int hashCode() {
		return (int) (mId ^ (mId >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Event other = (Event) obj;
		return mId == other.mId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(mId);
		mDay.writeToParcel(out, flags);
		out.writeLong((mStartTime == null) ? 0L : mStartTime.getTime());
		out.writeLong((mEndTime == null) ? 0L : mEndTime.getTime());
		out.writeString(mRoomName);
		out.writeString(mSlug);
		out.writeString(mTitle);
		out.writeString(mSubTitle);
		mTrack.writeToParcel(out, flags);
		out.writeString(mAbstractText);
		out.writeString(mDescription);
		out.writeString(mPresentersSummary);
		if (mPresenters == null) {
			out.writeInt(0);
		} else {
			out.writeInt(1);
			out.writeTypedList(mPresenters);
		}
		if (mLinks == null) {
			out.writeInt(0);
		} else {
			out.writeInt(1);
			out.writeTypedList(mLinks);
		}
	}

	public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
		@Override
		public Event createFromParcel(Parcel in) {
			return new Event(in);
		}

		@Override
		public Event[] newArray(int size) {
			return new Event[size];
		}
	};

	private Event(Parcel in) {
		mId = in.readLong();
		mDay = Day.CREATOR.createFromParcel(in);
		long time = in.readLong();
		if (time != 0L) {
			mStartTime = new Date(time);
		}
		time = in.readLong();
		if (time != 0L) {
			mEndTime = new Date(time);
		}
		mRoomName = in.readString();
		mSlug = in.readString();
		mTitle = in.readString();
		mSubTitle = in.readString();
		mTrack = Track.CREATOR.createFromParcel(in);
		mAbstractText = in.readString();
		mDescription = in.readString();
		mPresentersSummary = in.readString();
		if (in.readInt() == 1) {
			mPresenters = in.createTypedArrayList(Presenter.CREATOR);
		}
		if (in.readInt() == 1) {
			mLinks = in.createTypedArrayList(Link.CREATOR);
		}
	}
}