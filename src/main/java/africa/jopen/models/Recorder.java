package africa.jopen.models;


import africa.jopen.utils.TrackRecorder;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class Recorder {
	final private MutableList<TrackRecorder> trackRecorders = Lists.mutable.empty();
	
	Boolean recording = false;
	
	
	public Recorder() {
	}
	
	public Boolean getRecording() {
		return recording;
	}
	
	public void addTrack(String filename, MediaStreamTrack mediaStreamTrack) {
		
		TrackRecorder trackRecorder = new TrackRecorder(filename, mediaStreamTrack);
		trackRecorders.add(trackRecorder);
		
		if (recording) {
			trackRecorder.start();
		}
	}
	
	public void start() {
		
		for (TrackRecorder trackRecorder : trackRecorders) {
			
			trackRecorder.start();
		}
		
		this.recording = true;
	}
	
	public void stop() {
		
		for (TrackRecorder trackRecorder : trackRecorders) {
			trackRecorder.stop();
		}
		
		this.recording = false;
	}
	
}
