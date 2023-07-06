package africa.jopen.sockets.events;

import africa.jopen.http.IceCandidate;
import africa.jopen.http.videoroom.PostIceCandidate;
import africa.jopen.http.videoroom.PostSDPAnswer;
import africa.jopen.models.Client;
import africa.jopen.models.GStreamMediaResource;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.Events;
import africa.jopen.utils.Requests;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

import static africa.jopen.sockets.ClientWebSocket.broadcast;
import static africa.jopen.sockets.ClientWebSocket.rememberResponse;

public class GStreamsSocketEvent {
	private static final FluentLogger       logger             = FluentLogger.forEnclosingClass();
	private static final ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
	
	/**
	 * @param clientObject
	 * @param messageObject
	 * @param response
	 */
	public static void handleGStreamRequest(Client clientObject, JSONObject messageObject, JSONObject response) {
		
		final String requestType = messageObject.getString(Requests.REQUEST_TYPE);
		response.put("history", messageObject);
		switch (requestType) {
			case "remember" -> response = rememberResponse(connectionsManager, clientObject);
			case "update-ice-candidate" -> {
				final var iceCandidate = new IceCandidate(messageObject.getString("candidate"),messageObject.getString("sdpMid"),messageObject.getInt("sdpMLineIndex"));
				final var payload = new PostIceCandidate(null,iceCandidate,messageObject.getString("clientID"));
				clientObject.getWebRTCGStreamer().handleIceSdp(payload.iceCandidate().candidate(), payload.iceCandidate().sdpMidLineIndex());
			}
			case "send-answer" -> {
				final var payload = new PostSDPAnswer(null,messageObject.getString("answer"),messageObject.getString("clientID"));
				clientObject.getWebRTCGStreamer().handleSdp(payload.answer());
				connectionsManager.updateClient(clientObject);
				
				response = XUtils.buildJsonSuccessResponse(200, Events.EVENT_TYPE, Events.NOTIFICATION_EVENT, "Client answered Successfully", response);
			}
			case "play" -> {
				clientObject.getWebRTCGStreamer().startCall();
				response = XUtils.buildJsonSuccessResponse(200, Events.EVENT_TYPE, Events.NOTIFICATION_EVENT, "Call Started", response);
			}
			case "pause" -> {
				
				clientObject.getWebRTCGStreamer().pauseTransmission();
				response = XUtils.buildJsonSuccessResponse(200, Events.EVENT_TYPE, Events.NOTIFICATION_EVENT, "Call paused", response);
				
			}
			case "resume" -> {
				
				clientObject.getWebRTCGStreamer().resumeTransmission();
				response = XUtils.buildJsonSuccessResponse(200, Events.EVENT_TYPE, "notification", "Call resumed", response);
			}
			
			case "start" -> {
				if (!messageObject.has("clientID")) {
					response = XUtils.buildJsonErrorResponse(400, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "clientID is required", response);
					broadcast(clientObject, response.toString());
					return;
				}
				if (!messageObject.has("media")) {
					response = XUtils.buildJsonErrorResponse(400, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "media Object is required", response);
					broadcast(clientObject, response.toString());
					return;
				}
				JSONObject mediaJSON = messageObject.getJSONObject("media");
				if (!mediaJSON.has("path")) {
					response = XUtils.buildJsonErrorResponse(400, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "media Path is required", response);
					broadcast(clientObject, response.toString());
					return;
				}
				response.put("nextActions", Arrays.asList("createPeerConnection", "shareIceCandidates", "play"));
				try {
					final var path = mediaJSON.getString("path");
					if (!new File(path).exists()) {
						logger.atInfo().log("File not found error");
						response = XUtils.buildJsonErrorResponse(400, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "media Path is invalid ", response);
						broadcast(clientObject, response.toString());
						return;
					}
					var media = new GStreamMediaResource(mediaJSON.getString("title"), path);
					clientObject.setWebRTCGStreamer(media);
					connectionsManager.updateClient(clientObject);
					response = XUtils.buildJsonSuccessResponse(200, Events.EVENT_TYPE, Events.NOTIFICATION_EVENT, "Streaming Started Successfully, the app should start to receive some streams,the Server Is preparing WebRTC stuff", response);
					
				} catch (Exception e) {
					logger.atSevere().withCause(e).log("Failed to make a pipeline");
					response = XUtils.buildJsonErrorResponse(500, Events.EVENT_TYPE, Events.ERROR_EVENT, "Failed to process the video , there will no stream to see ", response);
					broadcast(clientObject, response.toString());
					return;
				}
			}
			default -> {
				response.put("clientID", clientObject.getClientID());
				response = XUtils.buildJsonErrorResponse(400, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "Invalid request type for G_STREAM feature", response);
				broadcast(clientObject, response.toString());
			}
		}
		broadcast(clientObject, response.toString());
	}
	
}
