package africa.jopen.models;

import africa.jopen.events.ClientsEvents;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;


import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.util.Map;


public class RoomModel {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private final String roomID;
	private final long createdTimeStamp = System.currentTimeMillis();
	private final MutableList<Client> clients = Lists.mutable.empty();
	private FeatureTypes featureTypes;
	private String roomName = "";
	private String roomDescription;
	private String creatorClientID;
	private String password;
	private String pin;
	private int maximumCapacity = 4;
	
	//@Inject
	public RoomModel() {
		roomID = XUtils.IdGenerator();
	}

   /* public RoomModel(@Nonnull FeatureTypes featureTypes,
                     String roomName,
                     String roomDescription,
                     String creatorClientID,
                     String password,
                     String pin) {
        this.featureTypes = featureTypes;
        this.roomName = roomName;
        this.roomDescription = roomDescription;
        this.creatorClientID = creatorClientID;
        this.password = password;
        this.pin = pin;
        roomID = XUtils.IdGenerator();
    }*/
   // Register the event handler to receive the events
   public void registerEventHandlers() {
	   //ClientsEvents.registerEventHandler(this::onEvent);
	 //  ClientsEvents.registerEventHandler(this::onClientEvent);
   }
	public void onEvent( ClientsEvents event) {
		
		
		Client updatedClient = event.getClient();
		
		int index = clients.detectIndex(client -> client.getClientID().equals(updatedClient.getClientID()));
		
		if (index >= 0) {
			clients.set(index, updatedClient);
			logger.atInfo().log("Updated client via an Event");
		} else {
			logger.atInfo().log("failed to update client , is missing or gone");
		}
		
		
		//: ToDo remove the client if the client does not keeping on hit remember me end point so that the server knows the user sis still relevent
		
	}
	
	public FeatureTypes getFeatureTypes() {
		return featureTypes;
	}
	
	public void setFeatureTypes(FeatureTypes featureTypes) {
		this.featureTypes = featureTypes;
	}
	
	public String getRoomName() {
		return roomName;
	}
	
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}
	
	public String getRoomID() {
		return roomID;
	}
	
	public String getRoomDescription() {
		return roomDescription;
	}
	
	public void setRoomDescription(String roomDescription) {
		this.roomDescription = roomDescription;
	}
	
	public String getCreatorClientID() {
		return creatorClientID;
	}
	
	public void setCreatorClientID(String creatorClientID) {
		this.creatorClientID = creatorClientID;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPin() {
		return pin;
	}
	
	public void setPin(String pin) {
		this.pin = pin;
	}
	
	public int getMaximumCapacity() {
		return maximumCapacity;
	}
	
	public void setMaximumCapacity(int maximumCapacity) {
		this.maximumCapacity = maximumCapacity;
	}
	
	public long getCreatedTimeStamp() {
		return createdTimeStamp;
	}
	
	public MutableList<Client> getParticipants() {
		return clients;
	}
	
	public MutableList<Map<String, Object>> getParticipantsDto() {
		MutableList<Map<String, Object>> returnVal = org.eclipse.collections.api.factory.Lists.mutable.empty();
		clients.forEach(client -> returnVal.add(Map.of(
				"clientID", client.getClientID(),
				"lastSeen", client.lastTimeStamp()
		)));
		
		return returnVal;
	}
	
	public RoomModel addParticipant(Client client) {
		boolean exists = clients.anySatisfy(client1 -> client1.getClientID().equals(client.getClientID()));
		if (!exists) {
			this.clients.add(client);
		}
		
		return this;
		
	}
}
