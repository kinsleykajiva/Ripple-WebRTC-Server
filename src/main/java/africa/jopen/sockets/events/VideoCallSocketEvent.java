package africa.jopen.sockets.events;

import africa.jopen.models.Client;
import africa.jopen.utils.ConnectionsManager;
import com.google.common.flogger.FluentLogger;
import org.json.JSONObject;

public class VideoCallSocketEvent {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final ConnectionsManager connectionsManager = ConnectionsManager.getInstance();

    public static void handleVideoCallRequest( Client clientObject, JSONObject messageObject, JSONObject response) {

    }

}
