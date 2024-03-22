package africa.jopen.ripple.interfaces;

public interface Events {
	
	String NOTIFICATION_EVENT               = "notification";
	String INCOMING_CALL_NOTIFICATION_EVENT = "incomingCall";
	String CALL_ANSWERED_NOTIFICATION_EVENT = "callAnswered";
	String SDP_OFFER_EVENT                  = "offer";
	String WEBRTC_EVENT                     = "webrtc";
	String SDP_ANSWER_EVENT                 = "answer";
	String ICE_CANDIDATES_EVENT             = "iceCandidates";
	String PROGRESS_G_STREAM_EVENT          = "progressGstream";
	String RESUME_G_STREAM_EVENT            = "resumeGstream";
	String VOLUME_ADJUSTED_G_STREAM_EVENT   = "volumeAdjustedGstream";
	String END_OF_STREAM_G_STREAM_EVENT     = "endedGstream";
	String PAUSE_G_STREAM_EVENT             = "pauseGstream";
	String SIP_REGISTRATION             = "sipRegistration";
	String SIP_CALL_PROGRESS             = "sipCallProgress";
	String SIP_CALL_ACCEPTED             = "sipCallAccepted";
	String SIP_CALL_INCOMING_ACCEPTED             = "sipCallIncomingAccepted";
	String SIP_CALL_INCOMING_TIME_OUT             = "sipCallIncomingTimeOut";
	String SIP_CALL_INCOMING             = "sipCallIncoming";
	String SIP_CALL_MEDIA_STARTED             = "sipCallMediaStarted";
	String SIP_CALL_MEDIA_STOPPED             = "sipCallMediaStopped";
	String SIP_CALL_ON_CONFIRMED             = "sipCallConfirmed";
	String SIP_CALL_FAILED             = "sipCallFailed";
	String SIP_CALL_ENDED                = "sipCallEnded";
	String SIP_CALL_CANCELLED             = "sipCallCancelled";
	String SIP_CALL_REJECTED             = "sipCallRejected";
	String SIP_CALL_REINVITE             = "sipCallReinvite";
	String SIP_CALL_HOLD             = "sipCallHold";
	String SIP_CALL_RESUME             = "sipCallResume";
	String SIP_CALL_TRANSFER             = "sipCallTransfer";
	String SIP_CALL_TRANSFERED             = "sipCallTransferred";
	String SIP_CALL_TRANSFER_ACCEPTED     = "sipCallTransferAccepted";
	String SIP_CALL_TRANSFER_FAILED     = "sipCallTransferFailed";
	String SIP_CALL_RINGING     = "sipCallRinging";
	String SIP_CALL_REDIRECT     = "sipCallRedirect";
	
	String REMEMBER_EVENT                   = "remember";
	String ERROR_EVENT                      = "error";
	String SYSTEM_FATAL_ERROR_EVENT         = "fatalError";
	String VALIDATION_ERROR_EVENT           = "validation";
	String AUTH_ERROR_EVENT                 = "auth";
	String EVENT_TYPE                       = "eventType";
	
}