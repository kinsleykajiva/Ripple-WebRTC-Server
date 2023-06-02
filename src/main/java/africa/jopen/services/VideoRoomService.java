package africa.jopen.services;

import africa.jopen.events.EventService;
import africa.jopen.http.PostClient;
import africa.jopen.http.videoroom.PostCreateRoom;
import africa.jopen.http.videoroom.PostIceCandidate;
import africa.jopen.http.videoroom.PostJoinRoom;
import africa.jopen.http.videoroom.PostSDPOffer;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class VideoRoomService implements Service {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    private final ConnectionsManager connectionsManager;
    private final EventService eventService;

    public VideoRoomService(ConnectionsManager connectionsManager, EventService eventService) {
        this.connectionsManager = connectionsManager;
        this.eventService = eventService;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/room-info", this::RoomParticipants)
                .post("/update-ice-candidate", this::updateIce)
                .post("/send-offer", this::sendOffer)
                .post("/join-room", this::joinRoom)
                .post("/create-room", this::createRoom)
        ;
    }

    private void RoomParticipants(ServerRequest request, ServerResponse response) {
        var roomID = request.queryParams().first("roomID");
        logger.atInfo().log(" roomID " + roomID);
        if (roomID.isEmpty()) {

            XUtils.buildErrorResponse(null, response, 400, "Room ID is required!");
            return;
        } else {
            var roomModelOptional = ConnectionsManager.ROOMS.select(roomM -> roomM.getRoomID().equals(roomID.get()));
            if (roomModelOptional.isEmpty()) {

                XUtils.buildErrorResponse(null, response, 400, "Room Not found!");
            } else {
                final var roomModel = roomModelOptional.getOnly();

                JsonObject data = JSON.createObjectBuilder()
                        .add("room", JSON.createObjectBuilder(Map.of(
                                "roomID", roomModel.getRoomID(),
                                "roomName", roomModel.getRoomName(),
                                "createdTimeStamp", roomModel.getCreatedTimeStamp(),
                                "password", roomModel.getPassword(),
                                "pin", roomModel.getPin(),
                                "maximumCapacity", roomModel.getMaximumCapacity(),
                                "roomDescription", roomModel.getRoomDescription(),
                                "creatorClientID", roomModel.getCreatorClientID(),
                                "participants", roomModel.getParticipantsDto()

                        )))
                        .build();

                XUtils.buildSuccessResponse(response, 200, "Room Information ", data);
            }
        }
        return;

    }

    private void updateIce(ServerRequest request, ServerResponse response) {

        request.content()
                .as(PostIceCandidate.class)
                .thenAccept(payload -> {
                    if (payload == null) {

                        XUtils.buildErrorResponse(null, response, 400, "Payload object is required!");
                    }

                    if (payload.clientID() == null || payload.clientID().isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Client ID is required!");
                    }


                    if (payload.iceCandidate().candidate() == null) {

                        XUtils.buildErrorResponse(null, response, 400, "icecandidate is required!");
                    }
                    Optional<RoomModel> roomModelOptional = connectionsManager.getRoomById(payload.roomID());
                    if (roomModelOptional.isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Room not found!");
                    }

                    RoomModel roomModel = roomModelOptional.get();
                    Optional<Client> clientModelOptional = roomModel.getParticipants().stream()
                            .filter(client -> client.getClientID().equals(payload.clientID()))
                            .findFirst();
                    if (clientModelOptional.isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Client not found!");
                    }
                    Client clientModel = clientModelOptional.get();
                    clientModel.addIceCandidate(payload.iceCandidate());
                    clientModel.setFeatureType(FeatureTypes.VIDEO_ROOM);// Todo Review if we need to always set this at this point
                    connectionsManager.updateRoom(roomModel, payload.clientID());

                    JsonObject data = JSON.createObjectBuilder()
                            .build();
                    XUtils.buildSuccessResponse(response, 200, "Updated Clients Ice Candidates ", data);


                }).exceptionally(ex -> XUtils.buildErrorResponse(ex, response, 0, "Failed to process request"));

    }

    private void sendOffer(ServerRequest request, ServerResponse response) {
        request.content()
                .as(PostSDPOffer.class)
                .thenAccept(payload -> {
                    if (payload == null) {

                        XUtils.buildErrorResponse(null, response, 400, "Payload object is required!");
                    }

                    if (Objects.requireNonNull(payload).clientID() == null || payload.clientID().isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Client ID is required!");
                    }

                    if (payload.offer() == null || payload.offer().isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Offer is required!");
                    }

                    Optional<RoomModel> roomModelOptional = connectionsManager.getRoomById(payload.roomID());
                    if (roomModelOptional.isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Room not found!");
                    }

                    RoomModel roomModel = roomModelOptional.get();
                    Optional<Client> clientModelOptional = roomModel.getParticipants().stream()
                            .filter(client -> client.getClientID().equals(payload.clientID()))
                            .findFirst();
                    if (clientModelOptional.isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Client not found!");
                    }
                    assert clientModelOptional.isPresent();
                    Client clientModel = clientModelOptional.get();
                    clientModel.setFeatureType(FeatureTypes.VIDEO_ROOM);
                    clientModel.getRtcModel().setOffer(payload.offer());

                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                        String responseAnswer = clientModel.processSdpOfferAsRemoteDescription();
                        connectionsManager.updateRoom(roomModel, payload.clientID());

                        return responseAnswer;
                    });
                    try {
                        JsonObject data = JSON.createObjectBuilder()
                                .add("sdp", future.get())
                                .build();
                        XUtils.buildSuccessResponse(response, 200, "SDP Offer processed, here is the answer ", data);
                    } catch (Exception e) {

                        XUtils.buildErrorResponse(null, response, 400, "Error processing SDP Offer");
                    }

                }).exceptionally(ex -> XUtils.buildErrorResponse(ex, response, 0, "Failed to process request"));
    }

    private void joinRoom(ServerRequest request, ServerResponse response) {
        request.content()
                .as(PostJoinRoom.class)
                .thenAccept(room -> {
                    if (room == null) {

                        XUtils.buildErrorResponse(null, response, 400, "Room object is required!");
                    }

                    if (room.password() == null || room.password().isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Room password is required!");
                    }

                    if (room.clientID() == null || room.clientID().isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Client ID is required!");
                    }

                    Optional<RoomModel> roomModelOptional = connectionsManager.getRoomById(room.roomID());
                    if (roomModelOptional.isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Invalid room ID!");
                    }

                    RoomModel roomModel = roomModelOptional.get();
                    // test auth of the joing the room ,
                    if (!roomModel.getPassword().equals(room.password())) {
                        // ToDo:The issue here is that this will need to review if the creator still to test auth or not
                        // ToDo: as per the logic for now the creatort is already loaded in momery of the server as an active client object

                        XUtils.buildErrorResponse(null, response, 400, "Room authentication failed! Access rejected.");
                    }

                    joinRoom(connectionsManager, room, roomModel);

                    JsonObject data = JSON.createObjectBuilder().build();

                    XUtils.buildSuccessResponse(response, 200, "Added to room", data);

                }).exceptionally(ex -> XUtils.buildErrorResponse(ex, response, 0, "Failed to process request"));

    }
    public static void joinRoom(ConnectionsManager connectionsManager, PostJoinRoom room, RoomModel roomModel) {
        Client clientObject = connectionsManager.updateClientWhenRemembered(room.clientID());
        clientObject.setFeatureType(FeatureTypes.VIDEO_ROOM);
        RoomModel updatedRoomModel = roomModel.addParticipant(clientObject);
        connectionsManager.updateRoom(updatedRoomModel, room.clientID());
    }

    public static Map<String, Object> createRoom(ConnectionsManager connectionsManager, PostCreateRoom room, Client client) {
        RoomModel roomModel = new RoomModel();
        roomModel.setFeatureTypes(FeatureTypes.VIDEO_ROOM);
        roomModel.setRoomName(room.roomName());
        roomModel.setRoomDescription(room.roomDescription());
        roomModel.setPassword(room.password());
        roomModel.setPin(room.pin());
        roomModel.setCreatorClientID(client.getClientID());
        client.setFeatureType(FeatureTypes.VIDEO_ROOM);
        roomModel.addParticipant(client);
        connectionsManager.addRoom(roomModel);
        //Todo: add handling events to notify the room of the the clients changes attributes as well .
        // ToDo : What wil happen if all the user have been removed by the Orphaning or deliberate exiting from the room of been removed  the admin


        Map<String, Object> responseData = new HashMap<>();
        responseData.put("roomID", roomModel.getRoomID());
        responseData.put("roomName", roomModel.getRoomName());
        responseData.put("createdTimeStamp", roomModel.getCreatedTimeStamp());
        responseData.put("password", roomModel.getPassword());
        responseData.put("pin", roomModel.getPin());
        responseData.put("maximumCapacity", roomModel.getMaximumCapacity());
        responseData.put("roomDescription", roomModel.getRoomDescription());
        responseData.put("creatorClientID", roomModel.getCreatorClientID());
        return responseData;
    }

    private void createRoom(ServerRequest request, ServerResponse response) {
        request.content()
                .as(PostCreateRoom.class)
                .thenAccept(room -> {
                    if (room == null) {

                        XUtils.buildErrorResponse(null, response, 400, "Room object is required!");
                    }

                    if (room.pin() == null || room.pin().isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Room pin is required!");
                    }

                    if (room.creatorClientID() == null || room.creatorClientID().isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Creator Client ID is required!");
                    }

                    if (room.password() == null || room.password().isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Room password is required!");
                    }

                    Optional<Client> clientOptional = connectionsManager.getClient(room.creatorClientID());
                    if (clientOptional.isEmpty()) {

                        XUtils.buildErrorResponse(null, response, 400, "Invalid client!");
                    }

                    try {
                        RoomModel roomModel = new RoomModel();
                        roomModel.setFeatureTypes(FeatureTypes.VIDEO_ROOM);
                        roomModel.setRoomName(room.roomName());
                        roomModel.setRoomDescription(room.roomDescription());
                        roomModel.setPassword(room.password());
                        roomModel.setPin(room.pin());
                        roomModel.setCreatorClientID(clientOptional.get().getClientID());
                        clientOptional.get().setFeatureType(FeatureTypes.VIDEO_ROOM);
                        roomModel.addParticipant(clientOptional.get());
                        connectionsManager.addRoom(roomModel);
                        //Todo: add handling events to notify the room of the the clients changes attributes as well .
                        // ToDo : What wil happen if all the user have been removed by the Orphaning or deliberate exiting from the room of been removed  the admin


                        Map<String, Object> responseData = createRoom(connectionsManager, room, clientOptional.get());

                        JsonObject data = JSON.createObjectBuilder()
                                .add("room", JSON.createObjectBuilder(responseData))
                                .build();

                        XUtils.buildSuccessResponse(response, 200, "Room created successfully", data);

                    } catch (Exception e) {
                        XUtils.buildErrorResponse(null, response, 400, "Failed to create room!");
                    }
                }).exceptionally(ex -> XUtils.buildErrorResponse(ex, response, 0, "Failed to process request"));

    }


}
