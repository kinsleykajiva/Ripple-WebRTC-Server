package africa.jopen.utils;

public interface Requests {
	String REQUEST_TYPE = "requestType";
	String REMEMBER = "remember";
	String ANSWER_CALL = "answer-call";
	 String MAKE_CALL = "make-call";
	 String HANGUP = "hangup";
	 String UPDATE_ICE_CANDIDATE = "update-ice-candidate";
	String SEND_OFFER = "send-offer";
	
	
	 int OK_RESPONSE = 200;
	 int SERVER_ERROR = 500;
	 int SERVER_BAD_REQUEST = 400;
}
