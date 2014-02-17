package edu.kuacm.expo2014.parsers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;

import edu.kuacm.expo2014.model.Day;
import edu.kuacm.expo2014.model.Event;
import edu.kuacm.expo2014.model.Link;
import edu.kuacm.expo2014.model.Presenter;
import edu.kuacm.expo2014.model.Track;
import edu.kuacm.expo2014.utils.DateUtils;
import android.text.TextUtils;

/**
 * Main parser for Expo schedule data in pentabarf XML format.
 * 
 * @author Christophe Beyls
 * 
 */
public class EventsParser extends IterableAbstractPullParser<Event> {

	private static final DateFormat DATE_FORMAT = DateUtils.withBelgiumTimeZone(new SimpleDateFormat("yyyy-MM-dd", Locale.US));

	// Calendar used to compute the events time, according to Belgium timezone
	private final Calendar mCalendar = Calendar.getInstance(DateUtils.getBelgiumTimeZone(), Locale.US);

	private Day mCurrentDay;
	private String mCurrentRoom;
	private Track mCurrentTrack;

	/**
	 * Returns the hours portion of a time string in the "hh:mm" format, without allocating objects.
	 * 
	 * @param time
	 * @return hours
	 */
	private static int getHours(String time) {
		return (Character.getNumericValue(time.charAt(0)) * 10) + Character.getNumericValue(time.charAt(1));
	}

	/**
	 * Returns the minutes portion of a time string in the "hh:mm" format, without allocating objects.
	 * 
	 * @param time
	 * @return minutes
	 */
	private static int getMinutes(String time) {
		return (Character.getNumericValue(time.charAt(3)) * 10) + Character.getNumericValue(time.charAt(4));
	}

	@Override
	protected boolean parseHeader(XmlPullParser parser) throws Exception {
		while (!isEndDocument()) {
			if (isStartTag("schedule")) {
				return true;
			}

			parser.next();
		}
		return false;
	}

	@Override
	protected Event parseNext(XmlPullParser parser) throws Exception {
		while (!isNextEndTag("schedule")) {
			if (isStartTag()) {
				String name = parser.getName();

				if ("day".equals(name)) {
					mCurrentDay = new Day();
					mCurrentDay.setIndex(Integer.parseInt(parser.getAttributeValue(null, "index")));
					mCurrentDay.setDate(DATE_FORMAT.parse(parser.getAttributeValue(null, "date")));
				} else if ("room".equals(name)) {
					mCurrentRoom = parser.getAttributeValue(null, "name");
				} else if ("event".equals(name)) {
					Event event = new Event();
					event.setId(Long.parseLong(parser.getAttributeValue(null, "id")));
					event.setDay(mCurrentDay);
					event.setRoomName(mCurrentRoom);
					// Initialize empty lists
					List<Presenter> presenters = new ArrayList<Presenter>();
					event.setPresenters(presenters);
					List<Link> links = new ArrayList<Link>();
					event.setLinks(links);

					String duration = null;
					String trackName = "";
					Track.Type trackType = Track.Type.other;

					while (!isNextEndTag("event")) {
						if (isStartTag()) {
							name = parser.getName();

							if ("start".equals(name)) {
								String time = parser.nextText();
								if (!TextUtils.isEmpty(time)) {
									mCalendar.setTime(mCurrentDay.getDate());
									mCalendar.set(Calendar.HOUR_OF_DAY, getHours(time));
									mCalendar.set(Calendar.MINUTE, getMinutes(time));
									event.setStartTime(mCalendar.getTime());
								}
							} else if ("duration".equals(name)) {
								duration = parser.nextText();
							} else if ("slug".equals(name)) {
								event.setSlug(parser.nextText());
							} else if ("title".equals(name)) {
								event.setTitle(parser.nextText());
							} else if ("subtitle".equals(name)) {
								event.setSubTitle(parser.nextText());
							} else if ("track".equals(name)) {
								trackName = parser.nextText();
							} else if ("type".equals(name)) {
								try {
									trackType = Enum.valueOf(Track.Type.class, parser.nextText());
								} catch (Exception e) {
									// trackType will be "other"
								}
							} else if ("abstract".equals(name)) {
								event.setAbstractText(parser.nextText());
							} else if ("description".equals(name)) {
								event.setDescription(parser.nextText());
							} else if ("presenters".equals(name)) {
								while (!isNextEndTag("presenters")) {
									if (isStartTag("presenter")) {
										Presenter presenter = new Presenter();
										presenter.setId(Long.parseLong(parser.getAttributeValue(null, "id")));
										presenter.setName(parser.nextText());

										presenters.add(presenter);
									}
								}
							} else if ("links".equals(name)) {
								while (!isNextEndTag("links")) {
									if (isStartTag("link")) {
										Link link = new Link();
										link.setUrl(parser.getAttributeValue(null, "href"));
										link.setDescription(parser.nextText());

										links.add(link);
									}
								}
							} else {
								skipToEndTag();
							}
						}
					}

					if ((event.getStartTime() != null) && !TextUtils.isEmpty(duration)) {
						mCalendar.add(Calendar.HOUR_OF_DAY, getHours(duration));
						mCalendar.add(Calendar.MINUTE, getMinutes(duration));
						event.setEndTime(mCalendar.getTime());
					}

					if ((mCurrentTrack == null) || !trackName.equals(mCurrentTrack.getName()) || (trackType != mCurrentTrack.getType())) {
						mCurrentTrack = new Track(trackName, trackType);
					}
					event.setTrack(mCurrentTrack);

					// Hacky hardcoded wackiness
					if (event.getTrack().getType() == Track.Type.competitions) {
						Link link = new Link();
						link.setUrl(event.getUrl());
						link.setDescription("Competition website");
						links.add(0, link);
					}
					
					return event;
				} else {
					skipToEndTag();
				}
			}
		}
		return null;
	}
}