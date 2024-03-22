package africa.jopen.ripple.plugins;

import africa.jopen.ripple.abstractions.PluginAbs;
import africa.jopen.ripple.interfaces.CommonAbout;
import africa.jopen.ripple.interfaces.Events;
import africa.jopen.ripple.utils.XUtils;
import io.micrometer.common.lang.Nullable;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.mjsip.media.FlowSpec;
import org.mjsip.media.MediaDesc;
import org.mjsip.media.StreamerOptions;
import org.mjsip.media.rx.AudioReceiver;
import org.mjsip.media.rx.JavaxAudioOutput;
import org.mjsip.media.tx.AudioTransmitter;
import org.mjsip.media.tx.JavaxAudioInput;
import org.mjsip.pool.PortConfig;
import org.mjsip.pool.PortPool;
import org.mjsip.sdp.MediaDescriptor;
import org.mjsip.sdp.SdpMessage;
import org.mjsip.sip.address.NameAddress;
import org.mjsip.sip.address.SipURI;
import org.mjsip.sip.call.DTMFInfo;
import org.mjsip.sip.provider.SipConfig;
import org.mjsip.sip.provider.SipProvider;
import org.mjsip.sip.provider.SipStack;
import org.mjsip.time.ConfiguredScheduler;
import org.mjsip.time.SchedulerConfig;
import org.mjsip.ua.*;
import org.mjsip.ua.clip.ClipPlayer;
import org.mjsip.ua.streamer.DefaultStreamerFactory;
import org.mjsip.ua.streamer.DispatchingStreamerFactory;
import org.mjsip.ua.streamer.NativeStreamerFactory;
import org.mjsip.ua.streamer.StreamerFactory;

import java.time.Instant;
import java.util.List;

import static africa.jopen.ripple.app.Main.IP_ADDRESS;
import static africa.jopen.ripple.utils.SDPUtils.*;

public class SipUserAgentPlugin extends PluginAbs implements UserAgentListener {
	protected              RegisteringUserAgent ua;
	protected              SipProvider          sip_provider;
	protected              UAConfig             uaConfig;
	static                 Logger               log              = Logger.getLogger(SipUserAgentPlugin.class.getName());
	private                UIConfig        uiConfig;
	private                StreamerFactory streamFactory;
	private                MediaOptions    mediaConfig;
	/**
	 * UA_IDLE=0
	 */
	protected static final String               UA_IDLE          = "IDLE";
	/**
	 * UA_INCOMING_CALL=1
	 */
	protected static final String               UA_INCOMING_CALL = "INCOMING_CALL";
	/**
	 * UA_OUTGOING_CALL=2
	 */
	protected static final String               UA_OUTGOING_CALL = "OUTGOING_CALL";
	/**
	 * UA_ONCALL=3
	 */
	protected static final String               UA_ONCALL        = "ONCALL";
	String call_state = UA_IDLE;
	private String      transaction = "";
	private CommonAbout commonAbout;
	//	protected RegisteringUserAgent ua;
	private Integer     thisObjectPositionAddress;
	
	public SipUserAgentPlugin( CommonAbout commonAbout, final Integer thisObjectPositionAddress,
	                           String realm, String username, String displayName, String password, String host, int port ) {
		setTransaction("0");
		System.out.println("xxxxx SipUserAgentPlugin constructor realm -  " + realm);
		System.out.println("xxxxx SipUserAgentPlugin constructor username -  " + username);
		System.out.println("xxxxx SipUserAgentPlugin constructor displayName -  " + displayName);
		System.out.println("xxxxx SipUserAgentPlugin constructor host -  " + host);
		System.out.println("xxxxx SipUserAgentPlugin constructor port -  " + port);
		System.out.println("xxxxx SipUserAgentPlugin constructor password -  " + password);
		this.commonAbout = commonAbout;
		this.thisObjectPositionAddress = thisObjectPositionAddress;
		SipConfig sipConfig = new SipConfig();
		sipConfig.setHostPort(port);
		UAConfig uaConfig = new UAConfig();
		uaConfig.setAuthRealm(realm);
		uaConfig.setUser(username);
		uaConfig.setDisplayName(displayName);
		sipConfig.setViaAddr(host);
		uaConfig.setAuthPasswd(password);
		uaConfig.setRegistrar(new SipURI(host, port)); // Set the SIP server address
		SchedulerConfig schedulerConfig = new SchedulerConfig();
		MediaConfig     mediaConfig     = new MediaConfig();
		PortConfig      portConfig      = new PortConfig();
		UIConfig        uiConfig        = new UIConfig();
		sipConfig.normalize();
		uaConfig.normalize(sipConfig);
		ConfiguredScheduler configuredScheduler = new ConfiguredScheduler(schedulerConfig);
		var                 sipPr               = new SipProvider(sipConfig, configuredScheduler);
//		uaConfig.setSendOnly(true);
		
		init(sipPr, portConfig.createPool(), uaConfig, uiConfig, mediaConfig);
	}
	private MediaAgent mediaAgent() {
		return new MediaAgent(mediaConfig.getMediaDescs(), streamerFactory);
	}
	private StreamerFactory streamerFactory;
	
	private void init( SipProvider sip_provider, PortPool portPool, UAConfig uaConfig, UIConfig uiConfig, MediaOptions mediaConfig ) {
		
		this.sip_provider = sip_provider;
		this.uaConfig = uaConfig;
		this.uiConfig = uiConfig;
		this.mediaConfig = mediaConfig;
		ua = new RegisteringUserAgent(sip_provider, portPool, this.uaConfig, this);
		//streamFactory = createStreamerFactory(this.mediaConfig, this.uaConfig);
		streamerFactory = createStreamerFactory(mediaConfig, uaConfig);
		changeStatus(UA_IDLE);
		log.info("Starting registration");
		log.info("Starting registration -- " + uaConfig.isRegister());
		if (uaConfig.isRegister()) {
			ua.loopRegister(uaConfig.getExpires(), uaConfig.getExpires() / 2, uaConfig.getKeepAliveTime());
			
			/*var rc = new RegistrationClient(sip_provider, uaConfig, new RegistrationLogger());
			if (uiConfig.doUnregisterAll) {
				rc.unregisterall();
			}
			
			if (uiConfig.doUnregister) {
				rc.unregister();
			}
			
			rc.register(uaConfig.getExpires());*/
			
		}
		
	}
	
	public void setTransaction( String transaction ) {
		this.transaction = transaction;
	}
	
	public String removeOpusCodec( String sdp ) {
		StringBuilder newSdp = new StringBuilder();
		String[]      lines  = sdp.split("\\r?\\n");
		
		for (String line : lines) {
			/*if(line.contains("a=recvonly")){
				line = line.replace("recvonly", "sendrecv");
			}*/
			if (line.contains("s=-")) {
				line = line.replace("s=-", "s=" + uaConfig.getAuthRealm() + " " + XUtils.IdGenerator());
				
			}
			if (line.contains("o=-")) {
				line = line.replace("o=-", "o=" + uaConfig.getUser());
			}
			
			
			if (line.contains("0.0.0.0")) { // Fix: replace the "0.0.0.0" Ip
				line = line.replace("0.0.0.0", IP_ADDRESS);
			}
			if (line.contains("127.0.0.1")) { // Fix: replace the "0.0.0.0" Ip
				line = line.replace("127.0.0.1", IP_ADDRESS);
			}
			/*if (line.contains("a=fmtp:63 111/111")) { // Fix: replace the fmtp line for "63" payload type. Placeholder fix, adjust according to your requirements
				line = "a=fmtp:63 appropriate_parameters_for_codecs";
			}*/
			/*if (line.contains("a=rtpmap:110 telephone-event/48000")) { // Fix: correct the frequency for telephone-event
				line = "a=rtpmap:110 telephone-event/8000";
			}*/
			if (!line.contains("opus") && !line.contains("red/48000")) {
				newSdp.append(line);
				newSdp.append("\n");
			}
		}
		/*newSdp.append("a=rtpmap:101 telephone-event/8000\n");
		newSdp.append("a=ptime:20\n");
		newSdp.append("a=maxptime:150\n");*/
		
		return newSdp.toString();
	}
	
	/*public void call(String target_uri) {
		ua.hangup();
		//ua.log("CALLING "+target_uri);
		System.out.println("calling "+target_uri);
		//if (!ua_profile.audio && !ua_profile.video) ua.log("ONLY SIGNALING, NO MEDIA");
		ua.call(target_uri,null);
		changeStatus(UA_OUTGOING_CALL);
	}*/
	public void makeOutGoingCall( @Nullable String sdp ) {
		
		var          nameAddress           = org.mjsip.sip.address.NameAddress.parse("sip:2710210@vafey.commonresolve.co.za:9099");
		String       ipAddressInOriginLine = extractIPAddressFromOriginLine(sdp) + "\n";
		String       sessionName           = extractSessionName(sdp) + "\n";
		var          extractOValues        = extractValuesFromOriginLine(sdp);
		String       fingerprintValue      = extractFingerprintValue(sdp) + "\n";
		List<String> rtpmapLines           = extractRtpmapLines(sdp);
		List<String> extmapLines           = extractExtmapLines(sdp);
		// Problem - m=audio 9 UDP/TLS/RTP/SAVPF 111 63 9 0 8 13 110 126
		String o = "- " + extractOValues.get(0) + " " + extractOValues.get(1) + " IN IP4 " + ipAddressInOriginLine + "\n";
		String c = "IN IP4 " + ipAddressInOriginLine + "\n";
		// Problem - m=audio 9 UDP/TLS/RTP/SAVPF 111 63 9 0 8 13 110 126
		//! not Proud of this solution!
		sdp = """
				v=0
				o=""" + o + """
				s=Asterisk PBX 13.38.1
				c=""" + c + """
				t=0 0
				a=group:BUNDLE 0
				a=extmap-allow-mixed
				a=msid-semantic: WMS 6e6fd708-1738-406a-bf65-1189b2f13f5c
				m=audio 18426 RTP/AVP 8 101
				a=rtcp:9 IN IP4 """ + ipAddressInOriginLine + """
				a=ice-ufrag:W77c
				a=ice-pwd:9VEM3zWAsygAj4umZwbAYA+U
				a=ice-options:trickle
				a=fingerprint:""" + fingerprintValue + """
				a=setup:actpass
				a=mid:0
				a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level
				a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
				a=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
				a=extmap:4 urn:ietf:params:rtp-hdrext:sdes:mid
				a=sendrecv
				a=msid:6e6fd708-1738-406a-bf65-1189b2f13f5c cae3ec4b-eaa0-48fe-815d-a0712f281a26
				a=rtcp-mux
				a=rtpmap:111 opus/48000/2
				a=rtcp-fb:111 transport-cc
				a=fmtp:111 minptime=10;useinbandfec=1
				a=rtpmap:63 red/48000/2
				a=rtpmap:8 PCMA/8000
				a=rtpmap:101 telephone-event/8000
				a=fmtp:63 111/111
				a=rtpmap:8 PCMA/8000
				a=rtpmap:110 telephone-event/48000
				a=rtpmap:126 telephone-event/8000
				a=ssrc:1272389523 cname:vkutGrWRxoZICk6X
				a=ssrc:1272389523 msid:6e6fd708-1738-406a-bf65-1189b2f13f5c cae3ec4b-eaa0-48fe-815d-a0712f281a26
				""";
//		SipStack.debug_level=8;
		//MediaDescriptor audioDesc = new MediaDescriptor(MediaType.AUDIO, 18426);
	//	var f=createStreamerFactory(this.mediaConfig, this.uaConfig);
	//	MediaAgent mediaAgent = new MediaAgent(null,f);
		
		//System.out.println("!!!==>\n" + sdp);
		SdpMessage sdpMess = new SdpMessage(sdp);
//		ua.call(nameAddress,sdpMess );
		ua.call(nameAddress,sdpMess ,mediaAgent());
		changeStatus(UA_OUTGOING_CALL);
//		ua.call(nameAddress, mediaAgent());
		//mediaAgent();
		SipStack        stack     = new SipStack();
		//RtpPacketSource rtpSource = new RtpPacketSource();
	}
	
	
	private UserAgentListener clipPlayer() {
		if (!mediaConfig.isUseRat() && !uiConfig.noSystemAudio) {
			return new ClipPlayer(uiConfig.mediaPath);
		}
		return null;
	}
	
	protected void changeStatus( String state ) {
		call_state = state;
		log.debug("state: " + call_state);
	}
	
	public StreamerFactory createStreamerFactory( MediaOptions mediaConfig, UAOptions uaConfig ) {
		DispatchingStreamerFactory factory = new DispatchingStreamerFactory();
		if (mediaConfig.isAudio()) {
			if (mediaConfig.isUseRat()) {
				factory.addFactory("audio", new NativeStreamerFactory(mediaConfig.getAudioMcastSoAddr(), mediaConfig.getBinRat()));
				
			} else {
				FlowSpec.Direction dir = uaConfig.getDirection();
				
				AudioTransmitter tx;
				if (dir.doSend()) {
					tx = new JavaxAudioInput(true, mediaConfig.isJavaxSoundDirectConversion());
				} else {
					tx = null;
				}
				
				AudioReceiver rx;
				if (dir.doReceive()) {
					rx = new JavaxAudioOutput(mediaConfig.isJavaxSoundDirectConversion());
				} else {
					rx = null;
				}
				
				// standard javax-based audio streamer
				StreamerOptions options = StreamerOptions.builder()
						.setRandomEarlyDrop(mediaConfig.getRandomEarlyDropRate())
						.setSymmetricRtp(mediaConfig.isSymmetricRtp())
						.build();
				
				factory.addFactory("audio", new DefaultStreamerFactory(options, rx, tx));
			}
			
		}
		if (mediaConfig.isVideo()) {
			if (mediaConfig.isUseVic()) {
				factory.addFactory("video", new NativeStreamerFactory(mediaConfig.getVideoMcastSoAddr(), mediaConfig.getBinVic()));
			}
		}
		return factory;
	}
	
	@Override
	public void onUaRegistrationSucceeded( UserAgent ua, String result ) {
		
		System.out.println("Registration succeeded: " + result);
		JSONObject response = new JSONObject();
		response.put("message", "Registration succeeded");
		response.put("registrationMessage", result);
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_REGISTRATION);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
		
	}
	
	@Override
	public void onUaRegistrationFailed( UserAgent ua, String result ) {
		System.out.println("Registration failed: " + result);
		JSONObject response = new JSONObject();
		response.put("message", "Registration Failed");
		response.put("success", false);
		response.put("registrationMessage", result);
		response.put(Events.EVENT_TYPE, Events.SIP_REGISTRATION);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaIncomingCall( UserAgent ua, NameAddress callee, NameAddress caller, MediaDesc[] media_descs ) {
		System.out.println("Incoming call from: " + caller.toString());
		changeStatus(UA_INCOMING_CALL);
		JSONObject response = new JSONObject();
		response.put("message", "Incoming call from");
		response.put("success", true);
		response.put("caller",
				new JSONObject()
						.put("displayName", caller.getDisplayName())
						.put("address", caller.getAddress())
		);
		response.put("callee",
				new JSONObject()
						.put("displayName", callee.getDisplayName())
						.put("address", callee.getAddress())
		);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_INCOMING);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
//		ua.acceptCall(200);
	}
	
	@Override
	public void onUaCallIncomingAccepted( UserAgent userAgent ) {
		System.out.println("Incoming call accepted");
		JSONObject response = new JSONObject();
		response.put("message", "Incoming call accepted");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_INCOMING_ACCEPTED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaIncomingCallTimeout( UserAgent userAgent ) {
		System.out.println("Incoming call timeout");
		JSONObject response = new JSONObject();
		response.put("message", "Incoming call timeout");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_INCOMING_TIME_OUT);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallCancelled( UserAgent ua ) {
		System.out.println("Call cancelled");
		JSONObject response = new JSONObject();
		response.put("message", "Call cancelled");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_CANCELLED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallConfirmed( UserAgent userAgent ) {
		System.out.println("Call confirmed");
		JSONObject response = new JSONObject();
		response.put("message", "Call confirmed");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_ON_CONFIRMED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallProgress( UserAgent ua ) {
		System.out.println("Call progress");
		
		
		JSONObject response = new JSONObject();
		response.put("message", "Call progress");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_PROGRESS);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallRinging( UserAgent ua ) {
		System.out.println("Ringing...");
		JSONObject response = new JSONObject();
		response.put("message", "Ringing");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_RINGING);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallAccepted( UserAgent ua ) {
		
		System.out.println("Call accepted");
		JSONObject response = new JSONObject();
		response.put("message", "Call accepted");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_ACCEPTED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallTransferred( UserAgent ua ) {
		System.out.println("Call transferred");
		JSONObject response = new JSONObject();
		response.put("message", "Call transferred");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_TRANSFERED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallFailed( UserAgent ua, String reason ) {
		System.out.println("Call failed: " + reason);
		JSONObject response = new JSONObject();
		response.put("message", "Call failed " + reason);
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_FAILED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallClosed( UserAgent ua ) {
		System.out.println("Call closed");
		JSONObject response = new JSONObject();
		response.put("message", "Call closed");
		response.put("success", true);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_ENDED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaCallRedirected( UserAgent userAgent, NameAddress redirect_to ) {
		System.out.println("Call redirected");
		JSONObject response = new JSONObject();
		response.put("message", "Call redirected");
		response.put("success", true);
		response.put("redirect", new JSONObject()
				.put("displayName",redirect_to.getDisplayName())
				.put("address",redirect_to.getAddress())
		);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_REDIRECT);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaMediaSessionStarted( UserAgent ua, String type, String codec ) {
		System.out.println("Media session started for " + type + "\n\n with codec " + codec);
		JSONObject response = new JSONObject();
		response.put("message", "Call Media session");
		response.put("success", true);
		response.put("codec", codec);
		response.put("type", type);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_MEDIA_STARTED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void onUaMediaSessionStopped( UserAgent ua, String type ) {
		System.out.println("Media session stopped");
		JSONObject response = new JSONObject();
		response.put("message", "Call session stopped");
		response.put("success", true);
		response.put("type", type);
		response.put(Events.EVENT_TYPE, Events.SIP_CALL_MEDIA_STOPPED);
		response.put("feature", FeatureTypes.SIP_GATEWAY.toString());
		notifyClient(response, this.thisObjectPositionAddress);
		
	}
	
	@Override
	public void onDtmfInfo( UserAgent ua, DTMFInfo dtmf ) {
		UserAgentListener.super.onDtmfInfo(ua, dtmf);
	}
	
	@Override
	public UserAgentListener andThen( UserAgentListener other ) {
		//return UserAgentListener.super.andThen(clipPlayer());
		return UserAgentListener.super.andThen(other);
	}
	
	@Override
	public void updateClientLastTimeStamp() {
		Instant now = Instant.now();
		commonAbout.onUpdateLastTimeStamp(now.toEpochMilli());
	}
	
	@Override
	protected void notifyClient( JSONObject pluginData, final Integer objectPosition ) {
		commonAbout.sendMessage(pluginData, objectPosition, FeatureTypes.SIP_GATEWAY);
	}
}
