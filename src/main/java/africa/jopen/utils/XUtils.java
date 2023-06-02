package africa.jopen.utils;

import com.google.common.flogger.FluentLogger;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import io.helidon.common.http.Http;
import io.helidon.webserver.ServerResponse;
import jakarta.json.*;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

public class XUtils {
	private XUtils() {
	}
	public static boolean isFilePath(String path) {
		try {
			// Attempt to resolve the path
			Paths.get(path);
			return true; // If no exception, it's a valid file path
		} catch (InvalidPathException e) {
			return false;
		}
	}

	public static boolean isURL(String path) {
		try {
			new URL(path); // Attempt to parse the string as a URL
			return true; // If no exception, it's a valid URL
		} catch (MalformedURLException e) {
			return false;
		}
	}
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
	
	public static String SERVER_NAME = "SERVER_ONE";
	public static String SERVER_VERSION = "";
	
	public static String IdGenerator() {
		UUID uuid = UUID.randomUUID();
		// Remove dashes from UUID and concatenate with time string
		String concatenated = uuid.toString().replaceAll("-", "") + System.currentTimeMillis();
		return concatenated.replaceAll("-", "");
	}
	
	/**
	 * [Brief description of the deprecated method]
	 * <p>
	 * [Explanation of the deprecation and reason behind it]
	 * <p>
	 * [Provide any relevant context or background information]
	 *
	 * @deprecated [Explanation of the deprecation]
	 * [Mention the alternative method or approach]
	 * Please use {@link XUtils#newMethod()} instead.
	 * [Provide guidance on migrating from the deprecated method to the new one]
	 * [Include examples or code snippets if necessary]
	 * [Specify version information]
	 */
	@Deprecated
	public void deprecatedMethod() {
		// Deprecated method implementation
	}
	
	
	public static <T> T buildErrorResponse(Throwable ex, ServerResponse response, int code, String message) {
		
		if (Objects.isNull(ex)) {
			JsonObject jsonErrorObject = JSON.createObjectBuilder()
					                             .add("meta", XUtils.SERVER_VERSION)
					                             .add("success", false)
					                             .add("message", message)
					                             .add("code", code)
					                             .build();
			response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
		} else {
			
			if (ex.getCause() instanceof JsonException) {
				
				logger.atFine().withCause(ex).log("Invalid JSON");
				JsonObject jsonErrorObject = JSON.createObjectBuilder()
						                             .add("meta", XUtils.SERVER_VERSION)
						                             .add("error", "Invalid JSON")
						                             .add("success", false)
						                             .add("message", message)
						                             .add("code", Http.Status.BAD_REQUEST_400.code())
						                             .build();
				response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
			} else {
				
				logger.atSevere().withCause(ex).log("Internal error");
				
				JsonObject jsonErrorObject = JSON.createObjectBuilder()
						                             .add("meta", XUtils.SERVER_VERSION)
						                             .add("error", ex.getMessage())
						                             .add("success", false)
						                             .add("message", message)
						                             .add("code", Http.Status.BAD_REQUEST_400.code())
						                             .build();
				response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(jsonErrorObject);
			}
		}
		
		return null;
	}
	
	public static <T> T buildErrorResponse2(Throwable ex, ServerResponse response, int code, String message) {
		JsonObjectBuilder jsonErrorBuilder = JSON.createObjectBuilder()
				                                     .add("meta", XUtils.SERVER_VERSION)
				                                     .add("success", false)
				                                     .add("message", message)
				                                     .add("code", code);
		
		if (Objects.isNull(ex)) {
			jsonErrorBuilder.add("error", "Unknown error");
		} else {
			if (ex.getCause() instanceof JsonException) {
				logger.atFine().withCause(ex).log("Invalid JSON");
				jsonErrorBuilder.add("error", "Invalid JSON");
			} else {
				logger.atFine().withCause(ex).log("Internal error");
				jsonErrorBuilder.add("error", "Internal error");
			}
		}
		
		JsonObject jsonErrorObject = jsonErrorBuilder.build();
		response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
		
		return null;
	}
	
	
	public static void buildSuccessResponse(ServerResponse response, int code, String message, JsonObject data) {
		JsonObject returnObject = JSON.createObjectBuilder()
				                          .add("meta", XUtils.SERVER_VERSION)
				                          .add("success", true)
				                          .add("timeZoneName", TimeZone.getDefault().getDisplayName())
				                          .add("timeZone", TimeZone.getDefault().toZoneId().toString())
				                          .add("serverName", SERVER_NAME)
				                          .add("timeStamp", System.currentTimeMillis())
				                          .add("code", code)
				                          .add("message", message)
				                          .add("data", data)
				                          .build();
		response.send(returnObject);
	}

	public static JSONObject buildJsonErrorResponse(int code, String messageTypeTitle, String messageType, String message, JSONObject data) {
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
