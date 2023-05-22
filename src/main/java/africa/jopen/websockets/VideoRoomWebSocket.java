package africa.jopen.websockets;

import africa.jopen.controllers.VideoRoomController;
import africa.jopen.http.videoroom.PostCreateRoom;
import africa.jopen.http.videoroom.PostJoinRoom;
import africa.jopen.http.videoroom.PostSDPOffer;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

@ServerEndpoint("/video-room/{clientID}")
@ApplicationScoped
public class VideoRoomWebSocket {
	@Inject
	ConnectionsManager connectionsManager;
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	
	@OnOpen
	public void onOpen(Session session, @PathParam("clientID") String clientID) {
		System.out.println("onOpen> " + clientID);
		logger.atInfo().log("OnOpen" + clientID);
		// since this is the first . The client id will need to be set after the fact from the server , so the client id wil be the agent name
		try {
			JSONObject response = new JSONObject();
			Client clientObject;
			logger.atInfo().log("..." + clientID);
			var testExists = connectionsManager.checkIfClientExists(clientID);
			if (testExists) {// this is a sub-sequent reconnection.
				clientObject = connectionsManager.updateClientWhenRemembered(clientID);
				clientObject.setSocketSession(session);
				connectionsManager.updateClient(clientObject);
			} else {
				clientObject = new Client(clientID);
				clientObject.setSocketSession(session);
				connectionsManager.addNewClient(clientObject);
			}
			response.put("clientID", clientObject.getClientID());
			response.put("lastSeen", clientObject.lastTimeStamp());
			response.put("eventType", "registration");
			response.put("message", "Client newly connected and recognized");
			response.put("code", 200);
			broadcast(clientObject, response.toString());
			
		} catch (Exception ex) {
			logger.atSevere().withCause(ex).log("OnOpen Error");
		}
	}
	
	@OnClose
	public void onClose(Session session, @PathParam("clientID") String clientID) {
		System.out.println("onClose> " + clientID);
	}
	
	@OnError
	public void onError(Session session, @PathParam("clientID") String clientID, Throwable throwable) {
		System.out.println("onError> " + clientID + ": " + throwable);
	}
	
	@OnMessage
	public void onMessage(String message, @PathParam("clientID") String clientID) {
		JSONObject messageObject;
		logger.atInfo().log("onMessage" + clientID + ": " + message);
		
		var testExists = connectionsManager.checkIfClientExists(clientID);
		if (!testExists) {
			logger.atInfo().log("This ID is illigal, the client will not be notified as we dont have a way to notify the client session");
			// we cant risk to run other existing sessions in the connections list , in-case we send to the wrong client that may have high jacked the session data by some means that i have not yet determined as much yet .
			return;
		}
		
		var clientObjectOptional = connectionsManager.getClient(clientID);
		assert clientObjectOptional.isPresent();
		Client clientObject = clientObjectOptional.get();
		JSONObject response = new JSONObject();
		try {
			logger.atInfo().log("..." + clientID);
			messageObject = new JSONObject(message);
			if (!messageObject.has("requestType")) {
				response.put("clientID", clientObject.getClientID());
				response = XUtils.buildJsonErrorResponse(400, "eventType", "validation",
						"Failed to understand the purpose of the request", response);
				broadcast(clientObject, response.toString());
				return;
			}
			switch (messageObject.getString("requestType")) {
				case "remember":
					clientObject = connectionsManager.updateClientWhenRemembered(clientID);
					response.put("clientID", clientID);
					response.put("lastSeen", clientObject.lastTimeStamp());
					response.put("featureInUse", clientObject.getFeatureType().toString());
					response = XUtils.buildJsonSuccessResponse(200, "eventType", "remember",
							"Client  Remembered Successfully", response);
					break;
				case "joinRoom":
					if (!messageObject.has("password")) {
						response = XUtils.buildJsonErrorResponse(400, "password", "validation",
								"password is required", response);
						broadcast(clientObject, response.toString());
						return;
					}
					if (!messageObject.has("clientID")) {
						response = XUtils.buildJsonErrorResponse(400, "clientID", "validation",
								"clientID is required", response);
						broadcast(clientObject, response.toString());
						return;
					}
					var room = new PostJoinRoom(
							messageObject.getString("roomID"),
							messageObject.getString("password"),
							messageObject.getString("clientID")
					);
					Optional<RoomModel> roomModelOptional = connectionsManager.getRoomById(room.roomID());
					if (roomModelOptional.isEmpty()) {
						response = XUtils.buildJsonErrorResponse(400, "room", "validation",
								"Invalid room ID!", response);
						broadcast(clientObject, response.toString());
						return;
					}
					RoomModel roomModel = roomModelOptional.get();
					if (!roomModel.getPassword().equals(room.password())) {
						response = XUtils.buildJsonErrorResponse(400, "password", "authentication",
								"Room authentication failed! Access rejected.", response);
						broadcast(clientObject, response.toString());
						return;
					}
					VideoRoomController.joinRoom(connectionsManager, room, roomModel);
					response = XUtils.buildJsonSuccessResponse(200, "eventType", "joinRoom",
							"Added to room", response);
					
					break;
				case "createRoom":
					if (!messageObject.has("pin")) {
						response = XUtils.buildJsonErrorResponse(400, "pin", "validation",
								"Pin is required", response);
						broadcast(clientObject, response.toString());
						return;
					}
					if (!messageObject.has("password")) {
						response = XUtils.buildJsonErrorResponse(400, "password", "validation",
								"password is required", response);
						broadcast(clientObject, response.toString());
						return;
					}
					if (!messageObject.has("creatorClientID")) {
						response = XUtils.buildJsonErrorResponse(400, "creatorClientID", "validation",
								"creatorClientID is required", response);
						broadcast(clientObject, response.toString());
						return;
					}
					var post = new PostCreateRoom(messageObject.getString("roomName"),
							messageObject.getString("roomDescription"),
							messageObject.getString("creatorClientID"),
							messageObject.getString("password"),
							messageObject.getString("pin")
					);
					Map<String, Object> resultRoom = VideoRoomController.createRoom(connectionsManager, post, clientObject);
					response.put("room", resultRoom);
					response = XUtils.buildJsonSuccessResponse(200, "eventType", "createRoom",
							"Room created successfully", response);
					
					break;
				case "sendOffer":
					if (!messageObject.has("clientID")) {
						response = XUtils.buildJsonErrorResponse(400, "clientID", "validation",
								"clientID is required", response);
						broadcast(clientObject, response.toString());
						return;
					}
					if (!messageObject.has("offer")) {
						response = XUtils.buildJsonErrorResponse(400, "offer", "validation",
								"offer is required", response);
						broadcast(clientObject, response.toString());
						return;
					}
					var payload = new PostSDPOffer(
							messageObject.getString("roomID"),
							messageObject.getString("offer"),
							messageObject.getString("clientID")
					);
					roomModelOptional = connectionsManager.getRoomById(payload.roomID());
					if (roomModelOptional.isEmpty()) {
						response = XUtils.buildJsonErrorResponse(404, "room", "validation",
								"Room not found!", response);
						broadcast(clientObject, response.toString());
						return;
					}
					
					roomModel = roomModelOptional.get();
					Optional<Client> clientModelOptional = roomModel.getParticipants().stream()
							                                       .filter(client -> client.getClientID().equals(payload.clientID()))
							                                       .findFirst();
					if (clientModelOptional.isEmpty()) {
						response = XUtils.buildJsonErrorResponse(404, "client", "validation",
								"Client not found!", response);
						broadcast(clientObject, response.toString());
						return;
					}
					
					CompletableFuture<String> future = VideoRoomController.getResponseCompletableFuture(connectionsManager, payload, clientModelOptional, roomModel);
					
					response.put("sdp", future.get());
					response = XUtils.buildJsonSuccessResponse(200, "eventType", "SDP",
							"SDP Offer processed, here is the answer ", response);
					
					break;
				
				
			}
			broadcast(clientObject, response.toString());
			
			
		} catch (Exception ex) {
			logger.atSevere().withCause(ex).log("onMessage Error");
		}
	}
	
	private void broadcast(Client client, String message) {
		client.getSocketSession().getAsyncRemote().sendObject(message, sendResult -> {
			if (sendResult.getException() != null) {
				logger.atSevere().withCause(sendResult.getException()).log("Failed to send message");
			}
		});
		
		
	}
}
