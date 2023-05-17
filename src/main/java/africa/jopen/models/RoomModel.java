package africa.jopen.models;

import africa.jopen.events.ClientsEvents;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

@ApplicationScoped
public  class RoomModel {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private FeatureTypes featureTypes;
    private final String roomID;
    private  String roomName="";
    private  String roomDescription;
    private  String creatorClientID;
    private  String password;
    private  String pin;
    private int maximumCapacity = 4;
    private final long createdTimeStamp = System. currentTimeMillis();
    private final MutableList<Client> clients = Lists.mutable.empty();

    public void onEvent(@Observes ClientsEvents event) {



        Client updatedClient = event.getClient();

        int index = clients.detectIndex(client -> client.clientId().equals(updatedClient.clientId()));

        if (index >= 0) {
            clients.set(index, updatedClient);
            logger.atInfo().log("Updated client via an Event");
        }else{
            logger.atInfo().log("failed to update client , is missing or gone");
        }


        //: ToDo remove the client if the client does not keeping on hit remember me end point so that the server knows the user sis still relevent

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

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setRoomDescription(String roomDescription) {
        this.roomDescription = roomDescription;
    }

    public void setCreatorClientID(String creatorClientID) {
        this.creatorClientID = creatorClientID;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public void setMaximumCapacity(int maximumCapacity) {
        this.maximumCapacity = maximumCapacity;
    }

    @Inject
    public RoomModel(){
        roomID = XUtils.IdGenerator();
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

    public String getRoomID() {
        return roomID;
    }

    public String getRoomDescription() {
        return roomDescription;
    }


    public String getCreatorClientID() {
        return creatorClientID;
    }


    public String getPassword() {
        return password;
    }



    public String getPin() {
        return pin;
    }


    public int getMaximumCapacity() {
        return maximumCapacity;
    }



    public long getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public MutableList<Client> getParticipants() {
        return clients;
    }

    public void addParticipant(Client client) {
        this.clients.add(client);
    }
}
