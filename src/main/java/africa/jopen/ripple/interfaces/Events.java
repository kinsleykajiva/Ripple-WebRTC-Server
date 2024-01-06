package africa.jopen.ripple.interfaces;

public interface Events {
	
	String NOTIFICATION_EVENT               = "notification";
	String INCOMING_CALL_NOTIFICATION_EVENT = "incomingCall";
	String CALL_ANSWERED_NOTIFICATION_EVENT = "callAnswered";
	String SDP_OFFER_EVENT                  = "offer";
	String WEBRTC_EVENT                     = "webrtc";
	String SDP_ANSWER_EVENT                 = "answer";
	String ICE_CANDIDATES_EVENT             = "iceCandidates";
	String RESUME_G_STREAM_EVENT             = "resumeGstream";
	String END_OF_STREAM_G_STREAM_EVENT             = "endedGstream";
	String PAUSE_G_STREAM_EVENT             = "pauseGstream";
	String REMEMBER_EVENT                   = "remember";
	String ERROR_EVENT                      = "error";
	String SYSTEM_FATAL_ERROR_EVENT         = "fatalError";
	String VALIDATION_ERROR_EVENT           = "validation";
	String AUTH_ERROR_EVENT                 = "auth";
	String EVENT_TYPE                       = "eventType";
	
}