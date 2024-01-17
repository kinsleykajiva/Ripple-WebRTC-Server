package africa.jopen.gstreamerdemo.lib;

public class IceCandidateMessage {
	static class Candidate {
		String sdpMid;
		int    sdpMLineIndex;
		String candidate;
	}
	
	private final Candidate candidate = new Candidate();
	
	
	/**
	 * Set the media stream identification tag for the media component the
	 * candidate is associated with.
	 *
	 * @param mid The media stream identification tag.
	 */
	public void setSdpMid(String mid) {
		candidate.sdpMid = mid;
	}
	
	/**
	 * Set the index (starting at zero) of the media description in the SDP the
	 * candidate is associated with.
	 *
	 * @param index The index of the media description in the SDP.
	 */
	public void setSdpMLineIndex(int index) {
		candidate.sdpMLineIndex = index;
	}
	
	/**
	 * Set the SDP string representation of the candidate.
	 *
	 * @param sdp The SDP of the candidate.
	 */
	public void setSdp(String sdp) {
		candidate.candidate = sdp;
	}
}
