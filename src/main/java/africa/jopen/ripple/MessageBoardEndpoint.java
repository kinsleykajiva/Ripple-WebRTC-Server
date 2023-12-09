package africa.jopen.ripple;


import africa.jopen.ripple.utils.MessageQueue;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;

import org.apache.log4j.Logger;

public class MessageBoardEndpoint implements WsListener {

	static        Logger       log          = Logger.getLogger(MessageBoardEndpoint.class.getName());
	private final MessageQueue messageQueue = MessageQueue.instance();
	
	@Override
	public void onMessage(WsSession session, String text, boolean last) {
		log.info("Received message: " + text);
		log.info("Received message last : " + last);
		// Send all messages in the queue
		if (text.equals("send")) {
			while (!messageQueue.isEmpty()) {
				session.send(messageQueue.pop(), last);
			}
		}
	}
	
	@Override
	public void onClose(WsSession session, int status, String reason) {
		log.info("Session closed: " + session);
		log.info("Session reason: " + reason);
		WsListener.super.onClose(session, status, reason);
	}
	
	@Override
	public void onError(WsSession session, Throwable t) {
		log.trace( "Session error: " + session, t);
		WsListener.super.onError(session, t);
	}
	
	@Override
	public void onOpen(WsSession session) {
		log.info("Session opened: " + session);
		WsListener.super.onOpen(session);
	}
}
