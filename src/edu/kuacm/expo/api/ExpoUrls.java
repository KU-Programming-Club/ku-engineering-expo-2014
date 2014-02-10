package edu.kuacm.expo.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.kuacm.expo.model.Track;

/**
 * This class contains all Expo URLs
 * 
 * @author Christophe Beyls
 * 
 */
public class ExpoUrls {
	private static final Map<String, String> mPresenterUrlMap = buildPresenterUrlMap();

	private static final String SCHEDULE_URL = "https://raw.github.com/RyanGlScott/ku-engineering-expo-base/master/assets/events.txt";
	private static final String EVENT_URL_FORMAT = "http://groups.ku.edu/~kuesc/expo/activities/competitions/%1$s";

	public static String getScheduleUrl() {
		return SCHEDULE_URL;
	}

	public static String getEventUrl(String name, Track track) {
		String formattedName = name.replaceAll("\\(.*?\\)","").trim().toLowerCase(Locale.US).replaceAll("\\s+", "-");
		if (track.getType() == Track.Type.competitions) {
			return String.format(Locale.US, EVENT_URL_FORMAT, formattedName);
		} else {
			return "http://groups.ku.edu/~kuesc/expo/activities";
		}
	}

	public static String getPresenterUrl(String name) {
		if (mPresenterUrlMap.containsKey(name)) {
			return mPresenterUrlMap.get(name);
		} else {
			return "https://rockchalkcentral.ku.edu/organizations";
		}
	}	

	private static Map<String, String> buildPresenterUrlMap() {
		final HashMap<String, String> map = new HashMap<String, String>();
		map.put("Architectural Engineering Institute (AEI)", "http://groups.ku.edu/~kuesc/groups/aei");
		map.put("American Institute of Aeronautics and Astronautics (AIAA)", "http://groupspaces.com/aiaaku");
		map.put("Alternative Energy Society (AES)", "http://biodiesel.ku.edu/aes");
		map.put("American Indian Science and Engineering Society (AISES)", "https://rockchalkcentral.ku.edu/organization/aises");
		map.put("American Institute of Chemical Engineers (AIChE)", "https://rockchalkcentral.ku.edu/organization/aiche");
		map.put("American Society of Civil Engineers (ASCE)", "https://sites.google.com/site/asceku/");
		map.put("American Society of Mechanical Engineers (ASME)", "https://rockchalkcentral.ku.edu/organization/asme");
		map.put("American Society of Heating, Refrigerating and Air-Conditioning Engineers (ASHRAE)", "http://groups.ku.edu/~kuesc/groups/ashrae");
		map.put("Association for Computing Machinery (ACM)", "http://people.eecs.ku.edu/~acm/");
		map.put("Biomedical Engineering Society (BMES)", "http://www.engr.ku.edu/~kubmes/");
		map.put("Chemistry Club", "https://chemclub.ku.edu/");
		map.put("Concrete Canoe", "https://sites.google.com/site/asceku/concrete-canoe");
		map.put("Earthquake Engineering Research Institute (EERI)", "https://rockchalkcentral.ku.edu/organization/EERI");
		map.put("EcoHawks", "http://depcik.faculty.ku.edu/?q=EcoHawks");
		map.put("Engineers Without Borders", "http://groups.ku.edu/~kuesc/groups/ewb");
		map.put("Eta Kappa Nu (HKN)", "http://groups.ku.edu/~hkn/");
		map.put("Expo Administration", "http://groups.ku.edu/~kuesc/expo");
		map.put("Institute of Electrical and Electronics Engineers (IEEE)", "http://www.ieeeku.org/");
		map.put("Illuminating Engineering Society (IES)", "http://ceae.engr.ku.edu/organizations/ies/");
		map.put("Jayhawk Motorsports", "http://www.jayhawkmotorsports.com/");
		map.put("Jayhawk Heavy Lift", "https://rockchalkcentral.ku.edu/organization/jayhawkheavylift");
		map.put("KU Robotics", "https://rockchalkcentral.ku.edu/organization/KU_Robotics");
		map.put("National Society of Black Engineers (NSBE)", "http://groups.ku.edu/~nsbe/");
		map.put("Physics and Engineering Student Organization (PESO)", "http://ephsx.org/");
		map.put("SAE Baja", "https://rockchalkcentral.ku.edu/organization/saebaja");
		map.put("Students for the Exploration and Development of Space (SEDS)", "https://rockchalkcentral.ku.edu/organization/seds");
		map.put("Self Engineering Leadership Fellows", "https://engr.ku.edu/self/");
		map.put("Society of Petroleum Engineers (SPE)", "https://rockchalkcentral.ku.edu/organization/spe");
		map.put("Society of Women Engineers", "http://groups.ku.edu/~swe/");
		map.put("Steel Bridge", "https://sites.google.com/site/asceku/steel-bridge");
		map.put("Society of Hispanic Professional Engineers (SHPE)", "http://engr.ku.edu/~kushpe/about.htm");
		map.put("Sigma Gamma Tau (ΣΓΤ)", "https://rockchalkcentral.ku.edu/organization/sigmagammatau");
		map.put("Theta Tau (ΘΤ)", "http://www.kuthetatau.org/");
		map.put("U.S. Green Building Council (USGBC)", "https://rockchalkcentral.ku.edu/organization/kuusgbc");
		return Collections.unmodifiableMap(map);
	}
}