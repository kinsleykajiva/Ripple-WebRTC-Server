package africa.jopen.controllers;

import africa.jopen.events.ClientsEvents;
import africa.jopen.http.PostClient;
import africa.jopen.http.PostCreateRoom;
import africa.jopen.http.PostJoinRoom;
import africa.jopen.http.PostSDPOffer;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/video")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VideoRoomController {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();


    @Inject
    Event<ClientsEvents> clientsEventsEvent;
    @Inject
    ConnectionsManager connectionsManager;

    @GET
    @Path("/room-info")
    public Response RoomParticipants(@QueryParam("roomID") String roomID) {

        if (roomID == null || roomID.isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 404,
                            "message", " roomID is empty"
                    )
            ).build();
        }


        var roomModelOptional = ConnectionsManager.roomsList.select(roomM -> roomM.getRoomID().equals(roomID));
        if (roomModelOptional.isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 404,
                            "message", " room not found!.."
                    )
            ).build();
        }
        final var roomModel = roomModelOptional.getOnly();

        return Response.ok(Map.of(
                        "success", true,
                        "code", 200,
                        "message", " room Information",
                        "data", Map.of(
                                "room", Map.of(
                                        "roomID", roomModel.getRoomID(),
                                        "roomName", roomModel.getRoomName(),
                                        "createdTimeStamp", roomModel.getCreatedTimeStamp(),
                                        "password", roomModel.getPassword(),
                                        "pin", roomModel.getPin(),
                                        "maximumCapacity", roomModel.getMaximumCapacity(),
                                        "roomDescription", roomModel.getRoomDescription(),
                                        "creatorClientID", roomModel.getCreatorClientID(),
                                        "participants", roomModel.getParticipantsDto()

                                )
                        )
                )
        ).build();

    }

    @POST
    @Path("/sendOffer")
    public Response sendOfferRoom(PostSDPOffer payload) {
        if (payload == null) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "payload Object is required!"
                    )
            ).build();
        }
        if (payload.clientID() == null || payload.clientID().isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "clientID is required!"
                    )
            ).build();
        }

        if (payload.offer() == null || payload.offer().isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "offer is required!"
                    )
            ).build();
        }
        var roomModelOptional = ConnectionsManager.roomsList.select(roomM -> roomM.getRoomID().equals(payload.roomID()));

        if (roomModelOptional.isEmpty()) {

        }
        roomModelOptional.getOnly().getParticipants().stream().filter(client -> client.clientId().equals(payload.clientID())).forEach(client -> {
            client.getRtcModel().setOffer(new JSONObject(payload.offer()));
        });
        // update the  ConnectionsManager.roomsList

        return Response.ok(Map.of(
                        "success", true,
                        "code", 400,
                        "message", "SDP Offer Shared"
                )
        ).build();
    }


    @POST
    @Path("/send-offer")
    public Response sendOffer(PostSDPOffer payload) {
        if (payload == null) {
            return XUtils.buildErrorResponse(false, 400, "Payload object is required!", Map.of());
        }

        if (payload.clientID() == null || payload.clientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Client ID is required!", Map.of());
        }

        if (payload.offer() == null || payload.offer().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Offer is required!", Map.of());
        }

        Optional<RoomModel> roomModelOptional = connectionsManager.getRoomById(payload.roomID());
        if (roomModelOptional.isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Room not found!", Map.of());
        }

        RoomModel roomModel = roomModelOptional.get();
        roomModel.getParticipants().stream()
                .filter(client -> client.clientId().equals(payload.clientID()))
                .findFirst()
                .ifPresent(client -> {
                    client.getRtcModel().setOffer(new JSONObject(payload.offer()));
                    connectionsManager.updateRoom(roomModel);
                });

        return XUtils.buildSuccessResponse(true, 200, "SDP Offer shared", Map.of());
    }


    @POST
    @Path("/join-roomxxxxxx")
    public Response joinRoomxxxxxxx(PostJoinRoom room) {
        if (room == null) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "room Object is required!"
                    )
            ).build();
        }
        if (room.password() == null || room.password().isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "room password is required!"
                    )
            ).build();
        }
        if (room.clientID() == null || room.clientID().isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", " clientID is required!"
                    )
            ).build();
        }
        var exists =
                ConnectionsManager.roomsList
                        .anySatisfy(roomModel -> roomModel.getRoomID().equals(room.roomID()));
        if (!exists) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", " roomID invalid!"
                    )
            ).build();
        }

        var roomModelOptional = ConnectionsManager.roomsList.select(roomM -> roomM.getRoomID().equals(room.roomID()));
        if (roomModelOptional.isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", " room no found!"
                    )
            ).build();
        }

        // test auth of the joing the room ,
        if (!roomModelOptional.getOnly().getPassword().equals(room.password())) {
            // ToDo:The issue here is that this will need to review if the creator still to test auth or not
            // ToDo: as per the logic for now the creatort is already loaded in momery of the server as an active client object
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", " room Auth failed ,Access Rejected!"
                    )
            ).build();
        }

        var clientObject = connectionsManager.updateClientWhenRemembered(room.clientID());
        var referedRoomModel = roomModelOptional.getOnly().addParticipant(clientObject);

        int index = ConnectionsManager.roomsList.detectIndex(roomM -> roomM.getRoomID().equals(room.roomID()));

        if (index >= 0) {
            logger.atInfo().log("Room updated");
            ConnectionsManager.roomsList.set(index, referedRoomModel);
        }

        return Response.ok(Map.of(
                        "success", true,
                        "code", 200,
                        "message", "Added to room"
                )
        ).build();
    }


    @POST
    @Path("/join-room")
    public Response joinRoom(PostJoinRoom room) {
        if (room == null) {
            return XUtils.buildErrorResponse(false, 400, "Room object is required!", Map.of());
        }

        if (room.password() == null || room.password().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Room password is required!", Map.of());
        }

        if (room.clientID() == null || room.clientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Client ID is required!", Map.of());
        }

        Optional<RoomModel> roomModelOptional = connectionsManager.getRoomById(room.roomID());
        if (roomModelOptional.isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Invalid room ID!", Map.of());
        }

        RoomModel roomModel = roomModelOptional.get();

        if (!roomModel.getPassword().equals(room.password())) {
            // ToDo:The issue here is that this will need to review if the creator still to test auth or not
            // ToDo: as per the logic for now the creatort is already loaded in momery of the server as an active client object
            return XUtils.buildErrorResponse(false, 400, "Room authentication failed! Access rejected.", Map.of());
        }

        Client clientObject = connectionsManager.updateClientWhenRemembered(room.clientID());
        RoomModel updatedRoomModel = roomModel.addParticipant(clientObject);
        connectionsManager.updateRoom(updatedRoomModel);

        return XUtils.buildSuccessResponse(true, 200, "Added to room", Map.of());
    }

    @POST
    @Path("/create-roomxxxxx")
    public Response createRoomxxxxx(PostCreateRoom room) {
        if (room == null) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "room Object is required!"
                    )
            ).build();
        }
        if (room.pin() == null || room.pin().isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "room  pin is required!"
                    )
            ).build();
        }
        if (room.creatorClientID() == null || room.creatorClientID().isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", " creatorClientID is required!"
                    )
            ).build();
        }
        if (room.password() == null || room.password().isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "room password is required!"
                    )
            ).build();
        }
        var clientOptional = connectionsManager.getClient(room.creatorClientID());
        if (!clientOptional.isPresent()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "Client Invalid!"
                    )
            ).build();
        }


        RoomModel roomModel;
        try {
            roomModel = new RoomModel();
            roomModel.setFeatureTypes(FeatureTypes.VIDEO_ROOM);
            roomModel.setRoomName(room.roomName());
            roomModel.setRoomDescription(room.roomDescription());
            roomModel.setPassword(room.password());
            roomModel.setPin(room.pin());
            roomModel.setCreatorClientID(clientOptional.get().clientId());

            roomModel.addParticipant(clientOptional.get());
            ConnectionsManager.roomsList.add(roomModel);
            //Todo: add handling events to notify the room of the the clients changes attributes as well .
            // ToDo : What wil happen if all the user have been removed by the Orphaning or deliberate exiting from the room of been removed  the admin
            return Response.ok(Map.of(
                            "success", true,
                            "code", 200,
                            "message", "Room created successfully",
                            "data", Map.of(
                                    "room", Map.of(
                                            "roomID", roomModel.getRoomID(),
                                            "roomName", roomModel.getRoomName(),
                                            "createdTimeStamp", roomModel.getCreatedTimeStamp(),
                                            "password", roomModel.getPassword(),
                                            "pin", roomModel.getPin(),
                                            "maximumCapacity", roomModel.getMaximumCapacity(),
                                            "roomDescription", roomModel.getRoomDescription(),
                                            "creatorClientID", roomModel.getCreatorClientID()

                                    )
                            )
                    )
            ).build();

        } catch (Exception e) {
            logger.atWarning().withCause(e).log("Error while creating room: " + room);
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "failed to create room!"
                    )
            ).build();
        }


    }


    @POST
    @Path("/create-room")
    public Response createRoom(PostCreateRoom room) {
        if (room == null) {
            return XUtils.buildErrorResponse(false, 400, "Room object is required!", Map.of());
        }

        if (room.pin() == null || room.pin().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Room pin is required!", Map.of());
        }

        if (room.creatorClientID() == null || room.creatorClientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Creator Client ID is required!", Map.of());
        }

        if (room.password() == null || room.password().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Room password is required!", Map.of());
        }

        Optional<Client> clientOptional = connectionsManager.getClient(room.creatorClientID());
        if (clientOptional.isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Invalid client!", Map.of());
        }

        try {
            RoomModel roomModel = new RoomModel();
            roomModel.setFeatureTypes(FeatureTypes.VIDEO_ROOM);
            roomModel.setRoomName(room.roomName());
            roomModel.setRoomDescription(room.roomDescription());
            roomModel.setPassword(room.password());
            roomModel.setPin(room.pin());
            roomModel.setCreatorClientID(clientOptional.get().clientId());

            roomModel.addParticipant(clientOptional.get());
            connectionsManager.addRoom(roomModel);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("roomID", roomModel.getRoomID());
            responseData.put("roomName", roomModel.getRoomName());
            responseData.put("createdTimeStamp", roomModel.getCreatedTimeStamp());
            responseData.put("password", roomModel.getPassword());
            responseData.put("pin", roomModel.getPin());
            responseData.put("maximumCapacity", roomModel.getMaximumCapacity());
            responseData.put("roomDescription", roomModel.getRoomDescription());
            responseData.put("creatorClientID", roomModel.getCreatorClientID());

            return XUtils.buildSuccessResponse(true, 200, "Room created successfully", responseData);
        } catch (Exception e) {
            logger.atWarning().withCause(e).log("Error while creating room: " + room);
            return XUtils.buildErrorResponse(false, 400, "Failed to create room!", Map.of());
        }
    }


    @POST
    @Path("/connectxxx")
    public Response connectClientxxx(PostClient client) {
        if (client == null) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "client Object is required!"
                    )
            ).build();
        }
        if (client.clientAgentName() == null || client.clientAgentName().isEmpty()) {
            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "client clientAgentName is required!"
                    )
            ).build();
        }

        Client clientObject = new Client(client.clientAgentName());
        connectionsManager.addNewClient(clientObject);


        return Response.ok(Map.of(
                        "success", true,
                        "code", 200,
                        "message", "Client Newly Connected and Recognised",
                        "data", Map.of(
                                "client", Map.of(
                                        "clientID", clientObject.clientId(),
                                        "lastSeen", clientObject.lastTimeStamp()
                                )
                        )
                )
        ).build();

    }


    @POST
    @Path("/connect")
    public Response connectClient(PostClient client) {
        if (client == null) {
            return XUtils.buildErrorResponse(false, 400, "Client object is required!", Map.of());
        }

        if (client.clientAgentName() == null || client.clientAgentName().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Client agent name is required!", Map.of());
        }

        Client clientObject = new Client(client.clientAgentName());
        connectionsManager.addNewClient(clientObject);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("clientID", clientObject.clientId());
        responseData.put("lastSeen", clientObject.lastTimeStamp());

        return XUtils.buildSuccessResponse(true, 200, "Client newly connected and recognized", responseData);
    }

}
