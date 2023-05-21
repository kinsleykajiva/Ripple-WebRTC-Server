package africa.jopen.websockets;

import com.google.common.flogger.FluentLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import static java.util.Objects.requireNonNull;

@ServerEndpoint("/video-room/{clientID}")
@ApplicationScoped
public class VideoRoomWebSocket {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	@OnOpen
	public void onOpen(Session session, @PathParam("clientID") String clientID) {
		System.out.println("onOpen> " + clientID);
	}
	
	@OnClose
	public void onClose(Session session, @PathParam("clientID") String clientID) {
		System.out.println("onClose> " + clientID);
	}
	
	@OnError
	public void onError(Session session, @PathParam("clientID") String clientID, Throwable throwable) {
		System.out.println("onError> " + clientID + ": " + throwable);
	}
	
	@OnMessage
	public void onMessage(String message, @PathParam("clientID") String clientID) {
		System.out.println("onMessage> " + clientID + ": " + message);
	}
}
