package africa.jopen.controllers;

import africa.jopen.http.PostClientRemember;
import africa.jopen.http.videocall.PostSDPOffer;
import africa.jopen.http.videoroom.PostIceCandidate;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Path("/streams/g")

public class GStreamingController {
    @Inject
    ConnectionsManager connectionsManager;

    private String sessionId;

    public record GStreamPost(String clientID) {
    }

    public record GStreamPostEvent(String clientID, String event) {
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
        var clientObject = connectionsManager.updateClientWhenRemembered(payload.clientID());

        clientObject.getWebRTCSendRecv().handleIceSdp(payload.iceCandidate().candidate());
        return XUtils.buildSuccessResponse(true, 200, "Updated Clients Ice Candidates ", Map.of());
    }

    @POST
    @Path("/send-offer")
    public Response sendOffer(PostSDPOffer payload) {
        if (Objects.isNull(payload)) {
            return XUtils.buildErrorResponse(false, 400, "Payload object is required!", Map.of());
        }
        var clientObject = connectionsManager.updateClientWhenRemembered(payload.clientID());

        clientObject.getRtcModel().setOffer(payload.offer());


        connectionsManager.updateClient(clientObject);

        clientObject.getWebRTCSendRecv().handleSdp(
                new JSONObject()
                        .put("type", "offer")
                        .put("sdp", payload.offer())
                        .toString()
        );
        clientObject.setWebRTCSendRecv();
        clientObject.getWebRTCSendRecv().startCall();

        Map<String, Object> responseMap = new HashMap<>();

        return XUtils.buildSuccessResponse(true, 200, "Client  Remembered Successfully", Map.of("client", responseMap));
    }



   /* @POST
    @Path("/new-ask")
    public Response askToAttainStream(GStreamPost client) {
        if (client.clientID() == null || client.clientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "clientID is required!", Map.of());
        }

        var testExists = connectionsManager.checkIfClientExists(client.clientID());
        if (!testExists) {
            return XUtils.buildErrorResponse(false, 400, "Client Not Found Please reconnect to the server feature!", Map.of());

        }
        var clientObject = connectionsManager.updateClientWhenRemembered(client.clientID());
        clientObject.setWebRTCSendRecv();
        Map<String, Object> responseMap = new HashMap<>();

        return XUtils.buildSuccessResponse(true, 200, "Client  Remembered Successfully", Map.of("client", responseMap));

    }*/

    @POST
    @Path("/start")
    public Response startStream(GStreamPostEvent client) {
        if (client.clientID() == null || client.clientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "clientID is required!", Map.of());
        }

        var testExists = connectionsManager.checkIfClientExists(client.clientID());
        if (!testExists) {
            return XUtils.buildErrorResponse(false, 400, "Client Not Found Please reconnect to the server feature!", Map.of());

        }
        var clientObject = connectionsManager.updateClientWhenRemembered(client.clientID());
        clientObject.getWebRTCSendRecv().startCall();
        Map<String, Object> responseMap = new HashMap<>();

        return XUtils.buildSuccessResponse(true, 200, "Streaming Started Successfully, the app should start to receive some streams", Map.of("client", responseMap));

    }
}
