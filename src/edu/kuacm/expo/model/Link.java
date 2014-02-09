package edu.kuacm.expo.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Link implements Parcelable {

	private String mUrl;
	private String mDescription;

	public Link() {}

	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String url) {
		mUrl = url;
	}

	public String getDescription() {
		return mDescription;
	}

	public void setDescription(String description) {
		mDescription = description;
	}

	@Override
	public String toString() {
		return mDescription;
	}

	@Override
	public int hashCode() {
		return mUrl.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Link other = (Link) obj;
		return mUrl.equals(other.mUrl);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mUrl);
		out.writeString(mDescription);
	}

	public static final Parcelable.Creator<Link> CREATOR = new Parcelable.Creator<Link>() {
		@Override
		public Link createFromParcel(Parcel in) {
			return new Link(in);
		}

		@Override
		public Link[] newArray(int size) {
			return new Link[size];
		}
	};

	private Link(Parcel in) {
		mUrl = in.readString();
		mDescription = in.readString();
	}
}