package africa.jopen.ripple.utils;

import org.json.JSONObject;

import java.util.UUID;
import java.util.logging.Logger;

public class XUtils {
	private static final Logger LOGGER = Logger.getLogger(XUtils.class.getName());
	
	static {
		LoggerConfig.setupLogger(LOGGER);
	}
	
	private XUtils() {
	}
	
	public static String IdGenerator() {
		UUID uuid = UUID.randomUUID();
		// Remove dashes from UUID and concatenate with time string
		String concatenated = uuid.toString().replaceAll("-", "") + System.currentTimeMillis();
		return concatenated.replaceAll("-", "");
	}
	
	public static JSONObject failedResponse(String transactionId, String message, JSONObject data){
		LOGGER.info("Building error/failed response");
		
		JSONObject returnObject = new JSONObject()
				.put("success", false)
				.put("transactionId", transactionId)
				.put("timeStamp", System.currentTimeMillis())
				.put("message", message)
				.put("data", data);
		
		return returnObject;
	}
	public static JSONObject successResponse(String transactionId, String message, JSONObject data) {
		LOGGER.info("Building success response");
		
		JSONObject returnObject = new JSONObject()
				.put("success", true)
				.put("transactionId", transactionId)
				.put("timeStamp", System.currentTimeMillis())
				.put("message", message)
				.put("data", data);
		
		return returnObject;
	}
	
}
