package africa.jopen.ripple.models;

import java.util.Objects;


public final class RTCModel {
	private String offer;
	private String answer;
	
	public String offer() {
		return offer;
	}
	
	public String answer() {
		return answer;
	}
	
	public String setAnswer(String answer) {
		return this.answer = answer;
	}
	
	public String setOffer(String offer) {
		return this.offer = offer;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (RTCModel) obj;
		return Objects.equals(this.offer, that.offer) &&
				Objects.equals(this.answer, that.answer);
	}
	
	
	
	@Override
	public int hashCode() {
		return Objects.hash(offer, answer);
	}
	
	@Override
	public String toString() {
		return "RTCModel[" +
				"answer=" + offer + ", " +
				"answer=" + answer + ']';
	}
	
	
}
