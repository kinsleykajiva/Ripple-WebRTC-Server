package africa.jopen.models;


public record VideCallNotification(String notificationID, String fromClientID, String toClientID, long start, long end) {

}
