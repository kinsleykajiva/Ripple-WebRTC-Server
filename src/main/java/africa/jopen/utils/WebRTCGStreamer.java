package africa.jopen.utils;

import africa.jopen.models.Client;
import com.google.common.flogger.FluentLogger;
import org.freedesktop.gstreamer.*;

import org.freedesktop.gstreamer.elements.DecodeBin;
import org.freedesktop.gstreamer.message.MessageType;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;
import org.freedesktop.gstreamer.webrtc.WebRTCSDPType;
import org.freedesktop.gstreamer.webrtc.WebRTCSessionDescription;
import org.json.JSONObject;

import java.util.*;


/**
 *
 */
public class WebRTCGStreamer {

    ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private Pipeline pipe;
    private WebRTCBin webRTCBin;
    private String clientID;
    private boolean isPaused =false;
    private static final int SECONDS_PER_MINUTE = 60;
    private int minutes =0;
    private int seconds =0;
    private float currentVolume = 1.0f; // Initial volume level

    /**
     * max-size-buffers  set to 1000, which determines the maximum number of buffers that can be held in the queue.
     * The buffering element helps in smoothing out the stream by allowing a certain number of buffers to be accumulated before passing them downstream
     */

    private final String PIPELINE_DESCRIPTION
            = """
            filesrc location="C:\\\\Users\\\\Kinsl\\\\Downloads\\\\Shatta Wale - Real Monster (SM Session _ EP 01).mp4" ! decodebin name=decoder
                       
            decoder. ! videoconvert ! queue2 max-size-buffers=1000 ! vp8enc deadline=1 ! rtpvp8pay ! queue ! application/x-rtp,media=video,encoding-name=VP8,payload=97 ! webrtcbin.
                       
            decoder. ! audioconvert ! audioresample ! queue2 max-size-buffers=1000 ! opusenc ! rtpopuspay ! queue ! application/x-rtp,media=audio,encoding-name=OPUS,payload=96 ! webrtcbin.
                       
            webrtcbin name=webrtcbin bundle-policy=max-bundle stun-server=stun://stun.l.google.com:19302
                     
             """; /// the target file is currently as test option but you can replace with your path but this is still needs to be stable still


    private String pipeLineMaker() {
        var clientObject = connectionsManager.getClient(clientID);
        String p = clientObject.get().getgStreamMediaResource().getPath();
        p = p.replaceAll("\\\\", "\\\\\\\\");

        return "filesrc location=" + p + " ! decodebin name=decoder\n" +
               "\n" +
               "decoder. ! videoconvert ! queue2 max-size-buffers=1000 ! vp8enc deadline=1 ! rtpvp8pay ! queue ! application/x-rtp,media=video,encoding-name=VP8,payload=97 ! webrtcbin.\n" +
               "\n" +
               "decoder. ! audioconvert ! audioresample ! audioamplify amplification=2.0 ! queue2 max-size-buffers=1000 ! opusenc ! rtpopuspay ! queue ! application/x-rtp,media=audio,encoding-name=OPUS,payload=96 ! webrtcbin.\n" +
               "\n" +
               "webrtcbin name=webrtcbin bundle-policy=max-bundle stun-server=stun://stun.l.google.com:19302\n" +
               "\n";
    }

    // Increase the volume step-by-step
    public void increaseVolume() {
        if (currentVolume < 2.0f) {
            currentVolume += 0.1f;
            adjustVolume();
        }
    }
    // Decrease the volume step-by-step
    public void decreaseVolume() {
        if (currentVolume > 0.1f) {
            currentVolume -= 0.1f;
            adjustVolume();
        }
    }

    public void startClock() {
        var clientObject = connectionsManager.getClient(clientID);
        assert clientObject.isPresent();
        final var maxCountSeconds = clientObject.get().getgStreamMediaResource().getDuration();
        final long[] countSeconds = {0};
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (countSeconds[0] == maxCountSeconds) {
                    timer.cancel();
                    countSeconds[0] = 0;
                    return;
                }
                if (!isPaused) {
                    seconds++;
                    countSeconds[0]++;

                    if (seconds == SECONDS_PER_MINUTE) {
                        seconds = 0;
                        minutes++;
                    }

                    String formattedTime = String.format("%02d:%02d", minutes, seconds);
                    System.out.println(formattedTime);

                    if (minutes == 1 && seconds == 1) {
                        System.out.println("01:01");
                    }
                    // emmit this to the client
                }
            }
        }, 0, 1_000); // Update every second
    }

    // Helper method to adjust the volume dynamically.
    private void adjustVolume() {
        // ToDo add limit or max level
        Element volumeElement = pipe.getElementByName("audioamplify");
        if (volumeElement != null) {
            volumeElement.set("amplification", currentVolume);
        }
    }



    public WebRTCGStreamer(String clientID) {
        this.clientID = clientID;
        pipe = (Pipeline) Gst.parseLaunch(pipeLineMaker());
        webRTCBin = (WebRTCBin) pipe.getElementByName("webrtcbin");
        XUtils.MAIN_CONFIG_MODEL.nat().stuns().stream().findFirst().ifPresent(server-> webRTCBin.setStunServer(server));
        setupPipeLogging(pipe);
        WebRTCBin.ON_NEGOTIATION_NEEDED onNegotiationNeeded = elem -> webRTCBin.createOffer(onOfferCreated);
        webRTCBin.connect(onNegotiationNeeded);
        WebRTCBin.ON_ICE_CANDIDATE onIceCandidate = (sdpMLineIndex, candidate) -> {
            var ice = new JSONObject().put("candidate", candidate).put("sdpMLineIndex", sdpMLineIndex);
            String json = new JSONObject().put("ice", ice).toString();
            logger.atInfo().log("ON_ICE_CANDIDATE: ");
            Map<String, Object> candidateMap = new HashMap<>();
            candidateMap.put("sdpMLineIndex", sdpMLineIndex);
            candidateMap.put("candidate", candidate);
            //System.out.println("xxxxxxxxxxxxxxxcccccccccccccc ice my ice  " + ice);
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
          //  logger.atInfo().log("Receiving stream! Element : " + element.getName()                    + " Pad : " + pad.getName());
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

    }

    public void startCall() {
        if (!isPaused) {
        if (!pipe.isPlaying()) {
            logger.atInfo().log("initiating streams");
            pipe.play();
            startClock();
         //   System.out.println("xxxxxxxxxxxxxxxcccccccccccccc play");
        }
        }
    }
    public void pauseTransmission() {
        if (!isPaused) {
            isPaused = true;
            pipe.setState(State.PAUSED);
        }
    }

    public void resumeTransmission() {
        if (isPaused) {
            isPaused = false;
            pipe.play();
        }
    }

    private void endCall() {
        logger.atInfo().log("ending call");
        pipe.setState(isPaused ? State.PAUSED : State.NULL);
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
          //  System.out.println("xxxxxxxxxxxxxxxcccccccccccccc handleSdp " + sdpStr);

        } catch (Exception exception) {
            logger.atSevere().withCause(exception).log(exception.getLocalizedMessage());
        }
    }

    public void handleIceSdp(String candidate, int sdpMLineIndex) {
        try {
          //  System.out.println("xxxxxxxxxxxxxxxcccccccccccccc ice  " + candidate);
            logger.atInfo().log("Adding remote client ICE candidate : " );
            webRTCBin.addIceCandidate(sdpMLineIndex, candidate);
        } catch (Exception exception) {
            logger.atSevere().withCause(exception).log(exception.getLocalizedMessage());
        }
    }




    private void setupPipeLogging(Pipeline pipe) {

        Bus bus = pipe.getBus();
        bus.connect((Bus.EOS) source -> {
          //  logger.atInfo().log("Reached end of stream : " + source.toString());
            endCall();
        });

        bus.connect((Bus.ERROR) (source, code, message) -> {
           // logger.atInfo().log("Error from source : " + source                 + ", with code : " + code + ", and message : " + message);
            endCall();
        });

        bus.connect((source, old, current, pending) -> {
            if (source instanceof Pipeline) {
                logger.atInfo().log("Pipe state changed from " + old + " to " + current);
            }
        });

        bus.connect((Bus.MESSAGE) (element, message) -> {
           //Todo not really working needs to be updated to work as much , no idea to fix this yet
            if (message.getType() == MessageType.ELEMENT) {
                Structure structure = message.getStructure();

                // Check if the structure is named "progress".
                if ("progress".equals(structure.getName())) {
                    // Get the "position" and "duration" values from the structure.
                    int position = structure.getInteger("position");
                    int duration = structure.getInteger("duration");

                    // Calculate the current position in minutes and seconds.
                    int currentPositionMinutes = position / 60;
                    int currentPositionSeconds = position % 60;

                    // Calculate the total duration in minutes and seconds.
                    int totalDurationMinutes = duration / 60;
                    int totalDurationSeconds = duration % 60;

                    // Format the progress value as a string.
                    String progress = String.format("%d:%02d/%d:%02d", currentPositionMinutes, currentPositionSeconds, totalDurationMinutes, totalDurationSeconds);

                    // Print the progress value to the console.
                    System.out.println("Progress: " + progress);
                }
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
       // System.out.println("xxxxxxxxxxxxxxxcccccccccccccc sdp json " + json);
        logger.atInfo().log("Sending answer:\n");
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
