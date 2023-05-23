package africa.jopen.utils;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

public class XUtils {

	
	public static String SERVER_NAME = "SERVER_ONE";
	
	public static String IdGenerator() {
		UUID uuid = UUID.randomUUID();
		
		// Get the current time in milliseconds
		long currentTime = System.currentTimeMillis();
		
		// Convert the time to a string
		String timeString = String.valueOf(currentTime);
		
		// Remove dashes from UUID and concatenate with time string
		String concatenated = uuid.toString().replaceAll("-", "") + timeString;
		return concatenated.replaceAll("-", "");
	}
	
	
	public static Response buildErrorResponse(boolean success, int code, String message, Map<String, Object> data) {
		return Response.status(Response.Status.BAD_REQUEST)
				.entity(buildResponseMap(success, code, message, data))
				.build();
	}
	
	public static Response buildSuccessResponse(boolean success, int code, String message, Map<String, Object> data) {
		return Response.ok()
				.entity(buildResponseMap(success, code, message, data))
				.build();
	}
	
	private static Map<String, Object> buildResponseMap(boolean success, int code, String message, Map<String, Object> data) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", success);
		response.put("timeZoneName", TimeZone.getDefault().getDisplayName() );
		response.put("timeZone", TimeZone.getDefault().toZoneId() /*"Africa/Johannesburg"*/);
		response.put("serverName", SERVER_NAME);
		response.put("timeStamp", System.currentTimeMillis());
		response.put("code", code);
		response.put("message", message);
		response.put("data", data);
		return response;
	}
	public static JSONObject buildJsonErrorResponse(int code,String messageTypeTitle, String messageType, String message, JSONObject data) {
		JSONObject response = new JSONObject();
		response.put("timeZoneName", TimeZone.getDefault().getDisplayName() );
		response.put("timeZone", TimeZone.getDefault().toZoneId() /*"Africa/Johannesburg"*/);
		response.put("serverName", SERVER_NAME);
		response.put("timeStamp", System.currentTimeMillis());
		response.put(messageTypeTitle,messageType);
		response.put("message", message);
		response.put("success", false);
		response.put("code", code);
		response.put("data",data);
		return  response;
	}
	public static JSONObject buildJsonSuccessResponse(int code,String messageTypeTitle, String messageType, String message, JSONObject data) {
		JSONObject response = new JSONObject();
		response.put("timeZoneName", TimeZone.getDefault().getDisplayName() );
		response.put("timeZone", TimeZone.getDefault().toZoneId() /*"Africa/Johannesburg"*/);
		response.put("serverName", SERVER_NAME);
		response.put("timeStamp", System.currentTimeMillis());
		response.put(messageTypeTitle,messageType);
		response.put("message", message);
		response.put("success", true);
		response.put("code", code);
		response.put("data",data);
		return  response;
	}

	public static final class GStreamerUtils{

		private GStreamerUtils(){}

		/**
		 * Configures paths to the GStreamer libraries. On Windows queries various
		 * GStreamer environment variables, and then sets up the PATH environment
		 * variable. On macOS, adds the location to jna.library.path (macOS binaries
		 * link to each other). On both, the gstreamer.path system property can be
		 * used to override. On Linux, assumes GStreamer is in the path already.
		 */
		public static void configurePaths() {
			if (Platform.isWindows()) {
				String gstPath = System.getProperty("gstreamer.path", findWindowsLocation());
				if (!gstPath.isEmpty()) {
					String systemPath = System.getenv("PATH");
					if (systemPath == null || systemPath.trim().isEmpty()) {
						Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath);
					} else {
						Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath
								+ File.pathSeparator + systemPath);
					}
				}
			} else if (Platform.isMac()) {
				String gstPath = System.getProperty("gstreamer.path",
						"/Library/Frameworks/GStreamer.framework/Libraries/");
				if (!gstPath.isEmpty()) {
					String jnaPath = System.getProperty("jna.library.path", "").trim();
					if (jnaPath.isEmpty()) {
						System.setProperty("jna.library.path", gstPath);
					} else {
						System.setProperty("jna.library.path", jnaPath + File.pathSeparator + gstPath);
					}
				}

			}
		}

		/**
		 * Query over a stream of possible environment variables for GStreamer
		 * location, filtering on the first non-null result, and adding \bin\ to the
		 * value.
		 *
		 * @return location or empty string
		 */
		static String findWindowsLocation() {
			if (Platform.is64Bit()) {
				return Stream.of("GSTREAMER_1_0_ROOT_MSVC_X86_64",
								"GSTREAMER_1_0_ROOT_MINGW_X86_64",
								"GSTREAMER_1_0_ROOT_X86_64")
						.map(System::getenv)
						.filter(p -> p != null)
						.map(p -> p.endsWith("\\") ? p + "bin\\" : p + "\\bin\\")
						.findFirst().orElse("");
			} else {
				return "";
			}
		}

	}
	
	
}
