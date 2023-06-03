package africa.jopen.sockets.events;

import africa.jopen.http.videocall.PostSDPOffer;
import africa.jopen.models.Client;
import africa.jopen.models.configs.main.MainConfigModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.XUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.flogger.FluentLogger;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static africa.jopen.sockets.ClientWebSocket.rememberResponse;

public class VideoCallSocketEvent {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final ConnectionsManager connectionsManager = ConnectionsManager.getInstance();

    public static void handleVideoCallRequest( Client client, JSONObject messageObject, JSONObject response) {
        final String requestType = messageObject.getString("requestType");
        response.put("history", messageObject);
        switch (requestType) {
            case "remember" -> response = rememberResponse(connectionsManager,client);

            case "send-offer" -> {

                //PostSDPOffer payload = objectMapper.readValue(file, PostSDPOffer.class);
                PostSDPOffer payload = new PostSDPOffer(messageObject.getString("offer") ,client.getClientID());
                var clientOptional = connectionsManager.getClient(payload.clientID());

                assert clientOptional.isPresent();

                var clientObject = clientOptional.get();
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    String responseAnswer = clientObject.processSdpOfferAsRemoteDescription();
                    connectionsManager.updateClient(clientObject);
                    return responseAnswer;
                });

                try {
                    // Retrieve the response from the CompletableFuture
                    var sdp= future.get();
                } catch (Exception e) {
                    logger.atInfo().withCause(e).log("Error");
                   // return XUtils.buildErrorResponse(false, 500, "Error processing SDP Offer", Map.of());
                }
            }
        }
    }

}
