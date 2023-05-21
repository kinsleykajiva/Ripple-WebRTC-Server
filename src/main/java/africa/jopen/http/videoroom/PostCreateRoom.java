package africa.jopen.http.videoroom;

public record PostCreateRoom(String roomName, String roomDescription, String creatorClientID, String password, String pin) {
}
