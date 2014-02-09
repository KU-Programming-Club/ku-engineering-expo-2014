package edu.kuacm.expo.model;

import edu.kuacm.expo.R;
import android.os.Parcel;
import android.os.Parcelable;

public class Track implements Parcelable {

	public static enum Type {
		// This weird naming convention is due to parsing constraints
		other(R.string.other), keynote(R.string.keynote), maintrack(R.string.main_track), devroom(R.string.developer_room), lightningtalk(
				R.string.lightning_talk), certification(R.string.certification_exam);

		private int mmNameResId;

		private Type(int nameResId) {
			mmNameResId = nameResId;
		}

		public int getNameResId() {
			return mmNameResId;
		}
	}

	private String mName;
	private Type mType;

	public Track() {
	}

	public Track(String name, Type type) {
		this.mName = name;
		this.mType = type;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public Type getType() {
		return mType;
	}

	public void setType(Type type) {
		this.mType = type;
	}

	@Override
	public String toString() {
		return mName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mName.hashCode();
		result = prime * result + mType.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Track other = (Track) obj;
		return mName.equals(other.mName) && (mType == other.mType);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mName);
		out.writeInt(mType.ordinal());
	}

	public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
		@Override
		public Track createFromParcel(Parcel in) {
			return new Track(in);
		}

		@Override
		public Track[] newArray(int size) {
			return new Track[size];
		}
	};

	private Track(Parcel in) {
		mName = in.readString();
		mType = Type.values()[in.readInt()];
	}
}