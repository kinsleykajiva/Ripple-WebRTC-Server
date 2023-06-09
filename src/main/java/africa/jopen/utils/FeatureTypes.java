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
	public static FeatureTypes getEnumByString(String featureType) {
		for (FeatureTypes type : FeatureTypes.values()) {
			if (type.description.equals(featureType)) {
				return type;
			}
		}
		throw new IllegalStateException("Unexpected value: " + featureType);
	}
}
