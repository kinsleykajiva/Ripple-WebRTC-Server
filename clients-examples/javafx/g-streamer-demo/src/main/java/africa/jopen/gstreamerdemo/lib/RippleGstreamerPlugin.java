package africa.jopen.gstreamerdemo.lib;

import africa.jopen.gstreamerdemo.lib.utils.VideoView;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@ApiStatus.NonExtendable
public class RippleGstreamerPlugin extends RipplePlugin {
	static        Logger          log        = Logger.getLogger(RippleApp.class.getName());
	private final List<Integer>  threadRefs = new ArrayList<>();
	private final Map<Integer,VideoView> VideoViews = new HashMap<>();
	
	public RippleGstreamerPlugin() {
		log.info("RippleGstreamerPlugin created");
	}
	
	
	protected void startBroadCast() {
		log.info("startBroadCast");
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
		return null;
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
