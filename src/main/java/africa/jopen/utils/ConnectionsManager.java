package africa.jopen.utils;


import africa.jopen.models.Client;
import jakarta.inject.Singleton;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Map;

@Singleton
//@ApplicationScoped
public class ConnectionsManager {
    private ConnectionsManager() {
        // Private constructor to enforce singleton pattern
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

    public boolean isClientRemembered(String id) {
        //  check if client exists using the clientid property
        return clientsList.anySatisfy(client -> client.clientId().equals(id));
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


    public void removeClient(Client client) {
        clientsList.remove(client);
    }


    /*
     * remove clients that are lastTimeStamp longer than 30 seconds
     * */
    public void removeOrphanClients() {
        long currentTime = System.currentTimeMillis();
        clientsList.removeIf(client -> (currentTime - client.lastTimeStamp()) > 30000);
    }

    private static class Holder {
        private static final ConnectionsManager INSTANCE = new ConnectionsManager();
    }

    public static ConnectionsManager getInstance() {
        return Holder.INSTANCE;
    }
}
