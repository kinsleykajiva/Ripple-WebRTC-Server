package africa.jopen.models;

import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import dev.onvoid.webrtc.*;

import java.util.Objects;
import java.util.Vector;

public final class Client implements PeerConnectionObserver{
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final String clientId = XUtils.IdGenerator();
    private  String clientAgentName ;
    private  long lastTimeStamp= System.currentTimeMillis();
    private final Vector<String> messages=new Vector<>();
    private final Recorder recorder= new Recorder();
    private  Integer trackCounter=0;
    private final RTCPeerConnection peerConnection;
    private RTCModel rtcModel = new RTCModel();


    public Client(String clientAgentName) {
        this.clientAgentName = clientAgentName;
        RTCConfiguration rtcConfiguration = new RTCConfiguration();
        RTCIceServer stunServer = new RTCIceServer();
        stunServer.urls.add("stun:stun.l.google.com:19302");
        PeerConnectionFactory peerConnectionFactory = new PeerConnectionFactory();
        rtcConfiguration.iceServers.add(stunServer);
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, this);
        logger.atInfo().log("Creating peer connection");
    }
    public RTCPeerConnection getPeerConnection() {
        return peerConnection;
    }

    public String clientId() {
        return clientId;

    }

    public long lastTimeStamp() {
        return lastTimeStamp;
    }
 public void updateLastTimeStamp(long newTime) {
         lastTimeStamp=newTime;
    }

    public Vector<String> messages() {
        return messages;
    }

    public Recorder recorder() {
        return recorder;
    }

    public Integer trackCounter() {
        return trackCounter;
    }



    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Client) obj;
        return Objects.equals(this.clientId, that.clientId) &&
                this.lastTimeStamp == that.lastTimeStamp &&
                Objects.equals(this.messages, that.messages) &&
                Objects.equals(this.recorder, that.recorder) &&
                Objects.equals(this.trackCounter, that.trackCounter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, lastTimeStamp, messages, recorder, trackCounter);
    }

    @Override
    public String toString() {
        return "Client[" +
                "clientId=" + clientId + ", " +
                "lastTimeStamp=" + lastTimeStamp + ", " +
                "messages=" + messages + ", " +
                "recorder=" + recorder + ", " +
                "trackCounter=" + trackCounter + ']';
    }

    @Override
    public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {

    }
}