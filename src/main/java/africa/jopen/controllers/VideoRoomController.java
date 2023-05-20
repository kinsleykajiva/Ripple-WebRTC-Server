package africa.jopen.controllers;

import africa.jopen.events.ClientsEvents;
import africa.jopen.http.*;
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
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
			return XUtils.buildErrorResponse(false, 400, "Room ID is required!", Map.of());
			
		}
		var roomModelOptional = ConnectionsManager.ROOMS.select(roomM -> roomM.getRoomID().equals(roomID));
		if (roomModelOptional.isEmpty()) {
			
			return XUtils.buildErrorResponse(false, 400, "Room Not found!", Map.of());
		}
		final var roomModel = roomModelOptional.getOnly();
		return XUtils.buildSuccessResponse(true, 200, "Room Information ", Map.of(
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
		));
		
		
	}
	
	@POST
	@Path("/update-ice-candidate")
	public Response updateIce(PostIceCandidate payload) {
		if (payload == null) {
			return XUtils.buildErrorResponse(false, 400, "Payload object is required!", Map.of());
		}
		
		if (payload.clientID() == null || payload.clientID().isEmpty()) {
			return XUtils.buildErrorResponse(false, 400, "Client ID is required!", Map.of());
		}
		
		
		if (payload.iceCandidate().candidate() == null) {
			return XUtils.buildErrorResponse(false, 400, "icecandidate is required!", Map.of());
		}
		Optional<RoomModel> roomModelOptional = connectionsManager.getRoomById(payload.roomID());
		if (roomModelOptional.isEmpty()) {
			return XUtils.buildErrorResponse(false, 400, "Room not found!", Map.of());
		}
		
		RoomModel roomModel = roomModelOptional.get();
		Optional<Client> clientModelOptional = roomModel.getParticipants().stream()
				.filter(client -> client.clientId().equals(payload.clientID()))
				.findFirst();
		if (clientModelOptional.isEmpty()) {
			return XUtils.buildErrorResponse(false, 400, "Client not found!", Map.of());
		}
		Client clientModel = clientModelOptional.get();
		clientModel.addIceCandidate(payload.iceCandidate());
		clientModel.setFeatureType(FeatureTypes.VIDEO_ROOM);// Todo Review if we need to always set this at this point
		connectionsManager.updateRoom(roomModel, payload.clientID());
		
		return XUtils.buildSuccessResponse(true, 200, "Updated Clients Ice Candidates ", Map.of());
		
		
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
		Optional<Client> clientModelOptional = roomModel.getParticipants().stream()
				.filter(client -> client.clientId().equals(payload.clientID()))
				.findFirst();
		if (clientModelOptional.isEmpty()) {
			return XUtils.buildErrorResponse(false, 400, "Client not found!", Map.of());
		}
		
		Client clientModel = clientModelOptional.get();
		clientModel.setFeatureType(FeatureTypes.VIDEO_ROOM);
		clientModel.getRtcModel().setOffer(payload.offer());
		
		
		CompletableFuture<Response> future = CompletableFuture.supplyAsync(() -> {
			String responseAnswer = clientModel.processSdpOfferAsRemoteDescription();
			connectionsManager.updateRoom(roomModel, payload.clientID());
			
			return XUtils.buildSuccessResponse(true, 200, "SDP Offer processed, here is the answer ", Map.of("sdp", responseAnswer));
		});
		
		try {
			// Retrieve the response from the CompletableFuture
			return future.get();
		} catch (Exception e) {
			logger.atInfo().withCause(e).log("Error");
			return XUtils.buildErrorResponse(false, 500, "Error processing SDP Offer", Map.of());
		}
		
		
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
		// test auth of the joing the room ,
		if (!roomModel.getPassword().equals(room.password())) {
			// ToDo:The issue here is that this will need to review if the creator still to test auth or not
			// ToDo: as per the logic for now the creatort is already loaded in momery of the server as an active client object
			return XUtils.buildErrorResponse(false, 400, "Room authentication failed! Access rejected.", Map.of());
		}
		
		Client clientObject = connectionsManager.updateClientWhenRemembered(room.clientID());
		clientObject.setFeatureType(FeatureTypes.VIDEO_ROOM);
		RoomModel updatedRoomModel = roomModel.addParticipant(clientObject);
		connectionsManager.updateRoom(updatedRoomModel, room.clientID());
		
		return XUtils.buildSuccessResponse(true, 200, "Added to room", Map.of());
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
			clientOptional.get().setFeatureType(FeatureTypes.VIDEO_ROOM);
			roomModel.addParticipant(clientOptional.get());
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
			
			return XUtils.buildSuccessResponse(true, 200, "Room created successfully", responseData);
		} catch (Exception e) {
			logger.atWarning().withCause(e).log("Error while creating room: " + room);
			return XUtils.buildErrorResponse(false, 400, "Failed to create room!", Map.of());
		}
	}
	
	
	@POST
	@Path("/connect")
	//@Deprecated
	public Response connectClient(PostClient client) {
		// ToDo : this is now moved to the general controller
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
