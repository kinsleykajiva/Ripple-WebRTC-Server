package africa.jopen.controllers;

import africa.jopen.http.PostClient;
import africa.jopen.models.Client;
import africa.jopen.utils.ConnectionsManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/video")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VideoRoomController {
    @Inject
    ConnectionsManager connectionsManager;
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
