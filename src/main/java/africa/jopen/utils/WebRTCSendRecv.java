package africa.jopen.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.flogger.FluentLogger;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.DecodeBin;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;
import org.freedesktop.gstreamer.webrtc.WebRTCSDPType;
import org.freedesktop.gstreamer.webrtc.WebRTCSessionDescription;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;

public class WebRTCSendRecv {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private Pipeline pipe;
    private WebRTCBin webRTCBin;
    private final String PIPELINE_DESCRIPTION
            = "videotestsrc is-live=true pattern=ball ! videoconvert ! queue ! vp8enc deadline=1 ! rtpvp8pay"
            + " ! queue ! application/x-rtp,media=video,encoding-name=VP8,payload=97 ! webrtcbin. "
            + "audiotestsrc is-live=true wave=sine ! audioconvert ! audioresample ! queue ! opusenc ! rtpopuspay"
            + " ! queue ! application/x-rtp,media=audio,encoding-name=OPUS,payload=96 ! webrtcbin. "
            + "webrtcbin name=webrtcbin bundle-policy=max-bundle stun-server=stun://stun.l.google.com:19302 ";

    public WebRTCSendRecv() {
        pipe = (Pipeline) Gst.parseLaunch(PIPELINE_DESCRIPTION);
        webRTCBin = (WebRTCBin) pipe.getElementByName("webrtcbin");
        setupPipeLogging(pipe);
        // When the pipeline goes to PLAYING, the on_negotiation_needed() callback
        // will be called, and we will ask webrtcbin to create an offer which will
        // match the pipeline above.
        webRTCBin.connect(onNegotiationNeeded);
        webRTCBin.connect(onIceCandidate);
        webRTCBin.connect(onIncomingStream);
    }

    private void startCall() {

    }

    private void endCall() {
        pipe.setState(State.NULL);
        // httpClient.close();
      //  Gst.quit();
    }

    private void handleSdp(String payload) {
        try {

            JSONObject answer = new JSONObject(payload);
            if (answer.has("sdp")) {
                String sdpStr = answer.getJSONObject("sdp").getString("sdp");
                logger.atInfo().log("Answer SDP:\n" + sdpStr);
                SDPMessage sdpMessage = new SDPMessage();
                sdpMessage.parseBuffer(sdpStr);
                WebRTCSessionDescription description = new WebRTCSessionDescription(WebRTCSDPType.ANSWER, sdpMessage);
                webRTCBin.setRemoteDescription(description);
            } else if (answer.has("ice")) {
                String candidate = answer.getJSONObject("ice").getString("candidate");
                int sdpMLineIndex = answer.getJSONObject("ice").getInt("sdpMLineIndex");
                logger.atInfo().log("Adding ICE candidate : " + candidate);
                webRTCBin.addIceCandidate(sdpMLineIndex, candidate);
            }
        } catch (Exception exception) {
            logger.atSevere().withCause(exception).log(exception.getLocalizedMessage());
        }
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

    private WebRTCBin.CREATE_OFFER onOfferCreated = offer -> {
        webRTCBin.setLocalDescription(offer);
        var sdp = new JSONObject();
        sdp.put("sdp", new JSONObject()
                .put("type", "offer")
                .put("sdp", offer.getSDPMessage().toString()));
        String json = sdp.toString();
        logger.atInfo().log("Sending offer:\n" + json);
        // websocket.sendTextFrame(json);
    };
    private final WebRTCBin.ON_NEGOTIATION_NEEDED onNegotiationNeeded = elem -> {
        logger.atInfo().log("onNegotiationNeeded: " + elem.getName());

        // When webrtcbin has created the offer, it will hit our callback and we
        // send SDP offer over the websocket to signalling server
        webRTCBin.createOffer(onOfferCreated);
    };
    private final WebRTCBin.ON_ICE_CANDIDATE onIceCandidate = (sdpMLineIndex, candidate) -> {
        var ice = new JSONObject().put("candidate", candidate).put("sdpMLineIndex", sdpMLineIndex);
        String json = new JSONObject().put("ice", ice).toString();
        logger.atInfo().log("ON_ICE_CANDIDATE: " + json);
        //  websocket.sendTextFrame(json);

    };
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
    private final Element.PAD_ADDED onIncomingStream = (element, pad) -> {
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

}
