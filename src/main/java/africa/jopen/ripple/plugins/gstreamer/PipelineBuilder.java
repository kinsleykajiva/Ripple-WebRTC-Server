package africa.jopen.ripple.plugins.gstreamer;

public class PipelineBuilder {
	private StringBuilder pipeline;
	
	public PipelineBuilder() {
		this.pipeline = new StringBuilder();
	}
	
	public PipelineBuilder addFileSource(String path) {
		this.pipeline.append("filesrc location=").append(path).append(" ! decodebin name=decoder\n");
		return this;
	}
	
	public PipelineBuilder addVideoConvert() {
		this.pipeline.append("decoder. ! videoconvert ! queue2 max-size-buffers=1000 ! vp8enc deadline=1 ! rtpvp8pay ! queue ! application/x-rtp,media=video,encoding-name=VP8,payload=97 ! webrtcbin.\n");
		return this;
	}
	
	public PipelineBuilder addAudioConvert() {
		this.pipeline.append("decoder. ! audioconvert ! audioresample ! audioamplify amplification=2.0 ! queue2 max-size-buffers=1000 ! opusenc ! rtpopuspay ! queue ! application/x-rtp,media=audio,encoding-name=OPUS,payload=96 ! webrtcbin.\n");
		return this;
	}
	
	public PipelineBuilder addWebRTCBin() {
		this.pipeline.append("webrtcbin name=webrtcbin bundle-policy=max-bundle stun-server=stun://stun.l.google.com:19302");
		return this;
	}
	
	public String build() {
		return this.pipeline.toString();
	}
}