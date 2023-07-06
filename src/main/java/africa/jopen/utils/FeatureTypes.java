package africa.jopen.utils;

public enum FeatureTypes {
	VIDEO_ROOM("VIDEO_ROOM"),
	AUDIO_ROOM("AUDIO_ROOM"),
	G_STREAM("G_STREAM"),
	VIDEO_CALL("VIDEO_CALL");
	
	private final String description;
	
	FeatureTypes(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}
}
