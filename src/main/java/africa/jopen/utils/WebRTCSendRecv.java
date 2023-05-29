package africa.jopen.utils;

import africa.jopen.models.Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.DecodeBin;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;
import org.freedesktop.gstreamer.webrtc.WebRTCSDPType;
import org.freedesktop.gstreamer.webrtc.WebRTCSessionDescription;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;


/**
 *
 */
public class WebRTCSendRecv {

    ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private Pipeline pipe;
    private WebRTCBin webRTCBin;
    private String clientID;

    /**
     * max-size-buffers  set to 1000, which determines the maximum number of buffers that can be held in the queue.
     * The buffering element helps in smoothing out the stream by allowing a certain number of buffers to be accumulated before passing them downstream
     */
   // private final String PIPELINE_DESCRIPTION = "videotestsrc is-live=true pattern=ball  ! video/x-raw,width=1024,height=768,framerate=20/1 ! videoconvert ! x264enc ! rtph264pay ! queue ! capsfilter caps=application/x-rtp,media=video,encoding-name=H264,payload=96";
    private final String PIPELINE_DESCRIPTION
            = """
            filesrc location="C:\\\\Users\\\\Kinsl\\\\Videos\\\\target.mp4" ! decodebin name=decoder
                       
            decoder. ! videoconvert ! queue2 max-size-buffers=1000 ! vp8enc deadline=1 ! rtpvp8pay ! queue ! application/x-rtp,media=video,encoding-name=VP8,payload=97 ! webrtcbin.
                       
            decoder. ! audioconvert ! audioresample ! queue2 max-size-buffers=1000 ! opusenc ! rtpopuspay ! queue ! application/x-rtp,media=audio,encoding-name=OPUS,payload=96 ! webrtcbin.
                       
            webrtcbin name=webrtcbin bundle-policy=max-bundle stun-server=stun://stun.l.google.com:19302
                     
             """; /// the target file is currently as test option but you can replace with your path but this is till neeeds to be stable still



    public WebRTCSendRecv(String clientID) {
        this.clientID = clientID;
        pipe = (Pipeline) Gst.parseLaunch(PIPELINE_DESCRIPTION);
        webRTCBin = (WebRTCBin) pipe.getElementByName("webrtcbin");
        setupPipeLogging(pipe);
        // When the pipeline goes to PLAYING, the on_negotiation_needed() callback
        // will be called, and we will ask webrtcbin to create an answer which will
        // match the pipeline above.
        // When webrtcbin has created the offer, it will hit our callback and we
        // send SDP offer over the websocket to signalling server
        WebRTCBin.ON_NEGOTIATION_NEEDED onNegotiationNeeded = elem -> {
            logger.atInfo().log("onNegotiationNeeded: " + elem.getName());

            // When webrtcbin has created the offer, it will hit our callback and we
            // send SDP offer over the websocket to signalling server
            webRTCBin.createOffer(onOfferCreated);
        };
        webRTCBin.connect(onNegotiationNeeded);
        WebRTCBin.ON_ICE_CANDIDATE onIceCandidate = (sdpMLineIndex, candidate) -> {
            var ice = new JSONObject().put("candidate", candidate).put("sdpMLineIndex", sdpMLineIndex);
            String json = new JSONObject().put("ice", ice).toString();
            logger.atInfo().log("ON_ICE_CANDIDATE: " + json);
            Map<String, Object> candidateMap = new HashMap<>();
            candidateMap.put("sdpMLineIndex", sdpMLineIndex);
            candidateMap.put("candidate", candidate);
            System.out.println("xxxxxxxxxxxxxxxcccccccccccccc ice my ice  " + ice);
            var clientObject = connectionsManager.getClient(clientID);
            assert clientObject.isPresent();
            clientObject.get().setCandidateMap(candidateMap);

            connectionsManager.updateClient(clientObject.get());
            if (Objects.nonNull(clientObject.get().getSocketSession())) {
                JSONObject response = new JSONObject();
                response.put("clientID", clientID);
                response.put("iceCandidates", candidateMap);
                response.put("lastSeen", clientObject.get().lastTimeStamp());
                response.put("featureInUse", clientObject.get().getFeatureType().toString());
                response = XUtils.buildJsonSuccessResponse(200, "eventType", "iceCandidates",
                        "Ice Shared", response);
                broadcast(clientObject.get(), response.toString());
            }

        };
        webRTCBin.connect(onIceCandidate);
        Element.PAD_ADDED onIncomingStream = (element, pad) -> {
            logger.atInfo().log("Receiving stream! Element : " + element.getName()
                    + " Pad : " + pad.getName());
            if (pad.getDirection() != PadDirection.SRC) {
                return;
            }
            DecodeBin decodeBin = new DecodeBin("decodebin_" + pad.getName());
            decodeBin.connect(onDecodedStream);
            pipe.add(decodeBin);
            decodeBin.syncStateWithParent();
            pad.link(decodeBin.getStaticPad("sink"));
        };
        webRTCBin.connect(onIncomingStream);
        logger.atInfo().log("initiating call");
        //Todo remove some of the code here is useless
       // startCall(); // called to trigger the ice process and sdp offer to the client
    }

    public void startCall() {
        if (!pipe.isPlaying()) {
            logger.atInfo().log("initiating streams");
            pipe.play();
            System.out.println("xxxxxxxxxxxxxxxcccccccccccccc play");
        }
    }

    private void endCall() {
        logger.atInfo().log("ending call");
        pipe.setState(State.NULL);
        //  Gst.quit();
    }

    public void handleSdp(String sdpStr) {
        try {
            logger.atInfo().log("Answer SDP:\n");
            SDPMessage sdpMessage = new SDPMessage();
            sdpMessage.parseBuffer(sdpStr);
            WebRTCSessionDescription description = new WebRTCSessionDescription(WebRTCSDPType.ANSWER, sdpMessage);
            webRTCBin.setRemoteDescription(description);
            //Todo remove some of the code here is useless
            System.out.println("xxxxxxxxxxxxxxxcccccccccccccc handleSdp " + sdpStr);

        } catch (Exception exception) {
            logger.atSevere().withCause(exception).log(exception.getLocalizedMessage());
        }
    }

    public void handleIceSdp(String candidate, int sdpMLineIndex) {
        try {
            System.out.println("xxxxxxxxxxxxxxxcccccccccccccc ice  " + candidate);
            logger.atInfo().log("Adding remote client ICE candidate : " + candidate);
            logger.atInfo().log("Adding remote client ice  validateIceCandidate : " + validateIceCandidate(candidate));
            logger.atInfo().log("Adding remote client ICE sdpMLineIndex : " + sdpMLineIndex);
            webRTCBin.addIceCandidate(sdpMLineIndex, candidate);
        } catch (Exception exception) {
            logger.atSevere().withCause(exception).log(exception.getLocalizedMessage());
        }
    }
    public boolean validateIceCandidate(String candidate) {
        String[] candidateFields = candidate.split(" ");
        if (candidateFields.length < 8) {
            return false;
        }
        if (!candidateFields[0].equals("a=candidate")) {
            return false;
        }
        if (!candidateFields[2].equals("UDP")) {
            return false;
        }
        if (!candidateFields[3].matches("\\d+")) {
            return false;
        }
        if (!candidateFields[4].matches("[^ ]+")) {
            return false;
        }
        if (!candidateFields[5].matches("\\d+")) {
            return false;
        }
        if (!candidateFields[6].equals("typ")) {
            return false;
        }
        if (!candidateFields[7].matches("[^ ]+")) {
            return false;
        }
        return true;
    }
    private void setupPipeLogging(Pipeline pipe) {
        Bus bus = pipe.getBus();
        bus.connect((Bus.EOS) source -> {
            logger.atInfo().log("Reached end of stream : " + source.toString());
            endCall();
        });

        bus.connect((Bus.ERROR) (source, code, message) -> {
            logger.atInfo().log("Error from source : " + source
                    + ", with code : " + code + ", and message : " + message);
            endCall();
        });

        bus.connect((source, old, current, pending) -> {
            if (source instanceof Pipeline) {
                logger.atInfo().log("Pipe state changed from " + old + " to " + current);
            }
        });
    }

    private final WebRTCBin.CREATE_OFFER onOfferCreated = offer -> {
        webRTCBin.setLocalDescription(offer);
        String sdpp = offer.getSDPMessage().toString();
        var sdp = new JSONObject();
        sdp.put("sdp", new JSONObject()
                .put("type", "offer")
                .put("sdp", sdpp));
        String json = sdp.toString();
        System.out.println("xxxxxxxxxxxxxxxcccccccccccccc sdp json " + json);
        logger.atInfo().log("Sending answer:\n" + sdpp);
        //Todo remove some of the code here is useless
        var clientObject = connectionsManager.getClient(clientID);
        assert clientObject.isPresent();
        clientObject.get().getRtcModel().setOffer(sdpp);
        connectionsManager.updateClient(clientObject.get());
        if (Objects.nonNull(clientObject.get().getSocketSession())) {
            JSONObject response = new JSONObject();
            response.put("clientID", clientID);
            response.put("clientSDP", sdpp);
            response.put("sdp", json);
            response.put("lastSeen", clientObject.get().lastTimeStamp());
            response.put("featureInUse", clientObject.get().getFeatureType().toString());
            response = XUtils.buildJsonSuccessResponse(200, "eventType", "webrtc",
                    "Client  Remembered Successfully", response);
            broadcast(clientObject.get(), response.toString());
        }

    };

    private void broadcast(Client client, String message) {
        client.getSocketSession().getAsyncRemote().sendObject(message, sendResult -> {
            if (sendResult.getException() != null) {
                logger.atSevere().withCause(sendResult.getException()).log("Failed to send message");
            }
        });


    }

    private final Element.PAD_ADDED onDecodedStream = (element, pad) -> {
        if (!pad.hasCurrentCaps()) {
            logger.atInfo().log("Pad has no current Caps - ignoring");
            return;
        }
        Caps caps = pad.getCurrentCaps();
        logger.atInfo().log("Received decoded stream with caps : " + caps.toString());
        if (caps.isAlwaysCompatible(Caps.fromString("video/x-raw"))) {
            Element q = ElementFactory.make("queue", "videoqueue");
            Element conv = ElementFactory.make("videoconvert", "videoconvert");
            Element sink = ElementFactory.make("autovideosink", "videosink");
            pipe.addMany(q, conv, sink);
            q.syncStateWithParent();
            conv.syncStateWithParent();
            sink.syncStateWithParent();
            pad.link(q.getStaticPad("sink"));
            q.link(conv);
            conv.link(sink);
        } else if (caps.isAlwaysCompatible(Caps.fromString("audio/x-raw"))) {
            Element q = ElementFactory.make("queue", "audioqueue");
            Element conv = ElementFactory.make("audioconvert", "audioconvert");
            Element resample = ElementFactory.make("audioresample", "audioresample");
            Element sink = ElementFactory.make("autoaudiosink", "audiosink");
            pipe.addMany(q, conv, resample, sink);
            q.syncStateWithParent();
            conv.syncStateWithParent();
            resample.syncStateWithParent();
            sink.syncStateWithParent();
            pad.link(q.getStaticPad("sink"));
            q.link(conv);
            conv.link(resample);
            resample.link(sink);
        }
    };

}
