package africa.jopen.controllers;

import africa.jopen.events.ClientsEvents;
import africa.jopen.http.PostClient;
import africa.jopen.http.PostCreateRoom;
import africa.jopen.http.PostJoinRoom;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.FeatureTypes;
import com.google.common.flogger.FluentLogger;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Map;

@Path("/video")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VideoRoomController {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();



    @Inject
    Event<ClientsEvents> clientsEventsEvent;
    @Inject
    ConnectionsManager connectionsManager;

    @POST
    @Path("/join-room")
    public Response joineRoom(PostJoinRoom room) {
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
            // as per the logic for now the creatort is already loaded in momery of the server as an active client object
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
    @Path("/create-room")
    public Response createRoom(PostCreateRoom room) {
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
            ConnectionsManager. roomsList.add(roomModel);
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
    @Path("/connect")
    public Response connectClient(PostClient client) {
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
}
