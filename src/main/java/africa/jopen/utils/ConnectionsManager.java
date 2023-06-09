package africa.jopen.utils;


import africa.jopen.events.ClientsEvents;
import africa.jopen.exceptions.ClientException;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.models.VideCallNotification;
import com.google.common.flogger.FluentLogger;
import jakarta.inject.Singleton;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Version;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Singleton
public class ConnectionsManager {
/*
	@Inject
	Event<ClientsEvents> clientsEventsEvent;
*/

/*	@Inject
	@ClientsEventQualifier
	Event<ClientsEvents> clientsEventsEvent;*/

	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private static final MutableList<Client> CLIENTS = Lists.mutable.empty();
	public static MutableList<RoomModel> ROOMS = Lists.mutable.empty();
	
	private ConnectionsManager() {
		// Private constructor to enforce singleton pattern
		logger.atInfo().log("started now ConnectionsManager");
		XUtils.GStreamerUtils.configurePaths();
		//GLib.setEnv("GST_DEBUG", "4", true);
		Gst.init(Version.of(1, 16));
	}
	private static class Holder {
		private static final ConnectionsManager INSTANCE = new ConnectionsManager();
	}
	public static ConnectionsManager getInstance() {
		return Holder.INSTANCE;
	}
	
	public Optional<RoomModel> getRoomById(String roomId) {
		return ROOMS.detectOptional(room -> room.getRoomID().equals(roomId));
	}
	
	public void addRoom(RoomModel roomModel) {
		ROOMS.add(roomModel);
	}
	
	public void updateRoom(RoomModel room, String clientID) {
		int index = CLIENTS.detectIndex(client -> client.getClientID().equals(clientID));
		var updatedClient = room.getParticipants().detectOptional(client -> client.getClientID().equals(clientID));
		if (updatedClient.isPresent()) {
			if (index >= 0) {
				CLIENTS.set(index, updatedClient.get());
				logger.atInfo().log("Updated client via an Room Accesses");
			} else {
				logger.atInfo().log("failed to update client ,it's either missing or gone");
			}
		}
		ROOMS.replaceAll(oldRoom -> oldRoom.getRoomID().equals(room.getRoomID()) ? room : oldRoom);
		
	}
	
	public void addNewClient(Client client) {
		CLIENTS.add(client);
	}
	
	public MutableList<Map<String, Object>> list() {
		MutableList<Map<String, Object>> returnVal = Lists.mutable.empty();
		CLIENTS.forEach(client -> returnVal.add(Map.of(
								"clientID", client.getClientID(),
								"lastSeen", client.lastTimeStamp()
						)
				)
		);
		
		return returnVal;
	}
	
	public boolean checkIfClientExists(String id) {
		//  check if client exists using the client d property
		return CLIENTS.anySatisfy(client -> client.getClientID().equals(id));
	}
	
	
	public Optional<Client> getClient(String id) {
		//  check if client exists using the client d property
		return CLIENTS.select(client -> client.getClientID().equals(id)).stream().findFirst();
	}


	
	public Client updateClientWhenRemembered(String id) {
		// update client of the client object in the list and return the client object
		for (Client client : CLIENTS) {
			if (client.getClientID().equals(id)) {
				client.updateLastTimeStamp(System.currentTimeMillis());
				ClientsEvents mClientsEvent = new ClientsEvents(client);
			//	clientsEventsEvent.fire(mClientsEvent);
				return client;
			}
			
		}
		
		throw new ClientException("Client not found ,IllegalStateException");
	}
	
	public Client updateClientAboutVideoCall(String id, VideCallNotification notification) {
		// update client of the client object in the list and return the client object
		for (Client client : CLIENTS) {
			if (client.getClientID().equals(id)) {
				client.setVideCallNotification(notification);
				return updateClientWhenRemembered(id);
				
			}
		}
		
		throw new ClientException("Client not found ,IllegalStateException");
	}
	
	public Client updateClient(Client client_) {
		
		CLIENTS.replaceAll(oldClient -> oldClient.getClientID().equals(client_.getClientID()) ? client_ : oldClient);
		
		return updateClientWhenRemembered(client_.getClientID());
	}
	
	public void removeClient(Client client) {
		CLIENTS.remove(client);
		logger.atInfo().log("freeing some objects");
	}
	
	/*
	 * remove clients that are lastTimeStamp longer than 30 seconds
	 * */
	public void removeOrphanClients() {
		long currentTime = System.currentTimeMillis();
		CLIENTS.removeIf(client -> (currentTime - client.lastTimeStamp()) > 30_000);

		// Clear references to removed clients from memory
		CLIENTS.forEach(client -> client = null);
		// Clear removed clients from memory
		logger.atInfo().log("freeing some objects");


	}
	
	/**
	 * This has to be reset the client notification for calls, it won't be sent when we remember this client
	 */
	public void removeDeadCallNotifications() {
		CLIENTS.stream()
				.filter(client -> Objects.nonNull(client.getVideCallNotification()))
				.filter(client -> System.currentTimeMillis() > client.getVideCallNotification().end())
				.forEach(client -> client.setVideCallNotification(null));
		
	}
	

}
