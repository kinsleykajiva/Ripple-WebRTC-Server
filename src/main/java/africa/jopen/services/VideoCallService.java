package africa.jopen.services;

import africa.jopen.events.EventService;
import africa.jopen.http.PostClient;
import africa.jopen.http.videocall.PostIceCandidate;
import africa.jopen.http.videocall.PostSDPOffer;
import africa.jopen.http.videocall.PostVideoMakeCall;
import africa.jopen.http.videocall.PostVideoRegister;
import africa.jopen.models.Client;
import africa.jopen.models.VideCallNotification;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VideoCallService implements Service {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
	
	private final ConnectionsManager connectionsManager;
	private final EventService eventService;
	
	public VideoCallService(ConnectionsManager connectionsManager, EventService eventService) {
		this.connectionsManager = connectionsManager;
		this.eventService = eventService;
	}
	
	@Override
	public void update(Routing.Rules rules) {
		rules
				.post("/update-ice-candidate", this::updateIce)
				.post("/send-offer", this::sendOffer)
				.post("/make-call", this::call)
				.post("/register", this::register)
		;
	}
	
	
	private void updateIce(ServerRequest request, ServerResponse response) {
		request.content()
				.as(PostIceCandidate.class)
				.thenAccept(payload -> {
					if (payload == null) {
						
						XUtils.buildErrorResponse(null, response, 400, "Payload object is required!");
						return;
					}
					
					if (payload.clientID() == null || payload.clientID().isEmpty()) {
						
						XUtils.buildErrorResponse(null, response, 400, "Client ID is required!");
						return;
					}
					
					
					if (payload.iceCandidate().candidate() == null) {
						
						XUtils.buildErrorResponse(null, response, 400, "icecandidate is required!");
						return;
					}
					
					var clientOptional = connectionsManager.getClient(payload.clientID());
					
					if (clientOptional.isEmpty()) {
						
						XUtils.buildErrorResponse(null, response, 400, "Client not found!");
						return;
					}
					Client clientModel = clientOptional.get();
					clientModel.addIceCandidate(payload.iceCandidate());
					connectionsManager.updateClient(clientModel);
					
					JsonObject data = JSON.createObjectBuilder()
							                  .build();
					XUtils.buildSuccessResponse(response, 200, "Updated Clients Ice Candidates ", data);
					
				}).exceptionally(ex -> XUtils.buildErrorResponse(ex, response, 0, "Failed to process request"));
	}
	
	private void sendOffer(ServerRequest request, ServerResponse response) {
		request.content()
				.as(PostSDPOffer.class)
				.thenAccept(payload -> {
					
					if (Objects.isNull(payload)) {
						
						XUtils.buildErrorResponse(null, response, 400, "Payload object is required!");
						return;
					}
					
					if (payload.clientID() == null || payload.clientID().isEmpty()) {
						
						XUtils.buildErrorResponse(null, response, 400, "Client ID is required!");
						return;
					}
					
					if (payload.offer() == null || payload.offer().isEmpty()) {
						
						XUtils.buildErrorResponse(null, response, 400, "Client ID is required!");
						return;
					}
					var clientOptional = connectionsManager.getClient(payload.clientID());
					
					assert clientOptional.isPresent();
					
					var clientObject = clientOptional.get();
					
					CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
						String responseAnswer = clientObject.processSdpOfferAsRemoteDescription();
						connectionsManager.updateClient(clientObject);
						
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
	
	private void call(ServerRequest request, ServerResponse response) {
		request.content()
				.as(PostVideoMakeCall.class)
				.thenAccept(payload -> {
					if (Objects.isNull(payload)) {
						XUtils.buildErrorResponse(null, response, 400, "payload  object is required!");
						return;
					}
					if (payload.fromClientID() == null || payload.fromClientID().isEmpty()) {
						XUtils.buildErrorResponse(null, response, 400, "fromClientID is required!");
						return;
					}
					
					if (payload.toClientID() == null || payload.toClientID().isEmpty()) {
						
						XUtils.buildErrorResponse(null, response, 400, "toClientID is required!");
						return;
					}
					
					var testToClientExists = connectionsManager.checkIfClientExists(payload.toClientID());
					var testFromClientExists = connectionsManager.checkIfClientExists(payload.fromClientID());
					
					if (!testToClientExists) {
						XUtils.buildErrorResponse(null, response, 400, "Target Client Not Found !");
						return;
						
					}
					if (!testFromClientExists) {
						
						XUtils.buildErrorResponse(null, response, 400, "You the attempting Client Not Found, Register again to the server !");
						return;
					}
					var fromClientOptional = connectionsManager.getClient(payload.fromClientID());
					assert fromClientOptional.isPresent();
					
					// send this as part of the next remeber cycle of the target Client
					long start = System.currentTimeMillis();
					long life = TimeUnit.SECONDS.toMillis(20);
					long end = start + life;
					
					var notification = new VideCallNotification(XUtils.IdGenerator(), fromClientOptional.get().getClientAgentName(), payload.fromClientID(), payload.toClientID(), start, end);
					connectionsManager.updateClientAboutVideoCall(payload.toClientID(), notification);
					connectionsManager.updateClientAboutVideoCall(payload.fromClientID(), notification);
					// both involved parties knows about this notification
					// we need to exchange the sdp with the caller remote description
					JsonObject data = JSON.createObjectBuilder().build();
					
					XUtils.buildSuccessResponse(response, 200, "Client notified, call in progress!", data);
					
					
				}).exceptionally(ex -> XUtils.buildErrorResponse(ex, response, 0, "Failed to process request"));
	}
	
	private void register(ServerRequest request, ServerResponse response) {
		request.content()
				.as(PostVideoRegister.class)
				.thenAccept(payload -> {
					if (Objects.isNull(payload)) {
						
						XUtils.buildErrorResponse(null, response, 400, "payload  object is required!");
						return;
					}
					
					if (payload.clientID() == null || payload.clientID().isEmpty()) {
						
						XUtils.buildErrorResponse(null, response, 400, "clientID is required!");
						return;
					}
					
					var testExists = connectionsManager.checkIfClientExists(payload.clientID());
					if (!testExists) {
						
						XUtils.buildErrorResponse(null, response, 400, "Client Not Found Please reconnect to the server feature!");
						return;
						
					}
					var clientOptional = connectionsManager.getClient(payload.clientID());
					
					assert clientOptional.isPresent();
					
					var clientObject = clientOptional.get();
					
					clientObject.setFeatureType(FeatureTypes.VIDEO_CALL);
					
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("clientID", clientObject.getClientID());
					responseMap.put("featureInUse", clientObject.getFeatureType().toString());/*this is a specific case I still need to investigate why enums would fail , could it be a Json lin issue or framework issue */
					responseMap.put("lastSeen", clientObject.lastTimeStamp());
					
					JsonObject data = JSON.createObjectBuilder()
							                  .add("client", JSON.createObjectBuilder(responseMap))
							                  .build();
					
					XUtils.buildSuccessResponse(response, 200, "Client  Ready To get calls", data);
				}).exceptionally(ex -> XUtils.buildErrorResponse(ex, response, 0, "Failed to process request"));
	}
	
	
}
