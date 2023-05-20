package africa.jopen.http;

public record PostCreateRoom(String roomName, String roomDescription, String creatorClientID, String password, String pin) {
}
