package africa.jopen.utils;


import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.models.VideCallNotification;
import com.google.common.flogger.FluentLogger;
import jakarta.inject.Singleton;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Singleton
//@ApplicationScoped
public class ConnectionsManager {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private ConnectionsManager() {
        // Private constructor to enforce singleton pattern
    }


    public static MutableList<RoomModel> roomsList = Lists.mutable.empty();

    public Optional<RoomModel> getRoomById(String roomId) {
        return roomsList.detectOptional(room -> room.getRoomID().equals(roomId));
    }

    public void addRoom(RoomModel roomModel) {
        roomsList.add(roomModel);
    }


    public void updateRoom(RoomModel room, String clientID) {
        int index = clientsList.detectIndex(client -> client.clientId().equals(clientID));
        var updatedClient = room.getParticipants().detectOptional(client -> client.clientId().equals(clientID));
        if (updatedClient.isPresent()) {
            if (index >= 0) {
                clientsList.set(index, updatedClient.get());
                logger.atInfo().log("Updated client via an Room Accesss");
            } else {
                logger.atInfo().log("failed to update client , is missing or gone");
            }
        }
        roomsList.replaceAll(oldRoom -> oldRoom.getRoomID().equals(room.getRoomID()) ? room : oldRoom);

    }

    private static MutableList<Client> clientsList = Lists.mutable.empty();

    public void addNewClient(Client client) {
        clientsList.add(client);
    }


    public MutableList<Map<String, Object>> list() {
        MutableList<Map<String, Object>> returnVal = Lists.mutable.empty();
        clientsList.forEach(client -> returnVal.add(Map.of(
                "clientID", client.clientId(),
                "lastSeen", client.lastTimeStamp()
        )));

        return returnVal;
    }

    public boolean checkIfClientExists(String id) {
        //  check if client exists using the clientid property
        return clientsList.anySatisfy(client -> client.clientId().equals(id));
    }

    public Optional<Client> getClient(String id) {
        //  check if client exists using the clientid property
        return clientsList.select(client -> client.clientId().equals(id)).stream().findFirst();
    }

    public Client updateClientWhenRemembered(String id) {
        // update client of the cleint object in the list and retunn the client object
        for (Client client : clientsList) {
            if (client.clientId().equals(id)) {
                client.updateLastTimeStamp(System.currentTimeMillis());
                return client;
            }
        }

        throw new RuntimeException("Client not found ,IllegalStateException");
    }


    public Client updateClientAboutVideoCall(String id, VideCallNotification notification) {
        // update client of the cleint object in the list and retunn the client object
        for (Client client : clientsList) {
            if (client.clientId().equals(id)) {
                client.setVideCallNotification(notification);
                return updateClientWhenRemembered(id);

            }
        }

        throw new RuntimeException("Client not found ,IllegalStateException");
    }

    public Client updateClient(Client client_) {

        clientsList.replaceAll(oldClient -> oldClient.clientId().equals(client_.clientId()) ? client_ : oldClient);

        return updateClientWhenRemembered(client_.clientId());
    }


    public void removeClient(Client client) {
        clientsList.remove(client);
    }


    /*
     * remove clients that are lastTimeStamp longer than 30 seconds
     * */
    public void removeOrphanClients() {
        long currentTime = System.currentTimeMillis();
        //  clientsList.removeIf(client -> (currentTime - client.lastTimeStamp()) > 30_000);
    }

    /**
     * This has to be reset the client notification for calls, it won't be sent when we remember this client
     */
    public void removeDeadCallNotifications() {
        clientsList.stream()
                .filter(client -> System.currentTimeMillis() > client.getVideCallNotification().end())
                .forEach(client -> client.setVideCallNotification(null));

    }

    private static class Holder {
        private static final ConnectionsManager INSTANCE = new ConnectionsManager();
    }

    public static ConnectionsManager getInstance() {
        return Holder.INSTANCE;
    }
}
