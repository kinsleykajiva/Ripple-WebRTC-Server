package africa.jopen.sockets;

import africa.jopen.controllers.VideoRoomController;
import africa.jopen.http.IceCandidate;
import africa.jopen.http.videoroom.*;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

@ServerEndpoint("/client-access/{clientID}/{featureType}")
@ApplicationScoped
public class ClientWebSocket {
    private ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    @OnOpen
    public void onOpen(Session session, @PathParam("clientID") String clientID, @PathParam("featureType") String featureType) {
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
                }
                if (Objects.equals(featureType, "VIDEO_CALL")) {
                    clientObject.setFeatureType(FeatureTypes.VIDEO_CALL);
                }
                if (Objects.equals(featureType, "AUDIO_ROOM")) {
                    clientObject.setFeatureType(FeatureTypes.AUDIO_ROOM);
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
        System.out.println("onClose> " + clientID);
    }

    @OnError
    public void onError(Session session, @PathParam("clientID") String clientID, Throwable throwable) {
        System.out.println("onError> " + clientID + ": " + throwable);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("clientID") String clientID) {
        logger.atInfo().log("onMessage " + clientID + ": " + message);

        if (!connectionsManager.checkIfClientExists(clientID)) {
            logger.atInfo().log("Invalid client ID. Unable to notify the client session.");
            return;
        }

        Client clientObject = connectionsManager.getClient(clientID)
                .orElseThrow(() -> new IllegalStateException("Client object not found"));

        JSONObject response = new JSONObject();
        try {
            logger.atInfo().log("..." + clientID);
            JSONObject messageObject = new JSONObject(message);

            if (!messageObject.has("requestType")) {
                response.put("clientID", clientObject.getClientID());
                response = XUtils.buildJsonErrorResponse(400, "eventType", "validation",
                        "Failed to understand the purpose of the request", response);
                broadcast(clientObject, response.toString());
                return;
            }

            FeatureTypes featureType = clientObject.getFeatureType();
            switch (featureType) {
                case G_STREAM -> handleGStreamRequest(clientObject, messageObject, response);
                case VIDEO_ROOM -> handleVideoRoomRequest(clientObject, messageObject, response);
                default -> {
                    response.put("clientID", clientObject.getClientID());
                    response = XUtils.buildJsonErrorResponse(400, "featureType", "validation",
                            "Invalid feature type", response);
                    broadcast(clientObject, response.toString());
                }
            }

            broadcast(clientObject, response.toString());
        } catch (Exception ex) {
            logger.atSevere().withCause(ex).log("onMessage Error");
        }
    }

    private JSONObject rememberResponse(Client clientObject) {
        JSONObject response = new JSONObject();
        clientObject = connectionsManager.updateClientWhenRemembered(clientObject.getClientID());
        response.put("clientID", clientObject.getClientID());
        response.put("lastSeen", clientObject.lastTimeStamp());
        response.put("featureInUse", clientObject.getFeatureType().toString());
        response = XUtils.buildJsonSuccessResponse(200, "eventType", "remember",
                "Client Remembered Successfully", response);

        return response;
    }


    private void handleGStreamRequest(Client clientObject, JSONObject messageObject, JSONObject response) {
        final String requestType = messageObject.getString("requestType");
        switch (requestType) {
            case "remember" -> response = rememberResponse(clientObject);
            case "update-ice-candidate" -> {
                if (!messageObject.has("clientID")) {
                    response = XUtils.buildJsonErrorResponse(400, "clientID", "validation",
                            "clientID is required", response);
                    broadcast(clientObject, response.toString());
                    return;
                }
                final var payload = new PostIceCandidate(
                        null, new IceCandidate(
                        messageObject.getString("candidate"),
                        messageObject.getString("sdpMid"),
                        messageObject.getInt("sdpMidLineIndex")
                ), messageObject.getString("clientID"));
                clientObject.getWebRTCSendRecv()
                        .handleIceSdp(payload.iceCandidate().candidate(), payload.iceCandidate().sdpMidLineIndex());
            }
            case "send-answer" -> {
                final var payload = new PostSDPAnswer(null, messageObject.getString("answer"),
                        messageObject.getString("clientID"));
                clientObject.getWebRTCSendRecv().handleSdp(
                        payload.answer()
                );
                connectionsManager.updateClient(clientObject);
                response = XUtils.buildJsonSuccessResponse(200, "eventType", "validation",
                        "Client answered Successfully", response);
            }
            case "start" -> {
                if (!messageObject.has("clientID")) {
                    response = XUtils.buildJsonErrorResponse(400, "clientID", "validation",
                            "clientID is required", response);
                    broadcast(clientObject, response.toString());
                    return;
                }
                clientObject.setWebRTCSendRecv();
                connectionsManager.updateClient(clientObject);
                response = XUtils.buildJsonSuccessResponse(200, "eventType", "validation",
                        "Streaming Started Successfully, the app should start to receive some streams", response);
            }
            default -> {
                response.put("clientID", clientObject.getClientID());
                response = XUtils.buildJsonErrorResponse(400, "requestType", "validation",
                        "Invalid request type for G_STREAM feature", response);
                broadcast(clientObject, response.toString());
            }
        }
        broadcast(clientObject, response.toString());
    }

    private void handleVideoRoomRequest(Client clientObject, JSONObject messageObject, JSONObject response) {
        String requestType = messageObject.getString("requestType");
        switch (requestType) {
            case "remember" -> response = rememberResponse(clientObject);
            case "joinRoom" -> {
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
            }
            case "createRoom" -> {
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
                        messageObject.getString("pin"),
                        messageObject.getString("password"),
                        messageObject.getString("creatorClientID")
                );
                Map<String, Object> resultRoom = VideoRoomController.createRoom(connectionsManager, post, clientObject);
                response.put("room", resultRoom);
                response = XUtils.buildJsonSuccessResponse(200, "eventType", "createRoom",
                        "Room created successfully", response);
            }
            default -> {
                response.put("clientID", clientObject.getClientID());
                response = XUtils.buildJsonErrorResponse(400, "requestType", "validation",
                        "Invalid request type for VIDEO_ROOM feature", response);
            }
        }
        broadcast(clientObject, response.toString());
    }

    private void broadcast(Client client, String message) {
        client.getSocketSession().getAsyncRemote().sendObject(message, sendResult -> {
            if (sendResult.getException() != null) {
                logger.atSevere().withCause(sendResult.getException()).log("Failed to send message");
            }
        });


    }
}