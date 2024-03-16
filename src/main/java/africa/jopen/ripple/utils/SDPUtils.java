package africa.jopen.ripple.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SDPUtils {
	
	
	public static void parseSDP(String sdpMessage) {
		String       ipAddressInOriginLine = extractIPAddressFromOriginLine(sdpMessage);
		String       sessionName           = extractSessionName(sdpMessage);
		String       fingerprintValue = extractFingerprintValue(sdpMessage);
		List<String> rtpmapLines      = extractRtpmapLines(sdpMessage);
		List<String> extmapLines      = extractExtmapLines(sdpMessage);
		
		System.out.println("IP Address in o= line: " + ipAddressInOriginLine);
		System.out.println("Session Name in s= line: " + sessionName);
		System.out.println("Fingerprint Value: " + fingerprintValue);
		System.out.println("a=rtpmap lines:");
		for (String line : rtpmapLines) {
			System.out.println(line);
		} System.out.println("a=extmapLines lines:");
		for (String line : extmapLines) {
			System.out.println(line);
		}
	}
	public static List<String> extractValuesFromOriginLine( String sdpMessage ) {
		Pattern pattern = Pattern.compile("o=- ([^ ]+) ([^ ]+) IN IP4 [^\\n]+");
		Matcher matcher = pattern.matcher(sdpMessage);
		List<String> values = new ArrayList<>();
		if (matcher.find()) {
			values.add(matcher.group(1)); // 1237562358473204055
			values.add(matcher.group(2)); // 2
		}
		return values;
	}
	public static String extractIPAddressFromOriginLine(String sdpMessage) {
		Pattern pattern = Pattern.compile("o=[^ ]+ [^ ]+ [^ ]+ IN IP4 ([^\\n]+)");
		Matcher matcher = pattern.matcher(sdpMessage);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return null;
	}
	public static List<String> extractExtmapLines(String sdpMessage) {
		Pattern pattern = Pattern.compile("a=extmap:(.*)");
		Matcher matcher = pattern.matcher(sdpMessage);
		List<String> extmapLines = new ArrayList<>();
		while (matcher.find()) {
			extmapLines.add(matcher.group(0));
		}
		return extmapLines;
	}
	public static String extractSessionName(String sdpMessage) {
		Pattern pattern = Pattern.compile("s=(.*)");
		Matcher matcher = pattern.matcher(sdpMessage);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}
	
	public static String extractFingerprintValue(String sdpMessage) {
		Pattern pattern = Pattern.compile("a=fingerprint:sha-256\\s(.*)");
		Matcher matcher = pattern.matcher(sdpMessage);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}
	
	public static List<String> extractRtpmapLines(String sdpMessage) {
		Pattern pattern = Pattern.compile("a=rtpmap:(.*)");
		Matcher matcher = pattern.matcher(sdpMessage);
		List<String> rtpmapLines = new ArrayList<>();
		while (matcher.find()) {
			rtpmapLines.add(matcher.group(0));
		}
		return rtpmapLines;
	}
	
}
