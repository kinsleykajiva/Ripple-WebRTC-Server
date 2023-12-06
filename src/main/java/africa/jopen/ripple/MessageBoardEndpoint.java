package africa.jopen.ripple;

import africa.jopen.ripple.utils.LoggerConfig;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageBoardEndpoint implements WsListener {
	private static final Logger       LOGGER       = Logger.getLogger(MessageBoardEndpoint.class.getName());
	private final        MessageQueue messageQueue = MessageQueue.instance();
	static {
		LoggerConfig.setupLogger(LOGGER);
	}
	@Override
	public void onMessage(WsSession session, String text, boolean last) {
		LOGGER.info("Received message: " + text);
		LOGGER.info("Received message last : " + last);
		// Send all messages in the queue
		if (text.equals("send")) {
			while (!messageQueue.isEmpty()) {
				session.send(messageQueue.pop(), last);
			}
		}
	}
	
	@Override
	public void onClose(WsSession session, int status, String reason) {
		LOGGER.info("Session closed: " + session);
		LOGGER.info("Session reason: " + reason);
		WsListener.super.onClose(session, status, reason);
	}
	
	@Override
	public void onError(WsSession session, Throwable t) {
		LOGGER.log(Level.SEVERE, "Session error: " + session, t);
		WsListener.super.onError(session, t);
	}
	
	@Override
	public void onOpen(WsSession session) {
		LOGGER.info("Session opened: " + session);
		WsListener.super.onOpen(session);
	}
}
