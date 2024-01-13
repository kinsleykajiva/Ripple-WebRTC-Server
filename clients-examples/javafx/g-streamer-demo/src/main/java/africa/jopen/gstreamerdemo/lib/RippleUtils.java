package africa.jopen.gstreamerdemo.lib;

import java.util.UUID;

public class RippleUtils {
	public static String IdGenerator() {
		UUID uuid = UUID.randomUUID();
		// Remove dashes from UUID and concatenate with time string
		String concatenated = uuid.toString().replaceAll("-", "") + System.currentTimeMillis();
		return concatenated.replaceAll("-", "");
	}
}
