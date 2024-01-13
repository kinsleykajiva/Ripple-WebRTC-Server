package africa.jopen.gstreamerdemo.lib;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class RippleUtils {
	
	static Logger log = Logger.getLogger(RippleUtils.class.getName());
	
	private static final String ALPHABET      = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int    ALPHABET_SIZE = ALPHABET.length();
	private static final Random RANDOM        = new Random();
	
	protected static String IdGenerator() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString().replaceAll("-", "") + System.nanoTime();
	}
	
	protected static String nonAlphaNumeric(String str) {
		return str.replaceAll("[^a-zA-Z0-9]", "");
	}
	
	public static String convertToWebSocketUrl(String url) {
		if (url == null || url.isEmpty()) return "";
		if (url.startsWith("http://")) return url.replace("http://", "ws://");
		if (url.startsWith("https://")) return url.replace("https://", "wss://");
		return "";
	}
	
	public static String getWebSocketUrl(String url) {
		if (url == null || url.isEmpty()) return "";
		if (url.startsWith("ws://")) return url;
		if (url.startsWith("wss://")) return url;
		return "";
	}
	
	public static String getWebSocketProtocol(String url) {
		if (url == null || url.isEmpty()) return "";
		if (url.startsWith("ws://")) return "ws";
		if (url.startsWith("wss://")) return "wss";
		return "";
	}
	
	public static String getWebSocketHost(String url) {
		if (url == null || url.isEmpty()) return "";
		if (url.startsWith("ws://")) return url.substring(5);
		if (url.startsWith("wss://")) return url.substring(6);
		return "";
	}
	
	public static String getWebSocketPort(String url) {
		if (url == null || url.isEmpty()) return "";
		if (url.startsWith("ws://")) return "";
		if (url.startsWith("wss://")) return "";
		return "";
	}
	
	public static String convertFromWebSocketUrl(String url) {
		if (url == null || url.isEmpty()) return "";
		if (url.startsWith("ws://")) return url.replace("ws://", "http://");
		if (url.startsWith("wss://")) return url.replace("wss://", "https://");
		return "";
		
	}
	
	public static boolean isJson(String str) {
		try {
			new JSONObject(str);
		} catch (JSONException ex) {
			try {
				new JSONArray(str);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}
	
	protected static String uniqueIDGenerator(String seed, int maxSize) {
		LocalTime time           = LocalTime.now();
		String    timeString     = String.format("%02d%02d%02d", time.getHour(), time.getMinute(), time.getSecond());
		String    dateTimeString = seed + timeString + RANDOM.nextInt(10000);
		if (dateTimeString.length() > 12) {
			dateTimeString = dateTimeString.substring(0, 12);
		}
		StringBuilder uniID = new StringBuilder();
		for (int i = 0; i < maxSize; i++) {
			uniID.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET_SIZE)));
		}
		
		for (int i = 0; i < dateTimeString.length(); i++) {
			int index = Character.digit(dateTimeString.charAt(i), 36);
			if (index < uniID.length()) {
				uniID.replace(index, index + 1, String.valueOf(dateTimeString.charAt(i)));
			}
		}
		
		return uniID.toString().replaceAll("[^a-z0-9]", "");
	}
	
	
}
