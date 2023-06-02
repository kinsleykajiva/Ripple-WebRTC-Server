package africa.jopen.gstreamer;

public class GStreamerUtils {

    /**
     * max-size-buffers  set to 1000, which determines the maximum number of buffers that can be held in the queue.
     * The buffering element helps in smoothing out the stream by allowing a certain number of buffers to be accumulated before passing them downstream
     * */
    public static final String PIPELINE_DESCRIPTION= """
           filesrc location="C:\\\\Users\\\\Kinsl\\\\Videos\\\\target.mp4" ! decodebin name=decoder
                      
           decoder. ! videoconvert ! queue2 max-size-buffers=1000 ! vp8enc deadline=1 ! rtpvp8pay ! queue ! application/x-rtp,media=video,encoding-name=VP8,payload=97 ! webrtcbin.
                      
           decoder. ! audioconvert ! audioresample ! queue2 max-size-buffers=1000 ! opusenc ! rtpopuspay ! queue ! application/x-rtp,media=audio,encoding-name=OPUS,payload=96 ! webrtcbin.
                      
           webrtcbin name=webrtcbin bundle-policy=max-bundle stun-server=stun://stun.l.google.com:19302
                    
            """; // the target file is currently as test option but you can replace with your path but this is till neeeds to be stable still


}
