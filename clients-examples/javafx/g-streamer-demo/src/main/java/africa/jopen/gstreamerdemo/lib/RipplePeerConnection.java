package africa.jopen.gstreamerdemo.lib;

import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@ApiStatus.NonExtendable
public class RipplePeerConnection implements PeerConnectionObserver {
	private final        PluginCallbacks.WebRTCPeerEvents webRTCPeerEvents;
	private final        int                              threadRef;
	private final        RTCPeerConnection                peerConnection;
	private static final HashMap<Integer, String>         REMOTE_OFFER_STRING_SDP_MAP = new HashMap<>();
	
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
		PeerConnectionFactory peerConnectionFactory = new PeerConnectionFactory();
		peerConnection = peerConnectionFactory.createPeerConnection(getRTCConfig(), this);
	}
	
	public String getSdpMidFromCandidate(String candidate) {
		String sdpMid = null;
		String[] candidateParts = candidate.split(" ");
		
		for (int i = 0; i < candidateParts.length; i++) {
			if (candidateParts[i].equals("sdpMid")) {
				sdpMid = candidateParts[i + 1];
				break;
			}
		}
		
		return sdpMid;
	}
	
	public void addIceCandidatePeerConnection(String iceCandidates, int sdpMLineIndex) {
		String sdpMid    = getSdpMidFromCandidate(iceCandidates);
		String sdp       = iceCandidates;
		var    candidate = new RTCIceCandidate(sdpMid, sdpMLineIndex, sdp);
		peerConnection.addIceCandidate(candidate);
	}
	
	@Blocking
	public void consumeAnswer(RTCSessionDescription rtcSessionDescription) {
		CompletableFuture<Void> SetSessionDescriptionObserverFuture = new CompletableFuture<>();
		
		peerConnection.setRemoteDescription(rtcSessionDescription, new SetSessionDescriptionObserver() {
			@Override
			public void onSuccess() {
				log.info("setRemoteDescription onSuccess");
				SetSessionDescriptionObserverFuture.complete(null);
			}
			
			@Override
			public void onFailure(String error) {
				log.severe("setRemoteDescription onFailure " + error);
				SetSessionDescriptionObserverFuture.completeExceptionally(new RuntimeException("setRemoteDescription failed with error: " + error));
			}
		});
		SetSessionDescriptionObserverFuture.join();
	}
	
	@Blocking
	public void createOffer() {
		var offerOption = new RTCOfferOptions();
		offerOption.iceRestart = true;
		peerConnection.createOffer(offerOption, new CreateSessionDescriptionObserver() {
			@Override
			public void onSuccess(RTCSessionDescription description) {
				CompletableFuture<Void> SetSessionDescriptionObserverFuture = new CompletableFuture<>();
				peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
					@Override
					public void onSuccess() {
						log.info("setLocalDescription onSuccess");
						SetSessionDescriptionObserverFuture.complete(null);
					}
					
					@Override
					public void onFailure(String error) {
						log.severe("setLocalDescription onFailure " + error);
						SetSessionDescriptionObserverFuture.completeExceptionally(new RuntimeException("setLocalDescription failed with error: " + error));
					}
				});
				SetSessionDescriptionObserverFuture.thenAccept(result -> {
					JSONObject message = new JSONObject();
					message.put("requestType", "offer");
					message.put("threadRef", threadRef);
					message.put("offer", description.sdp);
					webRTCPeerEvents.notify(message.toString());
				});
			}
			
			@Override
			public void onFailure(String error) {
				log.severe("createOffer onFailure " + error);
			}
		});
	}
	
	public void createAnswer() {
		log.info("Creating answer for threadRef: " + threadRef);
		log.info("xxxxxCreating answer for threadRef: " + getRemoteOfferStringSdp(threadRef));
		RTCSessionDescription rtcSessionDescription = new RTCSessionDescription(RTCSdpType.OFFER, getRemoteOfferStringSdp(threadRef));
		
		CompletableFuture<Void>                  SessionDescriptionObserverFuture       = new CompletableFuture<>();
		CompletableFuture<RTCSessionDescription> CreateSessionDescriptionObserverFuture = new CompletableFuture<>();
		
		SetSessionDescriptionObserver setSessionDescriptionObserver = new SetSessionDescriptionObserver() {
			@Override
			public void onSuccess() {
				log.info("setRemoteDescription onSuccess");
				SessionDescriptionObserverFuture.complete(null);
			}
			
			@Override
			public void onFailure(String error) {
				SessionDescriptionObserverFuture.completeExceptionally(new RuntimeException("setRemoteDescription failed with error: " + error));
			}
		};
		peerConnection.setRemoteDescription(rtcSessionDescription, setSessionDescriptionObserver);
		
		SessionDescriptionObserverFuture.thenAccept(result -> {
			RTCAnswerOptions answerOptions = new RTCAnswerOptions();
			CreateSessionDescriptionObserver createSessionDescriptionObserver = new CreateSessionDescriptionObserver() {
				
				@Override
				public void onSuccess(RTCSessionDescription description) {
					log.info("xxxxxCreateSessionDescriptionObserverFuture onSuccess");
					CreateSessionDescriptionObserverFuture.complete(description);
				}
				
				@Override
				public void onFailure(String error) {
					CreateSessionDescriptionObserverFuture.completeExceptionally(new RuntimeException("createAnswer failed with error: " + error));
				}
			};
			peerConnection.createAnswer(answerOptions, createSessionDescriptionObserver);
		});
		
		CreateSessionDescriptionObserverFuture.thenAccept(answer -> {
			CompletableFuture<Void> SetSessionDescriptionObserverFuture = new CompletableFuture<>();
			peerConnection.setLocalDescription(answer, new SetSessionDescriptionObserver() {
				@Override
				public void onSuccess() {
					log.info("setLocalDescription onSuccess");
					SetSessionDescriptionObserverFuture.complete(null);
				}
				
				@Override
				public void onFailure(String error) {
					log.info("setLocalDescription onFailure");
					SetSessionDescriptionObserverFuture.completeExceptionally(new RuntimeException("setLocalDescription failed with error: " + error));
				}
			});
			SetSessionDescriptionObserverFuture.thenAccept(result -> {
				JSONObject message = new JSONObject();
				message.put("requestType", "answer");
				message.put("threadRef", threadRef);
				message.put("answer", answer.sdp);
				webRTCPeerEvents.notify(message.toString());
			});
		});
	}
	
	public static String getRemoteOfferStringSdp(int threadRef) {
		return REMOTE_OFFER_STRING_SDP_MAP.get(threadRef);
		
	}
	
	public static void setRemoteOfferStringSdp(int threadRef, String sdp) {
		REMOTE_OFFER_STRING_SDP_MAP.put(threadRef, sdp);
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
	public void onIceCandidate( RTCIceCandidate rtcIceCandidate ) {
		if (rtcIceCandidate == null) {
			log.info("End of candidates");
			return;
		}
		JSONObject message = new JSONObject();
		
		message.put("sdpMid", rtcIceCandidate.sdpMid);
		message.put("candidate", rtcIceCandidate.sdp);
		message.put("sdpMLineIndex", rtcIceCandidate.sdpMLineIndex);
		message.put("sdp", rtcIceCandidate.sdp);
		message.put("threadRef", threadRef);
		message.put("requestType", "iceCandidate");
		/*IceCandidateMessage iceCandidateMessage = new IceCandidateMessage();
		
		iceCandidateMessage.setSdp(rtcIceCandidate.sdp);
		iceCandidateMessage.setSdpMid(rtcIceCandidate.sdpMid);
		iceCandidateMessage.setSdpMLineIndex(rtcIceCandidate.sdpMLineIndex);
		iceCandidateMessage.*/
		log.info("Sending ice candidate: " + message.toString());
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
		
		// same email id to a pipe lines , associate tables.
		
	}
}
