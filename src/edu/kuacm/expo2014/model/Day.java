package edu.kuacm.expo2014.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.kuacm.expo2014.utils.DateUtils;
import android.os.Parcel;
import android.os.Parcelable;

public class Day implements Parcelable {

	private static DateFormat DAY_DATE_FORMAT = DateUtils.withBelgiumTimeZone(new SimpleDateFormat("EEEE", Locale.US));

	private int mIndex;
	private Date mDate;

	public Day() {}

	public int getIndex() {
		return mIndex;
	}

	public void setIndex(int index) {
		mIndex = index;
	}

	public Date getDate() {
		return mDate;
	}

	public void setDate(Date date) {
		mDate = date;
	}

	public String getName() {
		return String.format(Locale.US, "Day %1$d (%2$s)", mIndex, DAY_DATE_FORMAT.format(mDate));
	}

	public String getShortName() {
		return DAY_DATE_FORMAT.format(mDate);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		return mIndex;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Day other = (Day) obj;
		return (mIndex == other.mIndex);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(mIndex);
		out.writeLong((mDate == null) ? 0L : mDate.getTime());
	}

	public static final Parcelable.Creator<Day> CREATOR = new Parcelable.Creator<Day>() {
		@Override
		public Day createFromParcel(Parcel in) {
			return new Day(in);
		}

		@Override
		public Day[] newArray(int size) {
			return new Day[size];
		}
	};

	private Day(Parcel in) {
		mIndex = in.readInt();
		long time = in.readLong();
		if (time != 0L) {
			mDate = new Date(time);
		}
	}
}