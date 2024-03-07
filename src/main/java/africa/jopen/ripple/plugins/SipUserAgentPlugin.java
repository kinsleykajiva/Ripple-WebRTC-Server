package africa.jopen.ripple.plugins;

import africa.jopen.ripple.sockets.WebsocketEndpoint;
import org.apache.log4j.Logger;
import org.mjsip.media.FlowSpec;
import org.mjsip.media.MediaDesc;
import org.mjsip.media.StreamerOptions;
import org.mjsip.media.rx.AudioReceiver;
import org.mjsip.media.rx.JavaxAudioOutput;
import org.mjsip.media.tx.AudioTransmitter;
import org.mjsip.media.tx.JavaxAudioInput;
import org.mjsip.pool.PortPool;
import org.mjsip.sip.address.NameAddress;
import org.mjsip.sip.address.SipURI;
import org.mjsip.sip.call.DTMFInfo;
import org.mjsip.sip.call.NotImplementedServer;
import org.mjsip.sip.call.OptionsServer;
import org.mjsip.sip.message.SipMethods;
import org.mjsip.sip.provider.SipId;
import org.mjsip.sip.provider.SipKeepAlive;
import org.mjsip.sip.provider.SipProvider;
import org.mjsip.ua.*;
import org.mjsip.ua.clip.ClipPlayer;
import org.mjsip.ua.registration.RegistrationClient;
import org.mjsip.ua.registration.RegistrationOptions;
import org.mjsip.ua.streamer.DefaultStreamerFactory;
import org.mjsip.ua.streamer.DispatchingStreamerFactory;
import org.mjsip.ua.streamer.NativeStreamerFactory;
import org.mjsip.ua.streamer.StreamerFactory;
import org.zoolu.net.SocketAddress;

public class SipUserAgentPlugin implements UserAgentListener {
	protected RegisteringUserAgent ua;
	protected SipProvider sip_provider;
	protected UAConfig _uaConfig;
	static    Logger   log = Logger.getLogger(SipUserAgentPlugin.class.getName());
	private UIConfig        _uiConfig;
	private StreamerFactory _streamerFactory;
	private MediaOptions    _mediaConfig;
	/** UA_IDLE=0 */
	protected static final String UA_IDLE="IDLE";
	/** UA_INCOMING_CALL=1 */
	protected static final String UA_INCOMING_CALL="INCOMING_CALL";
	/** UA_OUTGOING_CALL=2 */
	protected static final String UA_OUTGOING_CALL="OUTGOING_CALL";
	/** UA_ONCALL=3 */
	protected static final String UA_ONCALL="ONCALL";
	String call_state=UA_IDLE;
	
	
	public SipUserAgentPlugin(SipProvider sip_provider, PortPool portPool, UAConfig uaConfig, UIConfig uiConfig, MediaOptions mediaConfig) {
		this.sip_provider=sip_provider;
		_uaConfig=uaConfig;
		_uiConfig = uiConfig;
		_mediaConfig = mediaConfig;
//		ua=new RegisteringUserAgent(sip_provider, portPool,_uaConfig, this.andThen(clipPlayer()));
		ua=new RegisteringUserAgent(sip_provider, portPool,_uaConfig, this);
		_streamerFactory = createStreamerFactory(_mediaConfig, _uaConfig);
		changeStatus(UA_IDLE);
	}
	
	
	
	private UserAgentListener clipPlayer() {
		if (!_mediaConfig.isUseRat() && !_uiConfig.noSystemAudio) {
			return new ClipPlayer(_uiConfig.mediaPath);
		}
		return null;
	}
	protected void changeStatus(String state) {
		call_state=state;
		log.debug("state: "+call_state);
	}
	
	public StreamerFactory createStreamerFactory(MediaOptions mediaConfig, UAOptions uaConfig) {
		DispatchingStreamerFactory factory = new DispatchingStreamerFactory();
		if (mediaConfig.isAudio()) {
			if (mediaConfig.isUseRat()) {
				factory.addFactory("audio", new NativeStreamerFactory(mediaConfig.getAudioMcastSoAddr(), mediaConfig.getBinRat()));
				
			}else{
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
	
	}
	
	@Override
	public void onUaRegistrationFailed( UserAgent ua, String result ) {
	
	}
	
	@Override
	public void onUaIncomingCall( UserAgent ua, NameAddress callee, NameAddress caller, MediaDesc[] media_descs ) {
	
	}
	
	@Override
	public void onUaCallIncomingAccepted( UserAgent userAgent ) {
	
	}
	
	@Override
	public void onUaIncomingCallTimeout( UserAgent userAgent ) {
	
	}
	
	@Override
	public void onUaCallCancelled( UserAgent ua ) {
	
	}
	
	@Override
	public void onUaCallConfirmed( UserAgent userAgent ) {
	
	}
	
	@Override
	public void onUaCallProgress( UserAgent ua ) {
	
	}
	
	@Override
	public void onUaCallRinging( UserAgent ua ) {
	
	}
	
	@Override
	public void onUaCallAccepted( UserAgent ua ) {
	
	}
	
	@Override
	public void onUaCallTransferred( UserAgent ua ) {
	
	}
	
	@Override
	public void onUaCallFailed( UserAgent ua, String reason ) {
	
	}
	
	@Override
	public void onUaCallClosed( UserAgent ua ) {
	
	}
	
	@Override
	public void onUaCallRedirected( UserAgent userAgent, NameAddress redirect_to ) {
	
	}
	
	@Override
	public void onUaMediaSessionStarted( UserAgent ua, String type, String codec ) {
	
	}
	
	@Override
	public void onUaMediaSessionStopped( UserAgent ua, String type ) {
	
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
}
