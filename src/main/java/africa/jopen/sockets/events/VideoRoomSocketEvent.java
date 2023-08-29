package africa.jopen.sockets.events;

import africa.jopen.controllers.VideoRoomController;
import africa.jopen.http.videoroom.PostCreateRoom;
import africa.jopen.http.videoroom.PostJoinRoom;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.Events;
import africa.jopen.utils.Requests;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import org.json.JSONObject;

import java.util.Map;
import java.util.Optional;

import static africa.jopen.sockets.ClientWebSocket.broadcast;
import static africa.jopen.sockets.ClientWebSocket.rememberResponse;

public class VideoRoomSocketEvent {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
    
    public static void handleVideoRoomRequest(Client clientObject, JSONObject messageObject, JSONObject response) {
        final String requestType = messageObject.getString(Requests.REQUEST_TYPE);
        response.put("history", messageObject);
        
        switch (requestType) {
            case Requests.REMEMBER -> response = rememberResponse(connectionsManager, clientObject);
            case "joinRoom" -> response = handleJoinRoomRequest(messageObject, clientObject, response);
            case "createRoom" -> response = handleCreateRoomRequest(messageObject, clientObject, response);
            default -> response = handleInvalidRequest(response, clientObject);
        }
        
        broadcast(clientObject, response.toString());
    }
    
    
    private static JSONObject handleJoinRoomRequest(JSONObject messageObject, Client clientObject, JSONObject response) {
        if (!messageObject.has("password")) {
            handleValidationError("password is required", response, clientObject);
            return messageObject;
        }
        if (!messageObject.has("clientID")) {
            handleValidationError("clientID is required", response, clientObject);
            return messageObject;
        }
        
        var room = new PostJoinRoom(
                messageObject.getString("roomID"),
                messageObject.getString("password"),
                messageObject.getString("clientID")
        );
        
        Optional<RoomModel> roomModelOptional = connectionsManager.getRoomById(room.roomID());
        if (roomModelOptional.isEmpty()) {
            handleValidationError("Invalid room ID!", response, clientObject);
            return messageObject;
        }
        
        RoomModel roomModel = roomModelOptional.get();
        if (!roomModel.getPassword().equals(room.password())) {
            handleAuthenticationError("Room authentication failed! Access rejected.", response, clientObject);
            return messageObject;
        }
        
        VideoRoomController.joinRoom(connectionsManager, room, roomModel);
        response = XUtils.buildJsonSuccessResponse(Requests.OK_RESPONSE, "eventType", "joinRoom",
                "Added to room", response);
        return response;
    }
    
    
    private static JSONObject handleCreateRoomRequest(JSONObject messageObject, Client clientObject, JSONObject response) {
        if (!messageObject.has("pin")) {
            handleValidationError("Pin is required", response, clientObject);
            return new JSONObject();
        }
        if (!messageObject.has("password")) {
            handleValidationError("password is required", response, clientObject);
            return new JSONObject();
        }
        if (!messageObject.has("creatorClientID")) {
            handleValidationError("creatorClientID is required", response, clientObject);
            return new JSONObject();
        }
        
        var post = new PostCreateRoom(
                messageObject.getString("roomName"),
                messageObject.getString("roomDescription"),
                messageObject.getString("pin"),
                messageObject.getString("password"),
                messageObject.getString("creatorClientID")
        );
        
        Map<String, Object> resultRoom = VideoRoomController.createRoom(connectionsManager, post, clientObject);
        response.put("room", resultRoom);
        response = XUtils.buildJsonSuccessResponse(Requests.OK_RESPONSE, Events.EVENT_TYPE, "createRoom", "Room created successfully", response);
        return response;
    }
    
    private static JSONObject handleInvalidRequest(JSONObject response, Client clientObject) {
        response.put("clientID", clientObject.getClientID());
        response = XUtils.buildJsonErrorResponse(Requests.SERVER_BAD_REQUEST, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, "Invalid request type for VIDEO_ROOM feature", response);
        return response;
    }
    
    private static void handleValidationError(String message, JSONObject response, Client clientObject) {
        response = XUtils.buildJsonErrorResponse(Requests.SERVER_BAD_REQUEST, Events.EVENT_TYPE, Events.VALIDATION_ERROR_EVENT, message, response);
        broadcast(clientObject, response.toString());
        
    }
    
    private static void handleAuthenticationError(String message, JSONObject response, Client clientObject) {
        response = XUtils.buildJsonErrorResponse(Requests.SERVER_BAD_REQUEST, Events.EVENT_TYPE, "authentication", message, response);
        broadcast(clientObject, response.toString());
    }

}
