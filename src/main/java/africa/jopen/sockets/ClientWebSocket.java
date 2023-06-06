package africa.jopen.sockets;

import africa.jopen.controllers.VideoRoomController;
import africa.jopen.exceptions.ClientException;
import africa.jopen.http.IceCandidate;
import africa.jopen.http.videoroom.PostCreateRoom;
import africa.jopen.http.videoroom.PostIceCandidate;
import africa.jopen.http.videoroom.PostJoinRoom;
import africa.jopen.http.videoroom.PostSDPAnswer;
import africa.jopen.models.Client;
import africa.jopen.models.GStreamMediaResource;
import africa.jopen.models.RoomModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.Events;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static africa.jopen.sockets.events.GStreamsSocketEvent.handleGStreamRequest;
import static africa.jopen.sockets.events.VideoCallSocketEvent.handleVideoCallRequest;
import static africa.jopen.sockets.events.VideoRoomSocketEvent.handleVideoRoomRequest;

@ServerEndpoint("/client-access/{clientID}/{featureType}")
@ApplicationScoped
public class ClientWebSocket {
    private final ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    @OnOpen
    public void onOpen(Session session, @PathParam("clientID") String clientID, @PathParam("featureType") String featureType) {

         logger.atInfo().log("OnOpen > " + clientID);
        // since this is the first . The client id will need to be set after the fact from the server , so the client id wil be the agent name
        try {
            JSONObject response = new JSONObject();
            Client clientObject;
            var testExists = connectionsManager.checkIfClientExists(clientID);
            if (testExists) {// this is a sub-sequent reconnection.
                clientObject = connectionsManager.updateClientWhenRemembered(clientID);
                clientObject.setSocketSession(session);
                if (Objects.equals(featureType, "G_STREAM")) {
                    clientObject.setFeatureType(FeatureTypes.G_STREAM);
                }
                if (Objects.equals(featureType, "VIDEO_ROOM")) {
                    clientObject.setFeatureType(FeatureTypes.VIDEO_ROOM);
                }
                if (Objects.equals(featureType, "VIDEO_CALL")) {
                    clientObject.setFeatureType(FeatureTypes.VIDEO_CALL);
                }
                if (Objects.equals(featureType, "AUDIO_ROOM")) {
                    clientObject.setFeatureType(FeatureTypes.AUDIO_ROOM);
                }

                connectionsManager.updateClient(clientObject);
            } else {
                clientObject = new Client(clientID);
                if (Objects.equals(featureType, "G_STREAM")) {
                    clientObject.setFeatureType(FeatureTypes.G_STREAM);
                }
                if (Objects.equals(featureType, "VIDEO_ROOM")) {
                    clientObject.setFeatureType(FeatureTypes.VIDEO_ROOM);
                    clientObject.createPeerConnection();
                }
                if (Objects.equals(featureType, "VIDEO_CALL")) {
                    clientObject.setFeatureType(FeatureTypes.VIDEO_CALL);
                    clientObject.createPeerConnection();
                }
                if (Objects.equals(featureType, "AUDIO_ROOM")) {
                    clientObject.setFeatureType(FeatureTypes.AUDIO_ROOM);
                    clientObject.createPeerConnection();
                }
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
        logger.atInfo().log("onClose > " + clientID);
        Client clientObject = connectionsManager.getClient(clientID).orElseThrow(() -> new ClientException("Client object not found"));
        connectionsManager.removeClient(clientObject);
    }

    @OnError
    public void onError(Session session, @PathParam("clientID") String clientID, Throwable throwable) {
        logger.atSevere().withCause(throwable).log("onError > " + clientID + ": " + throwable);
        Client clientObject = connectionsManager.getClient(clientID).orElseThrow(() -> new ClientException("Client object not found"));
        connectionsManager.removeClient(clientObject);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("clientID") String clientID) {
        logger.atInfo().log("onMessage " + clientID + ": " + message);

        if (!connectionsManager.checkIfClientExists(clientID)) {
            logger.atInfo().log("Invalid client ID. Unable to notify the client session.");
            return;
        }

        Client clientObject = connectionsManager.getClient(clientID)
                .orElseThrow(() -> new ClientException("Client object not found"));

        JSONObject response = new JSONObject();
        try {

            JSONObject messageObject = new JSONObject(message);

            if (!messageObject.has("requestType")) {
                response.put("clientID", clientObject.getClientID());
                response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT,"Failed to understand the purpose of the request", response);
                broadcast(clientObject, response.toString());
                return;
            }

            FeatureTypes featureType = clientObject.getFeatureType();
            switch (featureType) {
                case G_STREAM -> handleGStreamRequest(clientObject, messageObject, response);
                case VIDEO_ROOM -> handleVideoRoomRequest( clientObject, messageObject, response);
                case VIDEO_CALL -> handleVideoCallRequest( clientObject, messageObject, response);
                default -> {
                    response.put("clientID", clientObject.getClientID());
                    response = XUtils.buildJsonErrorResponse(500,  Events.EVENT_TYPE,  Events.VALIDATION_ERROR_EVENT,"Invalid feature type", response);
                    broadcast(clientObject, response.toString());
                }
            }
            clientObject = connectionsManager.getClient(clientID).orElseThrow(() -> new ClientException("Client object not found"));

            broadcast(clientObject, response.toString());
        } catch (Exception ex) {
            logger.atSevere().withCause(ex).log("onMessage Error");
        }
    }

    public static JSONObject rememberResponse(ConnectionsManager connectionsManager ,Client clientObject) {

        JSONObject response = new JSONObject();
        clientObject = connectionsManager.updateClientWhenRemembered(clientObject.getClientID());
        response.put("clientID", clientObject.getClientID());
        response.put("lastSeen", clientObject.lastTimeStamp());
        response.put("featureInUse", clientObject.getFeatureType().toString());
        response = XUtils.buildJsonSuccessResponse(200,  Events.EVENT_TYPE, Events.REMEMBER_EVENT,"Client Remembered Successfully", response);

        return response;
    }


    public static void broadcast(Client client, String message) {
        if(Objects.isNull(client)){
            return;
        }
        client.getSocketSession().getAsyncRemote().sendObject(message, sendResult -> {
            if (sendResult.getException() != null) {
                logger.atSevere().withCause(sendResult.getException()).log("Failed to send message");
            }
        });


    }
}
