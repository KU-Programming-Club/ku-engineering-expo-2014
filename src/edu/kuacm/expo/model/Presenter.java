package edu.kuacm.expo.model;

import edu.kuacm.expo.api.ExpoUrls;
import edu.kuacm.expo.db.DatabaseManager;
import edu.kuacm.expo.utils.StringUtils;
import android.os.Parcel;
import android.os.Parcelable;

public class Presenter implements Parcelable {

	private long mId;
	private String mName;

	public Presenter() {}

	public long getId() {
		return mId;
	}

	public void setId(long id) {
		mId = id;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	public String getUrl() {
		return ExpoUrls.getPresenterUrl(StringUtils.toSlug(mName), DatabaseManager.getInstance().getYear());
	}

	@Override
	public String toString() {
		return mName;
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
		if (getClass() != obj.getClass())
			return false;
		Presenter other = (Presenter) obj;
		return (mId == other.mId);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(mId);
		out.writeString(mName);
	}

	public static final Parcelable.Creator<Presenter> CREATOR = new Parcelable.Creator<Presenter>() {
		@Override
		public Presenter createFromParcel(Parcel in) {
			return new Presenter(in);
		}

		@Override
		public Presenter[] newArray(int size) {
			return new Presenter[size];
		}
	};

	private Presenter(Parcel in) {
		mId = in.readLong();
		mName = in.readString();
	}
}