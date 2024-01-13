package africa.jopen.gstreamerdemo.lib;

import africa.jopen.gstreamerdemo.App;

import java.time.LocalTime;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class RippleUtils {
	
	static Logger log = Logger.getLogger(RippleUtils.class.getName());
	
	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int    ALPHABET_SIZE = ALPHABET.length();
	private static final Random RANDOM        = new Random();
    protected static String IdGenerator() {
        UUID uuid = UUID.randomUUID();
	    return uuid.toString().replaceAll("-", "") + System.nanoTime();
    }
	protected static String uniqueIDGenerator(String seed, int maxSize) {
		LocalTime time       = LocalTime.now();
		String    timeString = String.format("%02d%02d%02d", time.getHour(), time.getMinute(), time.getSecond());
		String dateTimeString = (seed + timeString + RANDOM.nextInt(10000)).substring(0, 12);
		
		StringBuilder uniID = new StringBuilder();
		for (int i = 0; i < maxSize; i++) {
			uniID.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET_SIZE)));
		}
		
		for (int i = 0; i < dateTimeString.length(); i++) {
			int index = Character.digit(dateTimeString.charAt(i), 36);
			uniID.replace(index, index + 1, String.valueOf(dateTimeString.charAt(i)));
		}
		
		return uniID.toString().replaceAll("[^a-z0-9]", "");
	}
	
	
}
