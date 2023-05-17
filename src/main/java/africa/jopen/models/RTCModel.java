package africa.jopen.models;

import org.json.JSONObject;

import java.util.Objects;


public final class RTCModel {
    private  JSONObject offer;
    private  JSONObject answer;

    public JSONObject offer() {
        return offer;
    }

    public JSONObject answer() {
        return answer;
    }
    public JSONObject setAnswer( JSONObject answer) {
        return this.answer =answer;
    }

   public JSONObject setOffer( JSONObject offer) {
        return this.offer =offer;
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
                "offer=" + offer + ", " +
                "answer=" + answer + ']';
    }


}
