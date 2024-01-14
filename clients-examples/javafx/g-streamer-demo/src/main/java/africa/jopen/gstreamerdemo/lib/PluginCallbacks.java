package africa.jopen.gstreamerdemo.lib;

import africa.jopen.gstreamerdemo.lib.utils.VideoView;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginCallbacks {
	public enum FeaturesAvailable {
		G_STREAM_BROADCASTER,
		G_STREAM
	}
	
	public interface RootPluginCallBacks {
		void onClientClosed();
		
		void onClientConnected();
		
		void onClientError(Throwable t);
		
		void onClientMessage(String message);
		
		void webRTCEvents(String message);
	}
	
	public interface GstreamerPluginCallBack extends RootPluginCallBacks {
		void onStreamUIUpdates(@NotNull String message);
		
		@Blocking
		void onStreamUIUpdates(@Nullable VideoView videoView);
	}
	
	public interface WebRTCPeerEvents {
		void IceCandidate(String message);
		
		/*The adding of MediaStreamTrack Class makes this hard to keep this de-coupled*/
		void onTrack(MediaStreamTrack track);
		
		void onTrack(MediaStreamTrack track, int threadRef);
		
		@NonBlocking
		void notify(@Nullable String message);
		
		
	}
	
}
