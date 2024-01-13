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
	private RippleApp rippleApp;
	public void setRippleApp(RippleApp rippleApp) {
		this.rippleApp = rippleApp;
	}
	@OnOpen
	public void onOpen(final Session session) {
		log.info("WebSocket opened");
		//session.getAsyncRemote().sendText("Hello from the client");
		rippleApp.onOpen("WebSocket opened");
	}
	
	@OnMessage
	public void onMessage(final String message) {
		log.info("Received msg: " + message);
		if(message == null || message.isEmpty()) return;
		if(!RippleUtils.isJson(message)) return;
		rippleApp.onMessage(message);
		
	}
	
	
	
	@OnError
	public void onError(Throwable e) {
		e.printStackTrace();
		log.log(Level.SEVERE, "Error", e);
		rippleApp.onError(e);
	}
}
