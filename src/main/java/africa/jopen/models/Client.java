package africa.jopen.models;

import africa.jopen.http.IceCandidate;
import africa.jopen.utils.FeatureTypes;
import africa.jopen.utils.WebRTCSendRecv;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import dev.onvoid.webrtc.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.*;
import java.util.concurrent.CompletableFuture;


public final class Client implements PeerConnectionObserver {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	final SetSessionDescriptionObserver localObserver = new SetSessionDescriptionObserver() {
		public void onSuccess() {
		}
		
		public void onFailure(String s) {
		}
	};

	private WebRTCSendRecv webRTCSendRecv;
	private final String clientID = XUtils.IdGenerator();
	private final Vector<String> messages = new Vector<>();
	private final Recorder recorder = new Recorder();
	private final RTCPeerConnection peerConnection;
	private  Session socketSession;
	private String clientAgentName;// this si the display name that will be used
	private FeatureTypes featureType;
	private long lastTimeStamp = System.currentTimeMillis();
	private Integer trackCounter = 0;
	private RTCModel rtcModel = new RTCModel();
	private Map<String, Object> candidateMap = new HashMap<>();
	private MutableList<Map<String, Object>> candidatesMapList = Lists.mutable.empty();
	private VideCallNotification videCallNotification;
	
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


	public WebRTCSendRecv getWebRTCSendRecv() {
		return webRTCSendRecv;
	}

	public void setWebRTCSendRecv() {
		peerConnection.close();
		setSocketSession(null);// not-really necessary to set to null really , just doing it just incase to save memory in the mean time
		this.webRTCSendRecv = new WebRTCSendRecv(clientID);
	}

	public void addIceCandidate(IceCandidate candidate) {
		
		RTCIceCandidate rtcCandidate = new RTCIceCandidate(candidate.sdpMid(), candidate.sdpMidLineIndex(), candidate.candidate());
		
		peerConnection.addIceCandidate(rtcCandidate);
	}
	
	public String processSdpOfferAsRemoteDescription() {
		logger.atInfo().log("ProcesSdpOfferAsRemoteDescription");
		RTCSessionDescription description = new RTCSessionDescription(RTCSdpType.OFFER, rtcModel.offer());
		
		CompletableFuture<String> future = new CompletableFuture<>();
		
		final var sdpObserver = new CreateSessionDescriptionObserver() {
			@Override
			public void onSuccess(RTCSessionDescription rtcSessionDescription) {
				peerConnection.setLocalDescription(rtcSessionDescription, localObserver);
				// now we are responding to this cleint with the remote description
				rtcModel.setAnswer(rtcSessionDescription.sdp);
				future.complete(rtcSessionDescription.sdp);
			}
			
			@Override
			public void onFailure(String s) {
				future.complete(null);
			}
		};
		
		final var remoteSDP = new SetSessionDescriptionObserver() {
			@Override
			public void onSuccess() {
				final RTCAnswerOptions answerOptions = new RTCAnswerOptions();
				answerOptions.voiceActivityDetection = false;
				peerConnection.createAnswer(answerOptions, sdpObserver);
			}
			
			@Override
			public void onFailure(String s) {
			
			}
		};
		peerConnection.setRemoteDescription(description, remoteSDP);
		try {
			// Wait for the CompletableFuture to complete (or complete exceptionally)
			return future.get();
		} catch (Exception e) {
			
			logger.atInfo().withCause(e).log("Error");
		}
		
		return "";
	}
	
	public FeatureTypes getFeatureType() {
		return featureType;
	}
	
	public void setFeatureType(FeatureTypes featureType) {
		this.featureType = featureType;
	}
	
	public RTCPeerConnection getPeerConnection() {
		return peerConnection;
	}
	
	public String getClientID() {
		return clientID;
		
	}
	
	public Session getSocketSession() {
		return socketSession;
	}
	
	public void setSocketSession(Session socketSession) {
		this.socketSession = socketSession;
	}
	
	public VideCallNotification getVideCallNotification() {
		return videCallNotification;
	}
	
	public void setVideCallNotification(VideCallNotification videCallNotification) {
		this.videCallNotification = videCallNotification;
	}
	
	public long lastTimeStamp() {
		return lastTimeStamp;
	}
	
	public void updateLastTimeStamp(long newTime) {
		lastTimeStamp = newTime;
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
	
	public RTCModel getRtcModel() {
		return rtcModel;
	}
	
	public void setRtcModel(RTCModel rtcModel) {
		this.rtcModel = rtcModel;
	}
	
	public String getClientAgentName() {
		return clientAgentName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Client) obj;
		return Objects.equals(this.clientID, that.clientID) &&
				this.lastTimeStamp == that.lastTimeStamp &&
				Objects.equals(this.messages, that.messages) &&
				Objects.equals(this.recorder, that.recorder) &&
				Objects.equals(this.trackCounter, that.trackCounter);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(clientID, lastTimeStamp, messages, recorder, trackCounter);
	}
	
	@Override
	public String toString() {
		return "Client[" +
				"clientId=" + clientID + ", " +
				"lastTimeStamp=" + lastTimeStamp + ", " +
				"messages=" + messages + ", " +
				"recorder=" + recorder + ", " +
				"trackCounter=" + trackCounter + ']';
	}
	
	@Override
	public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {
		if (rtcIceCandidate != null) {
			
			candidateMap.put("sdpMid", rtcIceCandidate.sdpMid);
			candidateMap.put("sdpMLineIndex", rtcIceCandidate.sdpMLineIndex);
			candidateMap.put("candidate", rtcIceCandidate.sdp);
			candidatesMapList.add(candidateMap);
			// When using Htt or Rest API access the best way to send this data is via the reminder request, the next time the client checks in then we send the data along is as an array.
			
		}
	}

	public MutableList<Map<String, Object>> getCandidatesMapList() {
		return candidatesMapList;
	}

	public void setCandidatesMapList(MutableList<Map<String, Object>> candidatesMapList) {
		this.candidatesMapList = candidatesMapList;
	}


	public Map<String, Object> getCandidateMap() {
		return candidateMap;
	}
	public void  resetCandidateMap() {
		 candidateMap.clear();
	}
	public void setCandidateMap(Map<String,Object> candits) {
		// ToDo check if this will require a review  during multi-threading case
		 this.candidateMap=candits;
	}



}