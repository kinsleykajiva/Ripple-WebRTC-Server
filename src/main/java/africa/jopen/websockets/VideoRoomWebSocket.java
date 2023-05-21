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

@ServerEndpoint("/video-room/{name}")
@ApplicationScoped
public class VideoRoomWebSocket {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	@OnOpen
	public void onOpen(Session session, @PathParam("name") String name) {
		System.out.println("onOpen> " + name);
	}
	
	@OnClose
	public void onClose(Session session, @PathParam("name") String name) {
		System.out.println("onClose> " + name);
	}
	
	@OnError
	public void onError(Session session, @PathParam("name") String name, Throwable throwable) {
		System.out.println("onError> " + name + ": " + throwable);
	}
	
	@OnMessage
	public void onMessage(String message, @PathParam("name") String name) {
		System.out.println("onMessage> " + name + ": " + message);
	}
}
