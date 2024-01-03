package africa.jopen.ripple.models;

public record MediaFile(String path,long maxDuration) {
	public MediaFile(String path, long maxDuration) {
		this.path = path;
		this.maxDuration = maxDuration;
	}
}
