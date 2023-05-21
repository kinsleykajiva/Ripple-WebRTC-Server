package africa.jopen.services;

import africa.jopen.events.ClientsEvents;
import africa.jopen.events.EventService;

import africa.jopen.http.PostClient;
import africa.jopen.http.PostClientRemember;
import africa.jopen.models.Client;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import jakarta.json.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GeneralService implements Service {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
    
    private final ConnectionsManager connectionsManager;
    private final EventService eventService;
    
    public GeneralService(ConnectionsManager connectionsManager, EventService eventService) {
        this.connectionsManager = connectionsManager;
        this.eventService = eventService;
    }
    
    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/client/all", this::getAllClient)
                .post("/client/remember", this::rememberMe)
                .post("/connect", this::connectClient)
        ;
    }
    
    
    
    private void getAllClient(ServerRequest request, ServerResponse response){
       
        JsonObject data = JSON.createObjectBuilder()
                .add("client", Json.createArrayBuilder(connectionsManager.list()))
                .build();

        
        XUtils.buildSuccessResponse(response, 200, "Clients Available", data);
    }
    
    private void rememberMe(ServerRequest request, ServerResponse response){
        request.content()
                .as(PostClientRemember.class)
                .thenAccept(client->{
                    // process payload
                    if (client == null) {
                         XUtils.buildErrorResponse(null, response,400, "PostClientRemember object is required!");
                    }
                    
                    if (client.clientID() == null || client.clientID().isEmpty()) {
                         XUtils.buildErrorResponse(null, response,400, "clientID is required!");
                    }
                    
                    var testExists = connectionsManager.checkIfClientExists(client.clientID());
                    if (!testExists) {
                         XUtils.buildErrorResponse(null, response,400, "Client Not Found Please reconnect to the server feature!");
                        
                    }
                    
                    var clientObject = connectionsManager.updateClientWhenRemembered(client.clientID());
                    

                    ClientsEvents clientsEvent = new ClientsEvents(clientObject);
                    eventService.fireEvent(clientsEvent);



                    Map<String, Object> responseMap = new HashMap<>();
                    if (!clientObject.getCandidateMap().isEmpty()) {
                        responseMap.put("iceCandidates", clientObject.getCandidateMap());
                    }
                    if (Objects.nonNull(clientObject.getVideCallNotification())) {
                        responseMap.put("videoCall", clientObject.getVideCallNotification());
                    }
                    
                    responseMap.put("clientID", clientObject.getClientID());
                    responseMap.put("featureInUse", clientObject.getFeatureType().toString());/*this is a specific case I still need to investigate why enums would fail , could it be a Json lin issue or framework issue */
                    responseMap.put("lastSeen", clientObject.lastTimeStamp());
                  
                    
                    JsonObject data = JSON.createObjectBuilder()
                            .add("client", JSON.createObjectBuilder(responseMap))
                            .build();
                    
                    XUtils.buildSuccessResponse(response, 200, "Client  Remembered Successfully", data);
                    
                    
                }).exceptionally(ex-> XUtils.buildErrorResponse(ex,response,0,"Failed to process request"));
        
        
    }
    
    
    private void connectClient(ServerRequest request, ServerResponse response){
        request.content()
                .as(PostClient.class)
                .thenAccept(client->{
                    if (client == null) {
                        XUtils.buildErrorResponse(null, response,400, "PostClientRemember object is required!");
                    }

                    if (client.clientAgentName() == null || client.clientAgentName().isEmpty()) {
                        XUtils.buildErrorResponse(null, response,400, "Client agent name is required!");
                    }
                    Client clientObject = new Client(client.clientAgentName());
                    connectionsManager.addNewClient(clientObject);

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("clientID", clientObject.getClientID());
                    responseData.put("lastSeen", clientObject.lastTimeStamp());

                    JsonObject data = JSON.createObjectBuilder()
                            .add("client", JSON.createObjectBuilder(responseData))
                            .build();

                    XUtils.buildSuccessResponse(response, 200, "Client newly connected and recognized", data);

                }).exceptionally(ex-> XUtils.buildErrorResponse(ex,response,0,"Failed to process request"));


    }
    
    
    
}
