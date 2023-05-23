package africa.jopen.controllers;

import africa.jopen.events.ClientsEvents;
import africa.jopen.http.PostClient;
import africa.jopen.http.PostClientRemember;
import africa.jopen.models.Client;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Path("/app")
public class GeneralController {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	@Inject
	ConnectionsManager connectionsManager;
	@Inject
	Event<ClientsEvents> clientsEventsEvent;
	
	@GET
	@Path("/client/all")
	public Response getAllClient() {
		
		return XUtils.buildSuccessResponse(true, 200, "Clients Available ", Map.of("clients", connectionsManager.list()));
		
		
	}
	
	
	@POST
	@Path("/client/remember")
	public Response rememberMe(PostClientRemember client) {
		
		if (client == null) {
			return XUtils.buildErrorResponse(false, 400, "PostClientRemember object is required!", Map.of());
		}
		
		if (client.clientID() == null || client.clientID().isEmpty()) {
			return XUtils.buildErrorResponse(false, 400, "clientID is required!", Map.of());
		}
		
		var testExists = connectionsManager.checkIfClientExists(client.clientID());
		if (!testExists) {
			return XUtils.buildErrorResponse(false, 400, "Client Not Found Please reconnect to the server feature!", Map.of());
			
		}
		
		var clientObject = connectionsManager.updateClientWhenRemembered(client.clientID());
		
		ClientsEvents mClientsEvent = new ClientsEvents(clientObject);
		clientsEventsEvent.fire(mClientsEvent);
		Map<String, Object> responseMap = new HashMap<>();
		if (!clientObject.getCandidateMap().isEmpty()) {
			responseMap.put("iceCandidates", clientObject.getCandidateMap());
		}
		if (Objects.nonNull(clientObject.getVideCallNotification())) {
			responseMap.put("videoCall", clientObject.getVideCallNotification());
		}
		if ( Objects.nonNull(clientObject.getRtcModel().answer()) && !clientObject.getRtcModel().answer().isEmpty()) {
			responseMap.put("sdpAnswer", clientObject.getRtcModel().answer());
		}
		
		responseMap.put("clientID", clientObject.getClientID());
		responseMap.put("featureInUse", clientObject.getFeatureType().toString());
		responseMap.put("lastSeen", clientObject.lastTimeStamp());

		clientObject.resetCandidateMap();
		clientObject.setVideCallNotification(null);
		connectionsManager.updateClient(clientObject);
		
		return XUtils.buildSuccessResponse(true, 200, "Client  Remembered Successfully", Map.of("client",
				responseMap));
		
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
		responseData.put("clientID", clientObject.getClientID());
		responseData.put("lastSeen", clientObject.lastTimeStamp());
		
		return XUtils.buildSuccessResponse(true, 200, "Client newly connected and recognized", responseData);
	}
}
