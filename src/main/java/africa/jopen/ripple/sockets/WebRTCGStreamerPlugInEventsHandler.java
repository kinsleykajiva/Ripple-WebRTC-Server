package africa.jopen.ripple.sockets;

import africa.jopen.ripple.models.Client;
import africa.jopen.ripple.models.MediaFile;
import africa.jopen.ripple.plugins.WebRTCGStreamerPlugIn;
import africa.jopen.ripple.utils.XUtils;
import io.helidon.websocket.WsSession;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WebRTCGStreamerPlugInEventsHandler {
	private static final Logger log = Logger.getLogger(WebRTCGStreamerPlugInEventsHandler.class.getName());
	
	static void answer(WsSession session, Client client, JSONObject jsonObject, String transaction, boolean last) {
		handleRequest(session, jsonObject, transaction, last,
				json -> json.has("answer") && json.has("threadRef"),
				json -> {
					WebRTCGStreamerPlugIn plugin = getPluginOrHandleError(client, json, session, transaction);
					if (plugin != null) {
						plugin.setTransaction(transaction);
						plugin.handleSdp(json.getString("answer"));
					}
				});
	}
	
	static void iceCandidate(WsSession session, Client client, JSONObject jsonObject, String transaction, boolean last) {
		handleRequest(session, jsonObject, transaction, last,
				json -> json.has("candidate") && json.has("sdpMLineIndex") && json.has("threadRef"),
				json -> {
					WebRTCGStreamerPlugIn plugin = getPluginOrHandleError(client, json, session, transaction);
					if (plugin != null) {
						plugin.setTransaction(transaction);
						plugin.handleIceSdp(json.getString("candidate"), json.getInt("sdpMLineIndex"));
					}
				});
	}
	
	static void newThread(WsSession session, Client client, JSONObject jsonObject, String transaction, boolean last) {
		JSONObject reqstData = jsonObject.getJSONObject("data");
		
		if (jsonObject.getString("feature").equals("G_STREAM_BROADCASTER")) {
			String filePath = XUtils.MAIN_CONFIG_MODEL.storagePath().media() + reqstData.getString("file");
			
			if (new File(filePath).exists()) {
				int thread = client.createAccessGStreamerPlugIn(new MediaFile(filePath, 0));
				client.replyToNewThreadRequest(transaction, thread, jsonObject.getString("feature"));
			} else {
				log.info("File not found: " + filePath);
				client.replyToNewThreadInvalidRequest(transaction, -1);
			}
		}
	}
	
	static void startBroadCast(WsSession session, Client client, JSONObject jsonObject, String transaction, boolean last) {
		handleRequest(session, jsonObject, transaction, last,
				json -> json.has("threadRef"),
				json -> getPluginOptional(client, json, session, transaction)
						.ifPresent(plugin -> {
							plugin.setTransaction(transaction);
							plugin.startCall();
						}));
	}
	
	static void resume(WsSession session, Client client, JSONObject jsonObject, String transaction, boolean last) {
		handleRequest(session, jsonObject, transaction, last,
				json -> json.has("threadRef"),
				json -> getPluginOptional(client, json, session, transaction)
						.ifPresent(plugin -> {
							plugin.setTransaction(transaction);
							plugin.resumeTransmission();
						}));
	}
	
	static void decreaseVolume(WsSession session, Client client, JSONObject jsonObject, String transaction, boolean last) {
		handleRequest(session, jsonObject, transaction, last,
				json -> json.has("threadRef"),
				json -> getPluginOptional(client, json, session, transaction)
						.ifPresent(plugin -> {
							plugin.setTransaction(transaction);
							plugin.decreaseVolume();
						}));
	}
	
	static void increaseVolume(WsSession session, Client client, JSONObject jsonObject, String transaction, boolean last) {
		handleRequest(session, jsonObject, transaction, last,
				json -> json.has("threadRef"),
				json -> getPluginOptional(client, json, session, transaction)
						.ifPresent(plugin -> {
							plugin.setTransaction(transaction);
							plugin.increaseVolume();
						}));
	}
	
	static void pause(WsSession session, Client client, JSONObject jsonObject, String transaction, boolean last) {
		handleRequest(session, jsonObject, transaction, last,
				json -> json.has("threadRef"),
				json -> getPluginOptional(client, json, session, transaction)
						.ifPresent(plugin -> {
							plugin.setTransaction(transaction);
							plugin.pauseTransmission();
						}));
	}
	
	private static void handleRequest(WsSession session, JSONObject jsonObject, String transaction, boolean last,
	                                  Predicate<JSONObject> validator, Consumer<JSONObject> handler) {
		if (validator.test(jsonObject)) {
			handler.accept(jsonObject);
		} else {
			sendErrorResponse(session, jsonObject, transaction, "Invalid request", last);
		}
	}
	
	private static void sendErrorResponse(WsSession session, JSONObject jsonObject, String transaction, String message, boolean last) {
		session.send(
				new JSONObject()
						.put("success", false)
						.put("eventType", jsonObject.getString("requestType"))
						.put("accessAuth", "GENERAL")
						.put("message", message)
						.put("transaction", transaction)
						.put("clientID", jsonObject.getString("clientID"))
						.toString()
				, last);
	}
	
	private static WebRTCGStreamerPlugIn getPluginOrHandleError(Client client, JSONObject jsonObject, WsSession session, String transaction) {
		Map<Integer, WebRTCGStreamerPlugIn> webRTCStreamMap = client.getWebRTCStreamMap();
		WebRTCGStreamerPlugIn plugin = webRTCStreamMap.get(jsonObject.getInt("threadRef"));
		
		if (Objects.isNull(plugin)) {
			sendErrorResponse(session, jsonObject, transaction, "Required threadRef Value not found, Reference error", true);
		}
		
		return plugin;
	}
	
	private static Optional<WebRTCGStreamerPlugIn> getPluginOptional(Client client, JSONObject jsonObject, WsSession session, String transaction) {
		return Optional.ofNullable(client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef")))
				.or(() -> {
					sendErrorResponse(session, jsonObject, transaction, "Required threadRef Value not found, Reference error", true);
					return Optional.empty();
				});
	}
}