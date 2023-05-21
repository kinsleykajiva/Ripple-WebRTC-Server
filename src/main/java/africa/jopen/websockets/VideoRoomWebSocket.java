package africa.jopen.websockets;

import africa.jopen.models.Client;
import africa.jopen.utils.ConnectionsManager;
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
import org.json.JSONObject;

import java.math.BigInteger;

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
				response.put("eventType", "message");
				response.put("message", "Failed to understand the purpose of the request");
				response.put("code", 400);
				broadcast(clientObject, response.toString());
				return;
			}
			switch (messageObject.getString("requestType")){
				case "remember":
					break;
					case "joinRoom":
					break;
					case "createRoom":
					break;
					case "sendOffer":
					break;
					
					
					
			}
			
			
			
			
			
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
