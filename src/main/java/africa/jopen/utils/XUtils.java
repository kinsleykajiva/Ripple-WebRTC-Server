package africa.jopen.utils;

import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;
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


    public static Response buildErrorResponse(boolean success, int code, String message,Map<String, Object> data) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(buildResponseMap(success, code, message,data))
                .build();
    }

    public static Response buildSuccessResponse(boolean success, int code, String message ,Map<String, Object> data ) {
        return Response.ok()
                .entity(buildResponseMap(success, code, message,data))
                .build();
    }

    private static Map<String, Object> buildResponseMap(boolean success, int code, String message,Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("code", code);
        response.put("message", message);
        response.put("data", data);
        return response;
    }
}
