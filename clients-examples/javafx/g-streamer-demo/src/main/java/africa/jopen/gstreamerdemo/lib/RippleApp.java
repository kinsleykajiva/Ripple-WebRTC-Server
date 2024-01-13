package africa.jopen.gstreamerdemo.lib;

import org.json.JSONObject;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RippleApp {
	
	static       Logger  log         = Logger.getLogger(RippleApp.class.getName());
	public       boolean isDebugging = false;
	public       boolean isConnected = false;
	public final String  serverUrl;
	public final String  clientID;
	private      Session session;
	
	public RippleApp(String serverUrl, PluginCallbacks.RootPluginCallBacks rootPluginCallBacks) {
		serverUrl = RippleUtils.convertToWebSocketUrl(serverUrl);
		if (serverUrl.endsWith("/")) {
			serverUrl = serverUrl + "websocket/client";
		} else {
			serverUrl = serverUrl + "/websocket/client";
		}
		this.serverUrl = serverUrl;
		String uniqueID = RippleUtils.uniqueIDGenerator("", 22);
		this.clientID = RippleUtils.IdGenerator() + uniqueID;
		log.info("RippleApp initialized");
		log.info("serverUrl: " + serverUrl);
		log.info("clientID: " + clientID);
		
		try {
			WebSocketContainer            container = ContainerProvider.getWebSocketContainer();
			RippleClientWebSocketEndpoint endpoint  = new RippleClientWebSocketEndpoint();
			endpoint.setRippleApp(this);
			session = container.connectToServer(endpoint, URI.create(this.serverUrl));
			
		} catch (DeploymentException | IOException e) {
			log.log(Level.SEVERE, "Error connecting to server", e);
		}
	}
	
	public void onMessage(String message) {
		log.info("Received message: " + message);
	}
	
	public void onOpen(String message) {
		log.info(message);
	}
	
	public void sendMessage(JSONObject message) {
		if (message == null) {
			log.warning("Message is null, nothing to send");
			return;
		}
		if (!isConnected) {
			log.warning("Not connected,Failed to send message");
			return;
		}
		if (message.toString().isEmpty()) {
			log.warning("Message is empty, nothing to send");
			return;
		}
		log.info("Sending message");
		log.info(message.toString());
		try {
			session.getBasicRemote().sendText(message.toString());
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error sending message", e);
		}
	}
	
	
	public void onError(Throwable e) {
		
		log.log(Level.SEVERE, "Socket session Error", e);
	}
}
