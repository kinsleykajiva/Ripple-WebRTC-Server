package africa.jopen.gstreamer;


import africa.jopen.models.Client;
import africa.jopen.utils.ConnectionsManager;
import com.google.common.flogger.FluentLogger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static africa.jopen.gstreamer.GStreamerUtils.PIPELINE_DESCRIPTION;

/*
* This is a Model class to use with every Client that connects.
* This is set to replace the existing  {@link #africa.jopen.gstreamer.GStreamer GStreamer}
*
* */

public class GStreamer {
    private final ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
    private  final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Pipeline pipe;
    private final WebRTCBin webRTCBin;

    private Client client;// every this needs to be updated at every instance
    private final MutableList <CompletableFuture< String  >> completableWebRTCFutures = Lists.mutable.empty();


    public GStreamer( Client client){
        if(Objects.isNull(client)){
            throw new  RuntimeException("The client can not  be null");
        }
        this.client = client;
        pipe = (Pipeline) Gst.parseLaunch(PIPELINE_DESCRIPTION);
        webRTCBin = (WebRTCBin) pipe.getElementByName("webrtcbin");
        setupPipeLogging(pipe);
    }
    public void startWebRTCDescriptions(){
        CompletableFuture<String> resFuture1 = new CompletableFuture<>();
        logger.atInfo().log("starting the webrtc stuff ....");
        webRTCBin.connect((WebRTCBin.ON_NEGOTIATION_NEEDED) elem -> {
            CompletableFuture<String> resFuture = new CompletableFuture<>();
            webRTCBin.createOffer(offer->{
               webRTCBin.setLocalDescription(offer);
               final  String sdp = offer.getSDPMessage().toString();
                logger.atInfo().log("Sending offer as answer:\n" + sdp);
                client.getRtcModel().setOffer(sdp);
                connectionsManager.updateClient(client);
                resFuture.complete(sdp);
            });
            try {
                String inner = resFuture.get();
                resFuture1.complete(inner);
            } catch (InterruptedException | ExecutionException e) {
                logger.atSevere().withCause(e).log("Error No SDP created");
                resFuture1.complete(null);
            }
        });
        try {
            String sdp =resFuture1.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.atSevere().withCause(e).log("Error No SDP created Exceptions");
            //throw new RuntimeException(e);
        }
        //

        webRTCBin.connect((WebRTCBin.ON_ICE_CANDIDATE) (sdpMLineIndex, candidate)->{});
        webRTCBin.connect((Element.PAD_ADDED)  (element, pad) -> {});
    }

    private void endCall() {
        logger.atInfo().log("ending call");
        pipe.setState(State.NULL);
        //  Gst.quit();
    }
    public void startCall() {
        logger.atInfo().log("initiating call");
        pipe.play();
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

  /*  public void onEvent(@Observes ClientsEvents event) {
        if(Objects.nonNull(event)){
            client = event.getClient();
        }
    }
*/


}
