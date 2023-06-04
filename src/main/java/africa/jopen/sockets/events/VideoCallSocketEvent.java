package africa.jopen.sockets.events;

import africa.jopen.http.IceCandidate;
import africa.jopen.http.videocall.PostIceCandidate;
import africa.jopen.http.videocall.PostSDPOffer;
import africa.jopen.models.Client;
import africa.jopen.models.configs.main.MainConfigModel;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.Events;
import africa.jopen.utils.XUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.flogger.FluentLogger;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static africa.jopen.sockets.ClientWebSocket.broadcast;
import static africa.jopen.sockets.ClientWebSocket.rememberResponse;

public class VideoCallSocketEvent {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final ConnectionsManager connectionsManager = ConnectionsManager.getInstance();

    public static void handleVideoCallRequest(Client client, JSONObject messageObject, JSONObject response) {
        final String requestType = messageObject.getString("requestType");
        response.put("history", messageObject);
        switch (requestType) {
            case "remember" -> response = rememberResponse(connectionsManager, client);

            case "update-ice-candidate" -> {
                var payload = new PostIceCandidate(
                        new IceCandidate(
                                messageObject.getString("candidate"),
                                messageObject.getString("sdpMid"),
                                messageObject.getInt("sdpMidLineIndex")
                        ), client.getClientID()
                );
                client.addIceCandidate(payload.iceCandidate());
                connectionsManager.updateClient(client);

                response = XUtils.buildJsonSuccessResponse(200, "eventType", Events.NOTIFICATION_EVENT,
                        "Updated Clients Ice Candidates ", response);
            }
            case "send-offer" -> {
                PostSDPOffer payload = new PostSDPOffer(messageObject.getString("offer"), client.getClientID());
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
                    var sdp = future.get();
                    response.put("sdp", sdp);
                    response = XUtils.buildJsonSuccessResponse(200, "eventType", "answer",
                            "SDP Offer processed, here is the answer ", response);

                } catch (Exception e) {
                    logger.atInfo().withCause(e).log("Error");
                    // return XUtils.buildErrorResponse(false, 500, "Error processing SDP Offer", Map.of());
                }
            }
        }
        broadcast(client, response.toString());
    }

}
