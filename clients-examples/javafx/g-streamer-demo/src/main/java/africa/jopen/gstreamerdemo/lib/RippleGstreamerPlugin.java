package africa.jopen.gstreamerdemo.lib;

import africa.jopen.gstreamerdemo.lib.utils.VideoView;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@ApiStatus.NonExtendable
public class RippleGstreamerPlugin extends RipplePlugin {
	static        Logger          log        = Logger.getLogger(RippleApp.class.getName());
	private final List<Integer>  threadRefs = new ArrayList<>();
	private final Map<Integer,VideoView> VideoViews = new HashMap<>();
	private       ExecutorService        executor   = Executors.newVirtualThreadPerTaskExecutor();
	private RippleApp rippleApp;
	
	public RippleGstreamerPlugin(RippleApp rippleApp) {
		log.info("RippleGstreamerPlugin created");
		this.rippleApp = rippleApp;
	}
	
	
	protected RipplePeerConnection startBroadCast(int threadRef) {
		log.info("startBroadCast");
		var peerConnection = new RipplePeerConnection(threadRef,rippleApp);
		var json = new JSONObject();
		json.put("requestType", "startBroadCast");
		json.put("threadRef", threadRef);
		rippleApp.sendMessage(json);
		return peerConnection;
	}
	
	public void renderThreadUI() {
		log.info("renderThreadUI");
	}
	public VideoView buildVideoView(int threadRef) {
		VideoView videoView = new VideoView();
		videoView.setId("videoView_" + threadRef);
		videoView.setPrefSize(320, 240);
		
		
		return videoView;
	}
	
	@Override
	@Blocking
	public VideoView addThread(int threadRef) {
		log.info("addThread");
		threadRefs.add(threadRef);
	 addUIVideoView(buildVideoView(threadRef),threadRef);
	 return  VideoViews.get(threadRef);
	}
	
	public Map<Integer, VideoView> getVideoViews() {
		return VideoViews;
	}
	
	@Blocking
	protected void addUIVideoView(@NotNull VideoView videoView,int threadRef) {
		VideoViews.put(threadRef,videoView);
	}
	

	@Blocking
	protected void onTrack(MediaStreamTrack track, int threadRef) {
		log.info("onTrack");
		// find VideoViews by threadRef
		VideoView videoView = VideoViews.get(threadRef);
		if(videoView == null){
			log.info("videoView is null");
			return;
		}
		String kind = track.getKind();
		if (kind.equals(MediaStreamTrack.VIDEO_TRACK_KIND)) {
			VideoTrack videoTrack = (VideoTrack) track;
			videoTrack.addSink(videoFrame->{
				log.info("Video track added");
				videoFrame.retain();
				videoView.setVideoFrame(videoFrame);
				videoFrame.release();
			});
		}
		
	}
}
