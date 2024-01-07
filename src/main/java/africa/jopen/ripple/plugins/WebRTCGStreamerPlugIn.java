package africa.jopen.ripple.plugins;

import africa.jopen.ripple.abstractions.PluginAbs;
import africa.jopen.ripple.interfaces.CommonAbout;
import africa.jopen.ripple.interfaces.Events;
import africa.jopen.ripple.models.MediaFile;
import africa.jopen.ripple.models.RTCModel;
import africa.jopen.ripple.plugins.gstreamer.PipelineBuilder;
import africa.jopen.ripple.utils.ConnectionsManager;
import org.apache.log4j.Logger;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.DecodeBin;
import org.freedesktop.gstreamer.message.MessageType;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;
import org.freedesktop.gstreamer.webrtc.WebRTCSDPType;
import org.freedesktop.gstreamer.webrtc.WebRTCSessionDescription;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebRTCGStreamerPlugIn extends PluginAbs {
	static Logger log = Logger.getLogger(WebRTCGStreamerPlugIn.class.getName());
	ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
	private Pipeline  pipe;
	private WebRTCBin webRTCBin;
	private boolean   isPaused = false;
	
	private static final int SECONDS_PER_MINUTE = 60;
	
	private int minutes = 0, seconds = 0;
	private String transaction = "";
	
	private       float       currentVolume = 1.0f; // Initial volume level
	private final CommonAbout commonAbout;
	private       Integer     thisObjectPositionAddress;
	private final RTCModel    rtcModel      = new RTCModel();
	private final MediaFile   mediaFile;
	
	public String getTransaction() {
		return transaction;
	}
	
	public void setTransaction(String transaction) {
		this.transaction = transaction;
	}
	
	public WebRTCGStreamerPlugIn(CommonAbout commonAbout, final Integer thisObjectPositionAddress, MediaFile mediaFile) {
		setTransaction("0");
		this.commonAbout = commonAbout;
		this.mediaFile = mediaFile;
		this.thisObjectPositionAddress = thisObjectPositionAddress;
		pipe = (Pipeline) Gst.parseLaunch(pipeLineMaker(mediaFile.path()));
		
	}
	
	
	private void setupPipeLogging(Pipeline pipe) {
		
		Bus bus = pipe.getBus();
		bus.connect((Bus.EOS) source -> {
			log.info("Reached end of stream : " + source.toString());
			endCall();
		});
		
		bus.connect((Bus.ERROR) (source, code, message) -> {
			log.info("Error from source : " + source + ", with code : " + code + ", and message : " + message);
			endCall();
		});
		
		bus.connect((source, old, current, pending) -> {
			if (source instanceof Pipeline) {
				log.info("Pipe state changed from " + old + " to " + current);
			}
		});
		bus.connect((Bus.EOS) source -> {
			log.info("Reached end of stream : " + source.toString());
			endCall();
			
			
			JSONObject response = new JSONObject();
			response.put("feature", FeatureTypes.G_STREAM.toString());
			response.put(Events.EVENT_TYPE, Events.END_OF_STREAM_G_STREAM_EVENT);
			notifyClient(response, this.thisObjectPositionAddress);
		});
		
		bus.connect((Bus.MESSAGE) (element, message) -> {
			//Todo not really working needs to be updated to work as much , no idea to fix this yet. #startClock() is in use at the mean time
			
			if (message.getType() == MessageType.ELEMENT) {
				Structure structure = message.getStructure();
				
				// Check if the structure is named "progress".
				if ("progress".equals(structure.getName())) {
					// Get the "position" and "duration" values from the structure.
					int position = structure.getInteger("position");
					int duration = structure.getInteger("duration");
					
					// Calculate the current position in minutes and seconds.
					int currentPositionMinutes = position / 60;
					int currentPositionSeconds = position % 60;
					
					// Calculate the total duration in minutes and seconds.
					int totalDurationMinutes = duration / 60;
					int totalDurationSeconds = duration % 60;
					
					// Format the progress value as a string.
					String progress = String.format("%d:%02d/%d:%02d", currentPositionMinutes, currentPositionSeconds, totalDurationMinutes, totalDurationSeconds);
					
					// Print the progress value to the console.
					System.out.println("Progress: " + progress);
				}
			}
			
			if (message.getType() == MessageType.ELEMENT && "progress".equals(message.getStructure().getName())) {
				int position = message.getStructure().getInteger("position");
				int duration = message.getStructure().getInteger("duration");
				
				String progress = String.format("%d:%02d/%d:%02d", position / 60, position % 60, duration / 60, duration % 60);
				System.out.println("Pxrogress: " + progress);
			}
		});
		
	}
	
	private final WebRTCBin.CREATE_OFFER onOfferCreated = offer -> {
		webRTCBin.setLocalDescription(offer);
		String sdpp = offer.getSDPMessage().toString();
		var    sdp  = new JSONObject();
		sdp.put("sdp", new JSONObject()
				.put("type", "offer")
				.put("sdp", sdpp));
		String json = sdp.toString();
		log.info("Sending answer:\n");
		//Todo remove some of the code here is useless
		
		rtcModel.setOffer(sdpp);
		
		JSONObject response = new JSONObject();
		response.put("clientSDP", sdpp);
		response.put("sdp", json);
		response.put("feature", FeatureTypes.G_STREAM.toString());
		response.put(Events.EVENT_TYPE, Events.WEBRTC_EVENT);
		notifyClient(response, this.thisObjectPositionAddress);
		
		
	};
	
	private final Element.PAD_ADDED onDecodedStream = (element, pad) -> {
		if (!pad.hasCurrentCaps()) {
			log.info("Pad has no current Caps - ignoring");
			return;
		}
		Caps caps = pad.getCurrentCaps();
		log.info("Received decoded stream with caps : " + caps.toString());
		if (caps.isAlwaysCompatible(Caps.fromString("video/x-raw"))) {
			Element q    = ElementFactory.make("queue", "videoqueue");
			Element conv = ElementFactory.make("videoconvert", "videoconvert");
			Element sink = ElementFactory.make("autovideosink", "videosink");
			pipe.addMany(q, conv, sink);
			q.syncStateWithParent();
			conv.syncStateWithParent();
			sink.syncStateWithParent();
			pad.link(q.getStaticPad("sink"));
			q.link(conv);
			conv.link(sink);
		} else if (caps.isAlwaysCompatible(Caps.fromString("audio/x-raw"))) {
			Element q        = ElementFactory.make("queue", "audioqueue");
			Element conv     = ElementFactory.make("audioconvert", "audioconvert");
			Element resample = ElementFactory.make("audioresample", "audioresample");
			Element sink     = ElementFactory.make("autoaudiosink", "audiosink");
			pipe.addMany(q, conv, resample, sink);
			q.syncStateWithParent();
			conv.syncStateWithParent();
			resample.syncStateWithParent();
			sink.syncStateWithParent();
			pad.link(q.getStaticPad("sink"));
			q.link(conv);
			conv.link(resample);
			resample.link(sink);
		}
	};
	
	
	private String pipeLineMaker(String path) {
		//! ToDo needs review for other file system paths
		path = path.replaceAll("\\\\", "\\\\\\\\");
		
		return new PipelineBuilder()
				.addFileSource(path)
				.addVideoConvert()
				.addAudioConvert()
				.addWebRTCBin()
				.build();
	}
	
	public void startClock() {
		
		final boolean[]          isCompleted     = {true};
		
		Thread.startVirtualThread(() -> {
			while (isCompleted[0]) {
				long duration = pipe.queryDuration(TimeUnit.SECONDS);
				long position = pipe.queryPosition(TimeUnit.SECONDS);
				int progress = (int) ((position / (double) duration) * 100);
				
				long minutesMax = duration / 60;
				long secondsMax = duration % 60;
				
				String formattedTime = String.format("%02d:%02d", position / 60, position % 60);
				String maxFormattedTime = String.format("%02d:%02d", minutesMax, secondsMax);
				isCompleted[0] =  progress != 100;
				JSONObject response = new JSONObject();
				response.put("isCompleted", isCompleted[0]);
				response.put("progressInPercentage", progress);
				response.put("progressInSeconds", position);
				response.put("maxProgressInSeconds", duration);
				response.put("maxFormattedTime", maxFormattedTime);
				response.put("progressformattedTime", formattedTime);
				response.put("feature", FeatureTypes.G_STREAM.toString());
				response.put(Events.EVENT_TYPE, Events.PROGRESS_G_STREAM_EVENT);
				
				notifyClient(response, this.thisObjectPositionAddress);
				
				try {
					TimeUnit.MILLISECONDS.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
	}
	
	public void startClockxxx() {
		
		final var    duration     = pipe.queryDuration(TimeUnit.SECONDS);
		final long[] countSeconds = {0};
		Timer        timer        = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				
				long position = pipe.queryPosition(TimeUnit.SECONDS); // Get the current position of the media file
				System.out.println("xxxx " + position);
				int progress = (int) ((position / (double) duration) * 100); // Calculate the progress as a percentage
				if (countSeconds[0] == duration) {
					
					timer.cancel();
					countSeconds[0] = 0;
					return;
				}
				
				if (!isPaused) {
					seconds++;
					countSeconds[0]++;
					
					if (seconds == SECONDS_PER_MINUTE) {
						seconds = 0;
						minutes++;
					}
					
					long   minutesMax       = duration / 60;
					long   secondsMax       = duration % 60;
					String formattedTime    = String.format("%02d:%02d", minutes, seconds);
					String maxFormattedTime = String.format("%02d:%02d", minutesMax, secondsMax);
					System.out.println(formattedTime);
					System.out.println("Progress: " + progress + "%");
					
					if (minutes == 1 && seconds == 1) {
						System.out.println("01:01");
					}
					// Todo emmit this to the client
					JSONObject response = new JSONObject();
					response.put("progressInPercentage", progress);
					response.put("progressInSeconds", position);
					response.put("maxProgressInSeconds", duration);
					response.put("maxFormattedTime", maxFormattedTime);
					response.put("progressformattedTime", formattedTime);
					response.put("feature", FeatureTypes.G_STREAM.toString());
					response.put(Events.EVENT_TYPE, Events.PROGRESS_G_STREAM_EVENT);
					
					// Send the progress to the client
					notifyClient(response, thisObjectPositionAddress);
				}
			}
		}, 1_000 * 3, 1_000); // Update every second
	}
	
	
	// Increase the volume step-by-step
	public void increaseVolume() {
		if (currentVolume < 2.0f) {
			currentVolume += 0.1f;
			adjustVolume();
		}
	}
	
	public void handleSdp(String sdpStr) {
		try {
			log.info("Answer SDP:\n");
			SDPMessage sdpMessage = new SDPMessage();
			sdpMessage.parseBuffer(sdpStr);
			WebRTCSessionDescription description = new WebRTCSessionDescription(WebRTCSDPType.ANSWER, sdpMessage);
			webRTCBin.setRemoteDescription(description);
			//Todo remove some of the code here is useless
			
		} catch (Exception exception) {
			exception.printStackTrace();
			//logger.atSevere().withCause(exception).log(exception.getLocalizedMessage());
			log.info(exception.getLocalizedMessage());
		}
	}
	
	public void handleIceSdp(String candidate, int sdpMLineIndex) {
		try {
			log.info("Adding remote client ICE candidate : ");
			webRTCBin.addIceCandidate(sdpMLineIndex, candidate);
		} catch (Exception exception) {
			exception.printStackTrace();
			//logger.atSevere().withCause(exception).log(exception.getLocalizedMessage());
			log.info(exception.getLocalizedMessage());
		}
	}
	
	public void startCall() {
		webRTCBin = (WebRTCBin) pipe.getElementByName("webrtcbin");
		setupPipeLogging(pipe);
		WebRTCBin.ON_NEGOTIATION_NEEDED onNegotiationNeeded = elem -> webRTCBin.createOffer(onOfferCreated);
		webRTCBin.connect(onNegotiationNeeded);
		WebRTCBin.ON_ICE_CANDIDATE onIceCandidate = (sdpMLineIndex, candidate) -> {
			var    ice  = new JSONObject().put("candidate", candidate).put("sdpMLineIndex", sdpMLineIndex);
			String json = new JSONObject().put("ice", ice).toString();
			log.info("ON_ICE_CANDIDATE: ");
			Map<String, Object> candidateMap = new HashMap<>();
			candidateMap.put("sdpMLineIndex", sdpMLineIndex);
			candidateMap.put("candidate", candidate);
			
			JSONObject response = new JSONObject();
			response.put("iceCandidates", candidateMap);
			response.put("feature", FeatureTypes.G_STREAM.toString());
			response.put(Events.EVENT_TYPE, Events.ICE_CANDIDATES_EVENT);
			notifyClient(response, this.thisObjectPositionAddress);
			
			
		};
		webRTCBin.connect(onIceCandidate);
		Element.PAD_ADDED onIncomingStream = (element, pad) -> {
			log.info("Receiving stream! Element : " + element.getName() + " Pad : " + pad.getName());
			if (pad.getDirection() != PadDirection.SRC) {
				return;
			}
			DecodeBin decodeBin = new DecodeBin("decodebin_" + pad.getName());
			decodeBin.connect(onDecodedStream);
			pipe.add(decodeBin);
			decodeBin.syncStateWithParent();
			pad.link(decodeBin.getStaticPad("sink"));
		};
		webRTCBin.connect(onIncomingStream);
		log.info("initiating call");
		//Todo remove some of the code here is useless
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (!isPaused) {
					if (!pipe.isPlaying()) {
						log.info("initiating streams");
						pipe.play();
						startClock();
					}
				}
			}
		}, 100 * 2); // delay in milliseconds, this is to allow the objects to be inited
	}
	
	public void pauseTransmission() {
		if (!isPaused) {
			isPaused = true;
			pipe.setState(State.PAUSED);
			JSONObject response = new JSONObject();
			response.put("transaction", getTransaction());
			response.put("feature", FeatureTypes.G_STREAM.toString());
			response.put(Events.EVENT_TYPE, Events.PAUSE_G_STREAM_EVENT);
			notifyClient(response, this.thisObjectPositionAddress);
		}
	}
	
	public void resumeTransmission() {
		if (isPaused) {
			isPaused = false;
			pipe.play();
			
			JSONObject response = new JSONObject();
			response.put("transaction", getTransaction());
			response.put("feature", FeatureTypes.G_STREAM.toString());
			response.put(Events.EVENT_TYPE, Events.RESUME_G_STREAM_EVENT);
			notifyClient(response, this.thisObjectPositionAddress);
		}
	}
	
	private void endCall() {
		log.info("ending call");
		pipe.setState(isPaused ? State.PAUSED : State.NULL);
		//  Gst.quit();
	}
	
	// Decrease the volume step-by-step
	public void decreaseVolume() {
		if (currentVolume > 0.1f) {
			currentVolume -= 0.1f;
			adjustVolume();
		}
		
	}
	private double currentVolumeInPercentage(){
		return currentVolume * 100;
	}
	
	// Helper method to adjust the volume dynamically.
	private void adjustVolume() {
		// ToDo add limit or max level
		Element volumeElement = pipe.getElementByName("audioamplify");
		if (volumeElement != null) {
			volumeElement.set("amplification", currentVolume);
			
		}
		JSONObject response = new JSONObject();
		response.put("transaction", getTransaction());
		response.put("currentVolume", currentVolume);
		response.put("currentVolumeInPercentage", currentVolumeInPercentage());
		response.put("feature", FeatureTypes.G_STREAM.toString());
		response.put(Events.EVENT_TYPE, Events.VOLUME_ADJUSTED_G_STREAM_EVENT);
		notifyClient(response, this.thisObjectPositionAddress);
	}
	
	@Override
	public void updateClientLastTimeStamp() {
		commonAbout.onUpdateLastTimeStamp(System.currentTimeMillis());
	}
	
	@Override
	protected void notifyClient(JSONObject pluginData, final Integer objectPosition) {
		commonAbout.sendMessage(pluginData, objectPosition);
	}
}
