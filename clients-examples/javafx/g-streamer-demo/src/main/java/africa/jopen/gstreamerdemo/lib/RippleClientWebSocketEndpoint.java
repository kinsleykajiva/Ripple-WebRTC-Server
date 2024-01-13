package africa.jopen.gstreamerdemo.lib;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import java.util.logging.Level;
import java.util.logging.Logger;

@ClientEndpoint
public class RippleClientWebSocketEndpoint {
	
	static Logger log = Logger.getLogger(RippleClientWebSocketEndpoint.class.getName());
	@OnOpen
	public void onOpen(Session session) {
		log.info("WebSocket opened");
		//session.getAsyncRemote().sendText("Hello from the client");
	}
	
	@OnMessage
	public void onMessage(String message) {
		log.info("Received msg: " + message);
		
	}
	
	@OnError
	public void onError(Throwable e) {
		e.printStackTrace();
		log.log(Level.SEVERE, "Error", e);
	}
}
