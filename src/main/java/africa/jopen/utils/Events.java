package africa.jopen.utils;

public interface Events {
	
	String NOTIFICATION_EVENT       = "notification";
	String INCOMING_CALL_NOTIFICATION_EVENT = "incomingCall";
	String SDP_OFFER_EVENT          = "offer";
	String WEBRTC_EVENT          = "webrtc";
	String SDP_ANSWER_EVENT         = "answer";
	String ICE_CANDIDATES_EVENT     = "iceCandidates";
	String REMEMBER_EVENT           = "remember";
	String ERROR_EVENT              = "error";
	String SYSTEM_FATAL_ERROR_EVENT = "fatalError";
	String VALIDATION_ERROR_EVENT   = "validation";
	String AUTH_ERROR_EVENT         = "auth";
	String EVENT_TYPE               = "eventType";
	
}
