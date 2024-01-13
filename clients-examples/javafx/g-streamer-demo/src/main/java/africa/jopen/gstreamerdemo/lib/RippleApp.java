package africa.jopen.gstreamerdemo.lib;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RippleApp {
	
	static        Logger                              log             = Logger.getLogger(RippleApp.class.getName());
	public        boolean                             isDebugging     = false;
	public        boolean                             isConnected     = false;
	public final  String                              serverUrl;
	public final  String                              clientID;
	private       WebSocket                           webSocket;
	private final PluginCallbacks.RootPluginCallBacks rootPluginCallBacks;
	private final ScheduledExecutorService            executorService = Executors.newSingleThreadScheduledExecutor();
	
	public RippleApp(String serverUrl, PluginCallbacks.RootPluginCallBacks rootPluginCallBacks) {
		serverUrl = RippleUtils.convertToWebSocketUrl(serverUrl);
		if (serverUrl.endsWith("/")) {
			serverUrl = serverUrl + "websocket/client";
		} else {
			serverUrl = serverUrl + "/websocket/client";
		}
		this.serverUrl = serverUrl;
		String uniqueID = RippleUtils.uniqueIDGenerator(RippleUtils.nonAlphaNumeric(System.getProperty("user.name")), 22);
		this.clientID = RippleUtils.IdGenerator() + uniqueID;
		log.info("RippleApp initialized");
		log.info("serverUrl: " + serverUrl);
		log.info("clientID: " + clientID);
		this.rootPluginCallBacks = rootPluginCallBacks;
	}
	private void startToRemindServerOfMe() {
		JSONObject message = new JSONObject();
		message.put("requestType", "remember");
		message.put("clientID", clientID);
		
		// Create a new virtual thread executor
		try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
			
			// Schedule the task to run every 30 seconds
			virtualExecutor.submit(() -> {
				while (true) {
					sendMessage(message);
					try {
						TimeUnit.SECONDS.sleep(30);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			});
		}
	}
	
	public void onMessage(final String message) {
		log.info("Received message: " + message);
		final JSONObject messageObject = new JSONObject(message);
		String           eventType     = messageObject.optString("eventType", null);
		boolean          success       = messageObject.optBoolean("success", false);
		JSONObject       plugin        = messageObject.optJSONObject("plugin", null);
		
		if (success && eventType != null) {
			switch (eventType) {
				case "newThread":
					break;
				case "register":
					System.out.println("Registered");
					startToRemindServerOfMe();
					break;
				default:
					break;
				
			}
		}
	}
	
	public void onOpen(String message) {
		log.info(message);
		isConnected = true;
		// cancell the scheduled reconnect
		executorService.shutdown();
		rootPluginCallBacks.onClientConnected();
		sendMessage(new JSONObject().put("requestType", "register").put("clientID", clientID));
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
		webSocket.send(message.toString());
	}
	
	public void connect() {
		
		OkHttpClient                  client   = new OkHttpClient();
		Request                       request  = new Request.Builder().url(serverUrl).build();
		RippleClientWebSocketEndpoint listener = new RippleClientWebSocketEndpoint();
		listener.setRippleApp(this);
		webSocket = client.newWebSocket(request, listener);
	}
	
	private void scheduleReconnect() {
		log.info("Scheduling reconnect");
		runAfterDelay(() -> {
			if (isConnected) {
				log.info("Already connected");
				return;
			}
			log.info("Reconnecting");
			connect();
		}, 10);
	}
	
	public void runAfterDelay(Runnable task, long delay) {
		executorService.schedule(task, delay, TimeUnit.SECONDS);
	}
	
	public void onClose(String webSocketClosed) {
		log.info(webSocketClosed);
		scheduleReconnect();
		rootPluginCallBacks.onClientClosed();
	}
	
	public void onError(Throwable e) {
		log.log(Level.SEVERE, "Socket session Error", e);
		rootPluginCallBacks.onClientError(e);
	}
	
	
}
