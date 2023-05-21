package africa.jopen.utils;

import com.google.common.flogger.FluentLogger;
import io.helidon.common.http.Http;
import io.helidon.webserver.ServerResponse;
import jakarta.json.*;

import java.util.Collections;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

public class XUtils {
	private XUtils() {
	}
	
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
	
	public static String SERVER_NAME = "SERVER_ONE";
	
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
					.add("success", false)
					.add("message", message)
					.add("code", code)
					.build();
			response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
		} else {
			
			if (ex.getCause() instanceof JsonException) {
				
				logger.atFine().withCause(ex).log("Invalid JSON");
				JsonObject jsonErrorObject = JSON.createObjectBuilder()
						.add("error", "Invalid JSON")
						.add("success", false)
						.add("message", message)
						.add("code", Http.Status.BAD_REQUEST_400.code())
						.build();
				response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
			} else {
				
				logger.atSevere().withCause(ex).log("Internal error");

				JsonObject jsonErrorObject = JSON.createObjectBuilder()
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
	
	
}
