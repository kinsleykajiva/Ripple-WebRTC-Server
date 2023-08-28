package africa.jopen.sockets.events;

import africa.jopen.http.IceCandidate;
import africa.jopen.http.videocall.PostIceCandidate;
import africa.jopen.http.videocall.PostSDPOffer;
import africa.jopen.models.Client;
import africa.jopen.models.VideCallNotification;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.Events;
import africa.jopen.utils.Requests;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static africa.jopen.sockets.ClientWebSocket.broadcast;
import static africa.jopen.sockets.ClientWebSocket.rememberResponse;
import static africa.jopen.utils.XUtils.buildErrorResponse;
import static africa.jopen.utils.XUtils.buildSuccessResponse;

public class VideoCallSocketEvent {
	private static final FluentLogger       logger             = FluentLogger.forEnclosingClass();
	private static final ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
	
	public static void handleVideoCallRequest(Client client, JSONObject messageObject, JSONObject response) {
		final String requestType = messageObject.getString(Requests.REQUEST_TYPE);
		final String toClientID ="toClientID";
		final String fromClientID ="fromClientID";
		response.put("history", messageObject);
		switch (requestType) {
			case Requests.REMEMBER -> response = rememberResponse(connectionsManager, client);

			case Requests.ANSWER_CALL -> {
				// this event is only from the called client
				var testToClientExists   = connectionsManager.checkIfClientExists(messageObject.getString(toClientID));
				var testFromClientExists = connectionsManager.checkIfClientExists(messageObject.getString(fromClientID));
				final var notificationID = messageObject.getString("notificationID"); // ToDo check what to do with this as much for cleaning reasons.

				if (!testToClientExists) {
					response = XUtils.buildJsonErrorResponse(Requests.SERVER_ERROR, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "Target Client Not Found !", response);
					broadcast(client, response.toString());
					return;
				}

				if (!testFromClientExists) {
					response = XUtils.buildJsonErrorResponse(Requests.SERVER_ERROR, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "You the attempting Client Not Found, Register again to the server !", response);
					broadcast(client, response.toString());
					return;
				}
				var fromClientOptional = connectionsManager.getClient(messageObject.getString(fromClientID));
				assert fromClientOptional.isPresent();
				response.put("notificationID", notificationID);
				response = XUtils.buildJsonSuccessResponse(200, Events.EVENT_TYPE, Events.CALL_ANSWERED_NOTIFICATION_EVENT, "Call Answered ", response);
				broadcast(fromClientOptional.get(), response.toString());
				return;// this return is required as this will only send this message to the other client !

			}
			case Requests.MAKE_CALL ->  {
				// ToDo will need to check if this target client is in a call or not then respond accordingly.
				var testToClientExists   = connectionsManager.checkIfClientExists(messageObject.getString(toClientID));
				var testFromClientExists = connectionsManager.checkIfClientExists(messageObject.getString(fromClientID));

				if (!testToClientExists) {
					response = XUtils.buildJsonErrorResponse(Requests.SERVER_ERROR, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "Target Client Not Found !", response);
					broadcast(client, response.toString());
					return;
				}
				
				if (!testFromClientExists) {
					response = XUtils.buildJsonErrorResponse(Requests.SERVER_ERROR, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "You the attempting Client Not Found, Register again to the server !", response);
					broadcast(client, response.toString());
					return;
				}
				var fromClientOptional = connectionsManager.getClient(messageObject.getString(fromClientID));
				assert fromClientOptional.isPresent();

				// send this as part of the next remember cycle of the target Client
				long start = System.currentTimeMillis();
				long life = TimeUnit.SECONDS.toMillis(20);
				long end = start + life;
				var toClient = connectionsManager.getClient(messageObject.getString(toClientID));
				assert toClient.isPresent();
				var notification = new VideCallNotification(XUtils.IdGenerator(), fromClientOptional.get().getClientAgentName(), messageObject.getString("fromClientID"), messageObject.getString("fromClientID"), start, end);

				response.put("videoCall", notification);
				response = XUtils.buildJsonSuccessResponse(Requests.OK_RESPONSE, Events.EVENT_TYPE, Events.INCOMING_CALL_NOTIFICATION_EVENT, "Getting incoming call ", response);
				broadcast(toClient.get(), response.toString());

				response = new JSONObject();
				response.put("videoCall", notification);
				response = XUtils.buildJsonSuccessResponse(Requests.OK_RESPONSE, Events.EVENT_TYPE, Events.NOTIFICATION_EVENT, "Client notified, call in progress!", response);

			}
			case Requests.HANGUP ->  {
				response.put("nextActions", Arrays.asList("closePeerConnection", "hangup"));
				response = XUtils.buildJsonSuccessResponse(Requests.OK_RESPONSE, Events.EVENT_TYPE, Events.NOTIFICATION_EVENT,"Call ended", response);
			}
			case Requests.UPDATE_ICE_CANDIDATE -> {
				var payload = new PostIceCandidate(
						new IceCandidate(
								messageObject.getString("candidate"),
								messageObject.getString("sdpMid"),
								messageObject.getInt("sdpMidLineIndex")
						), client.getClientID()
				);
				client.addIceCandidate(payload.iceCandidate());
				connectionsManager.updateClient(client);
				
				response = XUtils.buildJsonSuccessResponse(Requests.OK_RESPONSE, Events.EVENT_TYPE, Events.NOTIFICATION_EVENT,"Updated Clients Ice Candidates ", response);
			}
			case Requests.SEND_OFFER -> {
				PostSDPOffer payload        = new PostSDPOffer(messageObject.getString("offer"), client.getClientID());
				var          clientOptional = connectionsManager.getClient(payload.clientID());
				
				assert clientOptional.isPresent();
				
				var clientObject = clientOptional.get();
				CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
					String responseAnswer = clientObject.processSdpOfferAsRemoteDescription();
					connectionsManager.updateClient(clientObject);
					return responseAnswer;
				});
				
				try {
					// Retrieve the response from the CompletableFuture
					var sdp = future.get();
					response.put("sdp", sdp);
					response = XUtils.buildJsonSuccessResponse(Requests.OK_RESPONSE, Events.EVENT_TYPE, Events.SDP_ANSWER_EVENT,"SDP Offer processed, here is the answer ", response);
					
				} catch (Exception e) {
					logger.atInfo().withCause(e).log("Error");
					response = XUtils.buildJsonErrorResponse(Requests.SERVER_ERROR, Events.EVENT_TYPE, Events.ERROR_EVENT,"Error processing SDP Offer", response);
				}
			}
		}
		broadcast(client, response.toString());
	}
	
}
