package africa.jopen.controllers;

import africa.jopen.http.videocall.PostSDPOffer;
import africa.jopen.http.videoroom.PostIceCandidate;
import africa.jopen.http.videoroom.PostSDPAnswer;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.XUtils;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Path("/streams")

public class GStreamingController {
    ConnectionsManager connectionsManager = ConnectionsManager.getInstance();

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

        if (Objects.isNull( clientObject.getWebRTCGStreamer())){
            return XUtils.buildErrorResponse(false, 200, "Offer was not yet sent from this client", Map.of());
        }
        clientObject.getWebRTCGStreamer()
                .handleIceSdp(payload.iceCandidate().candidate() ,payload.iceCandidate().sdpMidLineIndex());
        return XUtils.buildSuccessResponse(true, 200, "Updated Clients Ice Candidates ", Map.of());
    }


    @POST
    @Path("/send-answer")
    public Response sendAnswer(PostSDPAnswer payload) {
        if (Objects.isNull(payload)) {
            return XUtils.buildErrorResponse(false, 400, "Payload object is required!", Map.of());
        }

        var testExists = connectionsManager.checkIfClientExists(payload.clientID());
        if (!testExists) {
            return XUtils.buildErrorResponse(false, 400, "Client Not Found Please reconnect to the server feature!", Map.of());

        }

        var clientObject = connectionsManager.updateClientWhenRemembered(payload.clientID());

        clientObject.getRtcModel().setAnswer(payload.answer());
        clientObject.setFeatureType(FeatureTypes.G_STREAM);

       // connectionsManager.updateClient(clientObject);
       // clientObject.setWebRTCSendRecv();
        clientObject.getWebRTCGStreamer().handleSdp(
                payload.answer()
        );

        clientObject.getWebRTCGStreamer().startCall();
        connectionsManager.updateClient(clientObject);

        Map<String, Object> responseMap = new HashMap<>();

        return XUtils.buildSuccessResponse(true, 200, "Client answered  Successfully", Map.of("client", responseMap));
    }
 @POST
    @Path("/send-answerxxxxxxxxxxxxxx")
    public Response sendAnswerxxxxx(PostSDPOffer payload) {
        if (Objects.isNull(payload)) {
            return XUtils.buildErrorResponse(false, 400, "Payload object is required!", Map.of());
        }

        var testExists = connectionsManager.checkIfClientExists(payload.clientID());
        if (!testExists) {
            return XUtils.buildErrorResponse(false, 400, "Client Not Found Please reconnect to the server feature!", Map.of());

        }

        var clientObject = connectionsManager.updateClientWhenRemembered(payload.clientID());

        clientObject.getRtcModel().setOffer(payload.offer());
        clientObject.setFeatureType(FeatureTypes.G_STREAM);

        connectionsManager.updateClient(clientObject);
        clientObject.setWebRTCGStreamer(null);
        clientObject.getWebRTCGStreamer().handleSdp(
                payload.offer()
        );

        clientObject.getWebRTCGStreamer().startCall();
        connectionsManager.updateClient(clientObject);

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
        //clientObject.getRtcModel().setOffer(payload.answer());
        clientObject.setFeatureType(FeatureTypes.G_STREAM);

        clientObject.setWebRTCGStreamer(null);

        //String offer = clientObject.getWebRTCSendRecv().startWebRTCDescriptions();


       // clientObject.getWebRTCSendRecv().startCall();
        connectionsManager.updateClient(clientObject);

        Map<String, Object> responseMap = new HashMap<>();
       // responseMap.put("answer",offer);

        return XUtils.buildSuccessResponse(true, 200, "Streaming Started Successfully, the app should start to receive some streams", Map.of("client", responseMap));

    }
}
