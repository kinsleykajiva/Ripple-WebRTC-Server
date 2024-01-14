package africa.jopen.gstreamerdemo.lib;

import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import org.jetbrains.annotations.ApiStatus;
import org.json.JSONObject;

import java.util.logging.Logger;

@ApiStatus.NonExtendable
public class RipplePeerConnection implements PeerConnectionObserver {
	private PluginCallbacks.WebRTCPeerEvents webRTCPeerEvents;
	private int                              threadRef;
	private PeerConnectionFactory            peerConnectionFactory;
	private RTCPeerConnection                peerConnection;
	
	public static RTCIceServer getIceServers() {
		RTCIceServer stunServer = new RTCIceServer();
		stunServer.urls.add("stun:stunserver.org:3478");
		stunServer.urls.add("stun:webrtc.encmed.cn:5349");
		return stunServer;
	}
	
	public static RTCConfiguration getRTCConfig() {
		
		RTCConfiguration config = new RTCConfiguration();
		config.iceServers.add(getIceServers());
		
		return config;
	}
	
	public RipplePeerConnection(int threadRef, PluginCallbacks.WebRTCPeerEvents webRTCPeerEvents) {
		this.threadRef = threadRef;
		this.webRTCPeerEvents = webRTCPeerEvents;
		peerConnectionFactory = new PeerConnectionFactory();
		peerConnection = peerConnectionFactory.createPeerConnection(getRTCConfig(), this);
	}
	
	public void createAnswer() {
		log.info("createAnswer " + threadRef);
		
	}
	
	static Logger log = Logger.getLogger(RipplePeerConnection.class.getName());
	
	@Override
	public void onConnectionChange(RTCPeerConnectionState state) {
		PeerConnectionObserver.super.onConnectionChange(state);
	}
	
	@Override
	public void onIceGatheringChange(RTCIceGatheringState state) {
		PeerConnectionObserver.super.onIceGatheringChange(state);
		if (state == null) {
			return;
		}
		
		if (state == RTCIceGatheringState.COMPLETE) {
			log.info("ICE gathering completed");
		}
	}
	
	@Override
	public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {
		if (rtcIceCandidate == null) {
			log.info("End of candidates");
			return;
		}
		JSONObject message = new JSONObject();
		message.put("sdpMid", rtcIceCandidate.sdpMid);
		message.put("sdpMLineIndex", rtcIceCandidate.sdpMLineIndex);
		message.put("sdp", rtcIceCandidate.sdp);
		message.put("threadRef", threadRef);
		webRTCPeerEvents.IceCandidate(message.toString());
	}
	
	@Override
	public void onAddStream(MediaStream stream) {
		PeerConnectionObserver.super.onAddStream(stream);
	}
	
	@Override
	public void onTrack(RTCRtpTransceiver transceiver) {
		PeerConnectionObserver.super.onTrack(transceiver);
		if (transceiver == null) {
			return;
		}
		MediaStreamTrack track = transceiver.getReceiver().getTrack();
		if (track == null) {
			return;
		}
		webRTCPeerEvents.onTrack(track, threadRef);
		
		
	}
}
