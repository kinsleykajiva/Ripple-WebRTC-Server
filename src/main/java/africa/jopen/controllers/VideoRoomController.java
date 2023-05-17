package africa.jopen.controllers;

import africa.jopen.events.ClientsEvents;
import africa.jopen.http.PostClient;
import africa.jopen.http.PostCreateRoom;
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
            roomModel.setCreatorClientID( clientOptional.get().clientId());


            roomModel.addParticipant(clientOptional.get());

            return Response.ok(Map.of(
                            "success", true,
                            "code", 200,
                            "message", "Room created successfully",
                            "data", Map.of(
                                    "room", Map.of(
                                            "roomName",roomModel.getRoomName(),
                                            "createdTimeStamp",roomModel.getCreatedTimeStamp(),
                                            "password",roomModel.getPassword(),
                                            "pin",roomModel.getPin(),
                                            "maximumCapacity",roomModel.getMaximumCapacity(),
                                            "roomDescription",roomModel.getRoomDescription(),
                                            "creatorClientID",roomModel.getCreatorClientID()

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
