package africa.jopen.gstreamerdemo.lib;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
public class RippleApp {
	
	static       Logger                        log         = Logger.getLogger(RippleApp.class.getName());
	public       boolean                       isDebugging = false;
	public       boolean                       isConnected = false;
	public final String                        serverUrl;
	public final String                        clientID;
	
	public RippleApp(String serverUrl) {
		this.serverUrl = serverUrl;
		String uniqueID = RippleUtils.uniqueIDGenerator("", 22);
		this.clientID = RippleUtils.IdGenerator() + uniqueID;
		log.info("RippleApp initialized");
		log.info("serverUrl: " + serverUrl);
		log.info("clientID: " + clientID);
		
		try {
			
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			Session            session   = container.connectToServer(RippleClientWebSocketEndpoint.class, URI.create(this.serverUrl));
			session.getBasicRemote().sendText("Hello");
			session.
		} catch (DeploymentException | IOException e) {
			log.log(Level.SEVERE, "Error connecting to server", e);
		}
	}
	public void sendMessage(JSONObject message){
		log.info("Sending message");
		log.info(message.toString());
	}
	
	
}
