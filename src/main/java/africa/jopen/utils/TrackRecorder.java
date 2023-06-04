package africa.jopen.utils;

import com.google.common.flogger.FluentLogger;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSink;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.VideoTrackSink;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TrackRecorder implements AudioTrackSink, VideoTrackSink {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private final File file;
	private final MediaStreamTrack mediaStreamTrack;
	
	private final String mediaKind;
	private final VideoFileRenderer videoFileRenderer;
	private Boolean recording = false;
	private Boolean infoWritten = false;
	
	public TrackRecorder(String fileName, MediaStreamTrack mediaStreamTrack) {
		
		final String recordingPath = "recording" /*Properties.getPropertyS("RecordingPath", ".")*/;
		
		this.file = new File(recordingPath, fileName);
		this.mediaStreamTrack = mediaStreamTrack;
		this.mediaKind = mediaStreamTrack.getKind();
		logger.atInfo().log("Track Recorder: tracks is %s", mediaKind);
		
		if (mediaKind.equals(MediaStreamTrack.VIDEO_TRACK_KIND)) {
			videoFileRenderer = new VideoFileRenderer(file);
		} else {
			videoFileRenderer = null;
		}
		
	}
	
	public void start() {
		
		if (mediaKind.equals(MediaStreamTrack.AUDIO_TRACK_KIND))
			((AudioTrack) mediaStreamTrack).addSink(this);
		
		else if (mediaKind.equals(MediaStreamTrack.VIDEO_TRACK_KIND))
			((VideoTrack) mediaStreamTrack).addSink(this);
		
		else return;    // unknown type, recording didn't start
		
		this.recording = true;
		
	}
	
	public void stop() {
		
		if (!this.recording) return;
		
		if (mediaKind.equals(MediaStreamTrack.AUDIO_TRACK_KIND))
			((AudioTrack) mediaStreamTrack).removeSink(this);
		
		if (mediaKind.equals(MediaStreamTrack.VIDEO_TRACK_KIND))
			((VideoTrack) mediaStreamTrack).removeSink(this);
		
		this.recording = false;
	}
	
	@Override
	public void onData(byte[] data, int bitsPerSample, int sampleRate, int channels, int frames) {
		
		if (!infoWritten)
			writeInfo("bitsPerSample: %d, sampleRate: %d, channels %d", bitsPerSample, sampleRate, channels);
		
		try (FileOutputStream fos = new FileOutputStream(file, true)) {
			
			fos.write(data);
			
		} catch (IOException ex) {
			logger.atSevere().withCause(ex).log(ex.getMessage());
		}
	}
	
	@Override
	public void onVideoFrame(VideoFrame frame) {
		
		if (!infoWritten)
			writeInfo("");
		
		try {
			videoFileRenderer.queue(frame);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.atSevere().withCause(e).log(e.getMessage());
		}
	}
	
	void writeInfo(final String message, Object... args) {
		

	}
	
	
}
