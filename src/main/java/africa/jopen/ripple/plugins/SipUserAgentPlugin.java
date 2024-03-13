package africa.jopen.ripple.plugins;

import africa.jopen.ripple.abstractions.PluginAbs;
import africa.jopen.ripple.interfaces.CommonAbout;
import africa.jopen.ripple.interfaces.Events;
import com.sun.net.httpserver.Request;
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
import org.mjsip.ua.registration.RegistrationClient;
import org.mjsip.ua.registration.RegistrationLogger;
import org.mjsip.ua.streamer.DefaultStreamerFactory;
import org.mjsip.ua.streamer.DispatchingStreamerFactory;
import org.mjsip.ua.streamer.NativeStreamerFactory;
import org.mjsip.ua.streamer.StreamerFactory;

import java.time.Instant;

public class SipUserAgentPlugin extends PluginAbs implements UserAgentListener {
	protected              RegisteringUserAgent ua;
	protected              SipProvider sip_provider;
	protected              UAConfig    uaConfig;
	static                 Logger          log              = Logger.getLogger(SipUserAgentPlugin.class.getName());
	private                UIConfig        uiConfig;
	private                StreamerFactory _streamerFactory;
	private                MediaOptions    mediaConfig;
	/**
	 * UA_IDLE=0
	 */
	protected static final String          UA_IDLE          = "IDLE";
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
	private String transaction = "";
	private  CommonAbout commonAbout;
	//	protected RegisteringUserAgent ua;
	private       Integer     thisObjectPositionAddress;
	public SipUserAgentPlugin(CommonAbout commonAbout, final Integer thisObjectPositionAddress,
	                          String realm,String username,String displayName,String password,String host,int port){
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
		UAConfig        uaConfig        = new UAConfig();
		uaConfig.setAuthRealm(realm);
		uaConfig.setUser(username);
		uaConfig.setDisplayName(displayName);
		sipConfig.setViaAddr(host);
		uaConfig.setAuthPasswd(password);
		uaConfig.setRegistrar(new SipURI(host,port)); // Set the SIP server address
		SchedulerConfig schedulerConfig = new SchedulerConfig();
		MediaConfig     mediaConfig     = new MediaConfig();
		PortConfig      portConfig      = new PortConfig();
		UIConfig        uiConfig        = new UIConfig();
		sipConfig.normalize();
		uaConfig.normalize(sipConfig);
		ConfiguredScheduler configuredScheduler = new ConfiguredScheduler(schedulerConfig);
		var                 sipPr               = new SipProvider(sipConfig,configuredScheduler);
//		uaConfig.setSendOnly(true);
		
		init(sipPr,portConfig.createPool(),uaConfig,uiConfig,mediaConfig);
	}
	private void init(  SipProvider sip_provider, PortPool portPool, UAConfig uaConfig, UIConfig uiConfig, MediaOptions mediaConfig ) {
		
		this.sip_provider = sip_provider;
		this.uaConfig = uaConfig;
		this.uiConfig = uiConfig;
		this.mediaConfig = mediaConfig;
		ua = new RegisteringUserAgent(sip_provider, portPool, this.uaConfig, this);
		_streamerFactory = createStreamerFactory(this.mediaConfig, this.uaConfig);
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
	
	public String removeOpusCodec(String sdp) {
    StringBuilder newSdp = new StringBuilder();
    String[] lines = sdp.split("\\r?\\n");
    
    for (String line : lines) {
        if (!line.contains("opus") && !line.contains("red/48000") && !line.contains("telephone-event")) {
            newSdp.append(line);
            newSdp.append("\n");
        }
    }
    
    return newSdp.toString();
}
	
	public void makeOutGoingCall( @Nullable String sdp ) {
		sdp = sdp.replace("a=rtpmap:111 opus/48000/2\\r\\n", "");
//		sdp = sdp.replaceAll("a=rtpmap:(\\d+) (G722|PCMU|CN)/8000\\r\\n", "");
		//sdp = sdp.replaceAll("a=rtpmap:(\\d+) CN/(16000|32000|48000|96000)\\r\\n", "");
		sdp = removeOpusCodec(sdp);
		System.out.println("==>"+sdp);
		var nameAddress = org.mjsip.sip.address.NameAddress.parse("sip:27118695168@vafey.commonresolve.co.za:9099");
//		IceCandidate j;
		SdpMessage sdpMess = new SdpMessage(sdp);
		//uaConfig.setAudioCodecs(new ArrayList<String>(Arrays.asList("PCMU/8000","PCMA/8000")));
		ua.call(nameAddress, sdpMess);
		
		changeStatus(UA_OUTGOING_CALL);
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
//		ua.acceptCall(200);
	}
	
	@Override
	public void onUaCallIncomingAccepted( UserAgent userAgent ) {
		System.out.println("Incoming call accepted");
	}
	
	@Override
	public void onUaIncomingCallTimeout( UserAgent userAgent ) {
		System.out.println("Incoming call timeout");
	}
	
	@Override
	public void onUaCallCancelled( UserAgent ua ) {
		System.out.println("Call cancelled");
	}
	
	@Override
	public void onUaCallConfirmed( UserAgent userAgent ) {
		System.out.println("Call confirmed");
	}
	
	@Override
	public void onUaCallProgress( UserAgent ua ) {
		System.out.println("Call progress");
	}
	
	@Override
	public void onUaCallRinging( UserAgent ua ) {
		System.out.println("Ringing...");
	}
	
	@Override
	public void onUaCallAccepted( UserAgent ua ) {
		
		System.out.println("Call accepted");
	}
	
	@Override
	public void onUaCallTransferred( UserAgent ua ) {
		System.out.println("Call transferred");
	}
	
	@Override
	public void onUaCallFailed( UserAgent ua, String reason ) {
		System.out.println("Call failed: " + reason);
	}
	
	@Override
	public void onUaCallClosed( UserAgent ua ) {
		System.out.println("Call closed");
	}
	
	@Override
	public void onUaCallRedirected( UserAgent userAgent, NameAddress redirect_to ) {
		System.out.println("Call redirected");
	}
	
	@Override
	public void onUaMediaSessionStarted( UserAgent ua, String type, String codec ) {
		System.out.println("Media session started");
	}
	
	@Override
	public void onUaMediaSessionStopped( UserAgent ua, String type ) {
		System.out.println("Media session stopped");
	}
	
	@Override
	public void onDtmfInfo( UserAgent ua, DTMFInfo dtmf ) {
		UserAgentListener.super.onDtmfInfo(ua, dtmf);
	}
	
	@Override
	public UserAgentListener andThen( UserAgentListener other ) {
		return UserAgentListener.super.andThen(clipPlayer());
//		return UserAgentListener.super.andThen(other);
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
