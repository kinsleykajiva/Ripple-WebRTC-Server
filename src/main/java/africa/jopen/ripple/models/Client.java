package africa.jopen.ripple.models;

import africa.jopen.ripple.interfaces.CommonAbout;
import africa.jopen.ripple.plugins.FeatureTypes;
import africa.jopen.ripple.plugins.SipUserAgentPlugin;
import africa.jopen.ripple.plugins.WebRTCGStreamerPlugIn;
import africa.jopen.ripple.utils.XUtils;
import io.helidon.websocket.WsSession;
import org.apache.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.json.JSONObject;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Client implements CommonAbout {
	private final String clientID;
	private final MutableList<String> transactions = Lists.mutable.empty();
	private long lastTimeStamp = System.currentTimeMillis();
	private boolean isDebugSession = false;
	private final Logger log = Logger.getLogger(Client.class.getName());
	
	private final MutableMap<Integer, WebRTCGStreamerPlugIn> webRTCStreamMap = Maps.mutable.empty();
	private final MutableMap<Integer, SipUserAgentPlugin>    sipUserAgentPluginMap = Maps.mutable.empty();
	private WsSession                                        wsSession;
	
	private final MutableList<String> payloads = Lists.mutable.empty();
	
	public Client(String clientID) {
		// Refactored to use ternary operator for better readability
		this.clientID = clientID == null || clientID.equals("null") || clientID.isEmpty() ? XUtils.IdGenerator() : clientID;
	}
	
	public void setDebugSession(boolean debugSession) {
		isDebugSession = debugSession;
	}
	
	public int createAccessSipUserPlugIn(String realm,String username,String displayName,String password,String host,int port ) {
		int position = sipUserAgentPluginMap.size() + 1;
		
		SipUserAgentPlugin sipUserAgentPlugin = new SipUserAgentPlugin(this, position,
				realm, username, displayName, password, host, port);
		sipUserAgentPluginMap.put(position, sipUserAgentPlugin);
		return position;
	}
	public int createAccessGStreamerPlugIn(MediaFile mediaFile) {
		int position = webRTCStreamMap.size() + 1;
		WebRTCGStreamerPlugIn gStreamerPlugIn = new WebRTCGStreamerPlugIn(this, position, mediaFile);
		webRTCStreamMap.put(position, gStreamerPlugIn);
		return position;
	}
	
	public MutableMap<Integer, WebRTCGStreamerPlugIn> getWebRTCStreamMap() {
		return webRTCStreamMap;
	}
	
	public String getClientID() {
		return clientID;
	}
	
	/**
	 * Sets the WebSocket session for this client.
	 * If the session is not null, iterates over the payloads list and attempts to send each payload.
	 * After a payload is successfully sent, it is removed from the list.
	 *
	 * @param wsSession The WebSocket session to be set for this client.
	 */
	public void setWsSession(WsSession wsSession) {
		this.wsSession = wsSession;
		if (wsSession != null) {
			
			Iterator<String> iterator = payloads.iterator();
			while (iterator.hasNext()) {
				String payload = iterator.next();
				// Attempt to send the payload
				attemptToSendMessage(payload, false);
				// If the payload was successfully sent, remove it from the list
				iterator.remove();
			}
		}
	}
	
	public MutableList<String> getTransactions() {
		return transactions;
	}
	
	public long getLastTimeStamp() {
		return lastTimeStamp;
	}
	
	public WsSession getWsSession() {
		return wsSession;
	}
	
	public synchronized void updateLastTimeStamp(long newTime) {
		if (newTime < lastTimeStamp) {
			throw new IllegalArgumentException("newTime cannot be less than lastTimeStamp");
		}
		lastTimeStamp = newTime;
	}
	
	/**
	 * Checks if the client is considered an orphan.
	 * A client is considered an orphan if the last timestamp when the client was active is older than a certain timeout.
	 * The timeout is either 2 minutes or a value set in the configuration.
	 *
	 * @return true if the client is an orphan, false otherwise.
	 */
	public boolean isClientOrphan() {
		Instant lastActiveTime = Instant.ofEpochMilli(lastTimeStamp);
		Duration durationSinceLastActive = Duration.between(lastActiveTime, Instant.now());
		// Refactored to simplify the condition using ternary operator and method extraction
		int rememberTimeOutInSeconds = XUtils.MAIN_CONFIG_MODEL.session().rememberTimeOutInSeconds();
		return isClientOrphanBasedOnTimeout(durationSinceLastActive, rememberTimeOutInSeconds);
	}
	
	// Extracted method for better readability
	private boolean isClientOrphanBasedOnTimeout(Duration durationSinceLastActive, int rememberTimeOutInSeconds) {
		return rememberTimeOutInSeconds == 0 ? durationSinceLastActive.getSeconds() > 120 : durationSinceLastActive.getSeconds() > rememberTimeOutInSeconds;
	}
	
	@Override
	public void onUpdateLastTimeStamp(long timeStamp) {
		updateLastTimeStamp(timeStamp);
	}
	
	@Override
	public void sendMessage(JSONObject pluginData, Integer objectPosition, FeatureTypes featureType) {
		// Refactored to use object initialization with put method chaining
		JSONObject response = new JSONObject()
				.put("clientID", clientID)
				.put("success", true)
				.put("feature", featureType.toString())
				.put("position", objectPosition)
				.put("plugin", pluginData)
				.put("accessAuth", "GENERAL")
				.put("lastSeen", lastTimeStamp);
		
		attemptToSendMessage(response);
		if (isDebugSession) {
			log.info(pluginData.toString());
		}
	}
	
	private void attemptToSendMessage(JSONObject jsonObject) {
		attemptToSendMessage(jsonObject.toString(), false);
	}
	
	/**
	 * Attempts to send a message to the client. If the WebSocket session is not available,
	 * the message is stored in a list to be sent later when the session becomes available.
	 *
	 * @param jsonObject   The message to be sent to the client. This is a JSON string.
	 * @param ignoreToSend If true, the method will not add the message to the payloads list
	 *                     if the WebSocket session is not available. This is useful to avoid
	 *                     storing duplicate messages in the payloads list.
	 * @throws UncheckedIOException If an I/O error occurs when trying to send the message.
	 */
	private void attemptToSendMessage(String jsonObject, boolean ignoreToSend) {
		try {
			if (wsSession != null) {
				wsSession.send(jsonObject, true);
			} else {
				if (!ignoreToSend) {
					payloads.add(jsonObject);
				}
			}
		} catch (UncheckedIOException e) {
			log.error(e.getMessage());
			if (!ignoreToSend) {
				payloads.add(jsonObject);
			}
		}
	}
	
	public void replyToRemembering(String transaction) {
		Instant now = Instant.now();
		onUpdateLastTimeStamp(now.toEpochMilli());
		// Refactored to use object initialization with put method chaining
		JSONObject response = new JSONObject()
				.put("clientID", clientID)
				.put("success", true)
				.put("eventType", "remember")
				.put("transaction", transaction)
				.put("accessAuth", "GENERAL")
				.put("lastSeen", lastTimeStamp);
		
		attemptToSendMessage(response);
		if (isDebugSession) {
			log.info(response.toString());
		}
	}
	
	public void replyToNewThreadInvalidRequest(String transaction, int position) {
		Instant now = Instant.now();
		onUpdateLastTimeStamp(now.toEpochMilli());
		// Refactored to use object initialization with put method chaining
		JSONObject response = new JSONObject()
				.put("clientID", clientID)
				.put("success", false)
				.put("message", "Invalid request")
				.put("eventType", "newThread")
				.put("transaction", transaction)
				.put("position", position)
				.put("accessAuth", "GENERAL")
				.put("lastSeen", lastTimeStamp);
		
		attemptToSendMessage(response);
		if (isDebugSession) {
			log.info(response.toString());
		}
	}
	
	public void replyToNewThreadRequest(String transaction, int position, String feature) {
		Instant now = Instant.now();
		onUpdateLastTimeStamp(now.toEpochMilli());
		// Refactored to use object initialization with put method chaining
		JSONObject response = new JSONObject()
				.put("clientID", clientID)
				.put("success", true)
				.put("eventType", "newThread")
				.put("feature", feature)
				.put("transaction", transaction)
				.put("position", position)
				.put("accessAuth", "GENERAL")
				.put("lastSeen", lastTimeStamp);
		
		attemptToSendMessage(response);
		if (isDebugSession) {
			log.info(response.toString());
		}
	}
	
	public void replyToInvalidRequest(JSONObject jsonObject) {
		Instant now = Instant.now();
		onUpdateLastTimeStamp(now.toEpochMilli());
		JSONObject response = new JSONObject();
		
		// Refactored to use method reference and lambda expression
		handleMissingField(jsonObject, response, "transaction", "transaction is required");
		handleMissingField(jsonObject, response, "requestType", "requestType is required");
		
		attemptToSendMessage(response.put("clientID", clientID));
		if (isDebugSession) {
			log.info(response.toString());
		}
	}
	
	// Refactored to use Predicate functional interface
	private void handleMissingField(JSONObject jsonObject, JSONObject response, String fieldName, String errorMessage) {
		Predicate<JSONObject> missingField = json -> !json.has(fieldName);
		if (missingField.test(jsonObject)) {
			response.put("error", errorMessage);
		}
	}
}