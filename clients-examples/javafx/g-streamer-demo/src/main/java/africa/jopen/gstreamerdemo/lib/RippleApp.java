package africa.jopen.gstreamerdemo.lib;

import africa.jopen.gstreamerdemo.lib.utils.VideoView;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static africa.jopen.gstreamerdemo.lib.RipplePeerConnection.setRemoteOfferStringSdp;

public class RippleApp implements PluginCallbacks.WebRTCPeerEvents {
	
	static        Logger                                 log                = Logger.getLogger(RippleApp.class.getName());
	public        boolean                                isDebugging        = false;
	public        boolean                                isConnected        = false;
	public final  String                                 serverUrl;
	public final  String                                 clientID;
	private       WebSocket                              webSocket;
	private final PluginCallbacks.RootPluginCallBacks    rootPluginCallBacks;
	private final ScheduledExecutorService               executorService    = Executors.newSingleThreadScheduledExecutor();
	private final ExecutorService                        perTaskExecutor    = Executors.newVirtualThreadPerTaskExecutor();
	private final HashMap<Integer, RipplePeerConnection> peerConnectionsMap = new HashMap<>();
	
	private PluginCallbacks.FeaturesAvailable FEATURE_IN_USE;
	private RipplePlugin                      ripplePlugin;
	
	
	public RippleApp(String serverUrl, PluginCallbacks.RootPluginCallBacks rootPluginCallBacks, PluginCallbacks.FeaturesAvailable feature) {
		serverUrl = RippleUtils.convertToWebSocketUrl(serverUrl);
		if (serverUrl.endsWith("/")) {
			serverUrl = serverUrl + "websocket/client";
		} else {
			serverUrl = serverUrl + "/websocket/client";
		}
		this.serverUrl = serverUrl;
		this.FEATURE_IN_USE = feature;
		String uniqueID = RippleUtils.uniqueIDGenerator(RippleUtils.nonAlphaNumeric(System.getProperty("user.name")), 22);
		this.clientID = RippleUtils.IdGenerator() + uniqueID;
		log.info("RippleApp initialized");
		log.info("serverUrl: " + serverUrl);
		log.info("clientID: " + clientID);
		this.rootPluginCallBacks = rootPluginCallBacks;
	}
	
	public void requestNewThread(String file) {
		JSONObject message = new JSONObject();
		message.put("requestType", "newThread");
		message.put("feature", this.FEATURE_IN_USE.toString());
		message.put("data", new JSONObject().put("file", file));
		message.put("transaction", RippleUtils.uniqueIDGenerator("transaction", 12));
		message.put("clientID", clientID);
		
	}
	
	private void startToRemindServerOfMe() {
		JSONObject message = new JSONObject();
		message.put("requestType", "remember");
		message.put("clientID", clientID);
		perTaskExecutor.submit(() -> {
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
	
	public void onMessage(final String message) {
		log.info("Received message: " + message);
		final JSONObject messageObject = new JSONObject(message);
		String           eventType     = messageObject.optString("eventType", null);
		boolean          success       = messageObject.optBoolean("success", false);
		JSONObject       plugin        = messageObject.optJSONObject("plugin", null);
		
		if (success && plugin != null) {
			String pluginEventType = plugin.optString("eventType", null);
			if (pluginEventType == null) {
				return;
			}
			if (pluginEventType.equals("webrtc")) {
				setRemoteOfferStringSdp(plugin.getInt("threadRef"), plugin.getString("sdp"));
			}
		}
		if (success && eventType != null) {
			switch (eventType) {
				case "newThread":
					if (FEATURE_IN_USE == PluginCallbacks.FeaturesAvailable.G_STREAM) {
						if (ripplePlugin != null) {
							if (plugin.has("position")) {
								int       threadRef = plugin.getInt("threadRef");
								VideoView videoView = ripplePlugin.addThread(threadRef);
								if (ripplePlugin instanceof RippleGstreamerPlugin) {
									
									peerConnectionsMap.put(threadRef, ((RippleGstreamerPlugin) ripplePlugin).startBroadCast(threadRef));
									
								}
								
								
							}
							
						}
					}
					
					break;
				case "register":
					System.out.println("Registered");
					if (FEATURE_IN_USE == PluginCallbacks.FeaturesAvailable.G_STREAM) {
						ripplePlugin = new RippleGstreamerPlugin(this);
					}
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
	
	@NonBlocking
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
		
		message.put("transaction", RippleUtils.uniqueIDGenerator("transaction", 12));
		message.put("clientID", clientID);
		log.info("Sending message");
		log.info(message.toString());
//		webSocket.send(message.toString());
		perTaskExecutor.submit(() -> webSocket.send(message.toString()));
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
	
	
	@Override
	public void IceCandidate(String message) {
		JSONObject messageObject = new JSONObject(message);
		messageObject.put("requestType", "iceCandidate");
		sendMessage(messageObject);
	}
	
	@Override
	public void onTrack(MediaStreamTrack track) {
	
	}
	
	@Override
	public void onTrack(MediaStreamTrack track, int threadRef) {
		if (track == null) {
			return;
		}
		if (ripplePlugin == null) {
			return;
		}
		if (FEATURE_IN_USE == PluginCallbacks.FeaturesAvailable.G_STREAM && ripplePlugin instanceof RippleGstreamerPlugin) {
			
			((RippleGstreamerPlugin) ripplePlugin).onTrack(track, threadRef);
		}
		
	}
	
	@Override
	@Nullable
	@NonBlocking
	public void notify(@Nullable String jsonMessage) {
		if (jsonMessage == null) {
			return;
		}
		if (jsonMessage.isEmpty()) {
			return;
		}
		JSONObject message = new JSONObject(jsonMessage);
		sendMessage(message);
	}
	
	
}
