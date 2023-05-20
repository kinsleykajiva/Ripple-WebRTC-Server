package africa.jopen.controllers;



import africa.jopen.http.videocall.PostIceCandidate;
import africa.jopen.http.videocall.PostSDPOffer;
import africa.jopen.http.videocall.PostVideoMakeCall;
import africa.jopen.http.videocall.PostVideoRegister;
import africa.jopen.models.Client;
import africa.jopen.models.RoomModel;
import africa.jopen.models.VideCallNotification;
import africa.jopen.utils.ConnectionsManager;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@Path("/video-call")
public class VideoCallController {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    @Inject
    ConnectionsManager connectionsManager;

    @POST
    @Path("/update-ice-candidate")
    public Response updateIce(PostIceCandidate payload) {
        if (payload == null) {
            return XUtils.buildErrorResponse(false, 400, "Payload object is required!", Map.of());
        }

        if (payload.clientID() == null || payload.clientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Client ID is required!", Map.of());
        }


        if (payload.iceCandidate().candidate() == null ) {
            return XUtils.buildErrorResponse(false, 400, "icecandidate is required!", Map.of());
        }

        var clientOptional = connectionsManager.getClient(payload.clientID());


        if (clientOptional.isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Client not found!", Map.of());
        }


        Client clientModel = clientOptional.get();
        clientModel.addIceCandidate(payload.iceCandidate());
        connectionsManager.updateClient(clientModel);

        return XUtils.buildSuccessResponse(true, 200, "Updated Clients Ice Candidates ", Map.of());


    }

    @POST
    @Path("/send-offer")
    public Response sendOffer(PostSDPOffer payload) {
        if (payload == null) {
            return XUtils.buildErrorResponse(false, 400, "Payload object is required!", Map.of());
        }

        if (payload.clientID() == null || payload.clientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Client ID is required!", Map.of());
        }

        if (payload.offer() == null || payload.offer().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "Offer is required!", Map.of());
        }
        var clientOptional = connectionsManager.getClient(payload.clientID());

        assert clientOptional.isPresent();

        var clientObject = clientOptional.get();

        CompletableFuture<Response> future = CompletableFuture.supplyAsync(() -> {
            String responseAnswer = clientObject.processSdpOfferAsRemoteDescription();
            connectionsManager.updateClient(clientObject);
            return XUtils.buildSuccessResponse(true, 200, "SDP Offer processed, here is the answer ", Map.of("sdp", responseAnswer));
        });
        try {
            // Retrieve the response from the CompletableFuture
            return future.get();
        } catch (Exception e) {
            logger.atInfo().withCause(e).log("Error");
            return XUtils.buildErrorResponse(false, 500, "Error processing SDP Offer", Map.of());
        }
    }

    @POST
    @Path("/make-call")
    public Response call(PostVideoMakeCall payload) {
        if (Objects.isNull(payload)) {
            return XUtils.buildErrorResponse(false, 400, "payload  object is required!", Map.of());
        }


        if (payload.fromClientID() == null || payload.fromClientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "fromClientID is required!", Map.of());
        }

        if (payload.toClientID() == null || payload.toClientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "toClientID is required!", Map.of());
        }

        var testToClientExists = connectionsManager.checkIfClientExists(payload.toClientID());
        var testFromClientExists = connectionsManager.checkIfClientExists(payload.fromClientID());

        if (!testToClientExists) {
            return XUtils.buildErrorResponse(false, 404, "Target Client Not Found !", Map.of());

        }
        if (!testFromClientExists) {
            return XUtils.buildErrorResponse(false, 404, "You the attempting Client Not Found, Register again to the server !", Map.of());
        }

        // send this as part of the next remeber cycle of the target Client
        long start = System.currentTimeMillis();
        long life = TimeUnit.SECONDS.toMillis(20);
        long end = start + life;

        var notification = new VideCallNotification(payload.fromClientID(), payload.toClientID(), start, end);
        connectionsManager.updateClientAboutVideoCall(payload.toClientID(), notification);

        return XUtils.buildSuccessResponse(true, 200, "Client notified, call in progress!", Map.of());

    }

    /**
     * This client is just available there is no room involed as much .
     */
    @POST
    @Path("/register")
    public Response register(PostVideoRegister payload) {

        if (Objects.isNull(payload)) {
            return XUtils.buildErrorResponse(false, 400, "payload  object is required!", Map.of());
        }

        if (payload.clientID() == null || payload.clientID().isEmpty()) {
            return XUtils.buildErrorResponse(false, 400, "clientID is required!", Map.of());
        }

        var testExists = connectionsManager.checkIfClientExists(payload.clientID());
        if (!testExists) {
            return XUtils.buildErrorResponse(false, 404, "Client Not Found Please reconnect to the server feature!", Map.of());

        }
        var clientOptional = connectionsManager.getClient(payload.clientID());

        assert clientOptional.isPresent();

        var clientObject = clientOptional.get();

        clientObject.setFeatureType(FeatureTypes.VIDEO_CALL);
        return XUtils.buildSuccessResponse(true, 200, "Client  Ready To get calls", Map.of("client",
                Map.of(

                        "clientID", clientObject.clientId(),
                        "featureInUse", clientObject.getFeatureType(),
                        "lastSeen", clientObject.lastTimeStamp()
                )));


    }

}