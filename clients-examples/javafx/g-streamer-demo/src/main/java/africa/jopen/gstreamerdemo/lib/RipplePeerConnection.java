package africa.jopen.gstreamerdemo.lib;

import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaStream;

public class RipplePeerConnection implements PeerConnectionObserver {
	@Override
	public void onConnectionChange(RTCPeerConnectionState state) {
		PeerConnectionObserver.super.onConnectionChange(state);
	}
	
	@Override
	public void onIceGatheringChange(RTCIceGatheringState state) {
		PeerConnectionObserver.super.onIceGatheringChange(state);
	}
	
	@Override
	public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {
	
	}
	
	@Override
	public void onAddStream(MediaStream stream) {
		PeerConnectionObserver.super.onAddStream(stream);
	}
	
	@Override
	public void onTrack(RTCRtpTransceiver transceiver) {
		PeerConnectionObserver.super.onTrack(transceiver);
	}
}
