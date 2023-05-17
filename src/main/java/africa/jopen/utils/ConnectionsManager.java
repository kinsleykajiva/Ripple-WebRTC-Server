package africa.jopen.utils;

import africa.jopen.models.Client;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsManager {
    private static MutableList<Client> clientsList = Lists.mutable.empty();

    public static void addNewClient(Client client) {
        clientsList.add(client);
    }


    public static void removeClient(Client client) {
        clientsList.remove(client);
    }


    /*
     * remove clients that are lastTimeStamp longer than 30 seconds
     * */
    public static void removeOrphanClients() {
        long currentTime = System.currentTimeMillis();
        clientsList.removeIf(client -> (currentTime - client.lastTimeStamp()) > 30000);
    }

}
