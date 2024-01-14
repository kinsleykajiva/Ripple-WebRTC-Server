package africa.jopen.gstreamerdemo.lib;

import africa.jopen.gstreamerdemo.lib.utils.VideoView;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import org.jetbrains.annotations.Blocking;

public abstract class RipplePlugin {
	@Blocking
	protected abstract VideoView addThread(int threadRef);
	
	@Blocking
	protected void addUIVideoView(VideoView videoView) {
		//Implement the necessary functionality here
	}
	
	/**This is to pass new data media stream to the plugin.This will be called from the WebRTC Ontrack PeerConnection*/
	@Blocking
	protected abstract void onTrack(MediaStreamTrack track, int threadRef);
}
