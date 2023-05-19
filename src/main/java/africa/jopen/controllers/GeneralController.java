package africa.jopen.controllers;

import africa.jopen.events.ClientsEvents;
import africa.jopen.http.PostClient;
import africa.jopen.http.PostClientRemember;
import africa.jopen.utils.ConnectionsManager;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/app")
public class GeneralController {

    @Inject
    ConnectionsManager connectionsManager;
    @Inject
    Event<ClientsEvents> clientsEventsEvent;

    @GET
    @Path("/client/all")
    public Response getAllClient() {
        return Response.ok(Map.of(
                        "success", true,
                        "code", 200,
                        "message", "Clients Available ",
                        "data", Map.of(
                                "clients", connectionsManager.list()
                        )
                )
        ).build();
    }


    @POST
    @Path("/client/remember")
    public Response rememberMe(PostClientRemember client) {

        var testExists = connectionsManager.isClientRemembered(client.clientID());
        if (!testExists) {

            return Response.ok(Map.of(
                            "success", false,
                            "code", 400,
                            "message", "Client Not Found Please reconnect to the server feature"
                    )
            ).build();
        }

        var clientObject = connectionsManager.updateClientWhenRemembered(client.clientID());

        ClientsEvents mClientsEvent = new ClientsEvents(clientObject);
        clientsEventsEvent.fire(mClientsEvent);

        return Response.ok(Map.of(
                        "success", true,
                        "code", 200,
                        "message", "Client  Remembered Successfully",
                        "data", Map.of(
                                "client", Map.of(
                                        "iceCandidates", clientObject.getCandidateMap(),
                                        "clientID", clientObject.clientId(),
                                        "lastSeen", clientObject.lastTimeStamp()
                                )
                        )
                )
        ).build();


    }
}
