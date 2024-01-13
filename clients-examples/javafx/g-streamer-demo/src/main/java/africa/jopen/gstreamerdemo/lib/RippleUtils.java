package africa.jopen.gstreamerdemo.lib;

import java.util.UUID;

public class RippleUtils {
    public static String IdGenerator() {
        UUID uuid = UUID.randomUUID();
	    return uuid.toString().replaceAll("-", "") + System.nanoTime();
    }
}
