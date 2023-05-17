package africa.jopen.utils;

import java.util.UUID;

public class XUtils {

  public static   String IdGenerator(){
        UUID uuid = UUID.randomUUID();

        // Get the current time in milliseconds
        long currentTime = System.currentTimeMillis();

        // Convert the time to a string
        String timeString = String.valueOf(currentTime);

        // Remove dashes from UUID and concatenate with time string
        String concatenated = uuid.toString().replaceAll("-", "") + timeString;
        return  concatenated.replaceAll("-" , "");
    }
}
