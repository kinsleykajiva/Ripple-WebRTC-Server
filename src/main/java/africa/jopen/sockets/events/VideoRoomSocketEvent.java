package africa.jopen.sockets.events;

import africa.jopen.controllers.VideoRoomController;
import africa.jopen.http.videoroom.PostCreateRoom;
import africa.jopen.http.videoroom.PostJoinRoom;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.Events;
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
        final String requestType = messageObject.getString("requestType");
        response.put("history", messageObject);
        switch (requestType) {
            case "remember" -> response = rememberResponse(connectionsManager,clientObject);
            case "joinRoom" -> {
                if (!messageObject.has("password")) {
                    response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE,  Events.VALIDATION_ERROR_EVENT,
                            "password is required", response);
                    broadcast(clientObject, response.toString());
                    return;
                }
                if (!messageObject.has("clientID")) {
                    response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE,  Events.VALIDATION_ERROR_EVENT,
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
                    response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE,  Events.VALIDATION_ERROR_EVENT,
                            "Invalid room ID!", response);
                    broadcast(clientObject, response.toString());
                    return;
                }
                RoomModel roomModel = roomModelOptional.get();
                if (!roomModel.getPassword().equals(room.password())) {
                    response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE, "authentication",
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
                    response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE,  Events.VALIDATION_ERROR_EVENT,
                            "Pin is required", response);
                    broadcast(clientObject, response.toString());
                    return;
                }
                if (!messageObject.has("password")) {
                    response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE,  Events.VALIDATION_ERROR_EVENT,
                            "password is required", response);
                    broadcast(clientObject, response.toString());
                    return;
                }
                if (!messageObject.has("creatorClientID")) {
                    response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE,  Events.VALIDATION_ERROR_EVENT,
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
                response = XUtils.buildJsonSuccessResponse(200,  Events.EVENT_TYPE, "createRoom",
                        "Room created successfully", response);
            }
            default -> {
                response.put("clientID", clientObject.getClientID());
                response = XUtils.buildJsonErrorResponse(400,  Events.EVENT_TYPE,  Events.VALIDATION_ERROR_EVENT,
                        "Invalid request type for VIDEO_ROOM feature", response);
            }
        }
        broadcast(clientObject, response.toString());
    }

}
