package africa.jopen.utils;

public interface Events {

    public final String NOTIFICATION_EVENT = "notification";
    public final String SDP_OFFER_EVENT = "offer";
    public final String SDP_ANSWER_EVENT = "answer";
    public final String ICE_CANDIDATES_EVENT = "iceCandidates";
    public final String REMEMBER_EVENT = "remember";
    final String ERROR_EVENT = "error";
    final String SYSTEM_FATAL_ERROR_EVENT = "fatalError";
    final String VALIDATION_ERROR_EVENT = "validation";
    final String AUTH_ERROR_EVENT = "auth";
    String EVENT_TYPE = "eventType";
}
