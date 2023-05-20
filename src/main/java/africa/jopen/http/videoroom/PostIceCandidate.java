package africa.jopen.http.videoroom;

import africa.jopen.http.IceCandidate;

public record PostIceCandidate(String roomID, IceCandidate iceCandidate, String clientID) {
}
