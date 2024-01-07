package africa.jopen.ripple.models;

import africa.jopen.ripple.interfaces.CommonAbout;
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
import java.util.Iterator;

public class Client implements CommonAbout {
	private       String              clientID;
	private final MutableList<String> transactions   = Lists.mutable.empty();
	private       long                lastTimeStamp  = System.currentTimeMillis();
	private       boolean             isDebugSession = false;
	private       Logger              log            = Logger.getLogger(Client.class.getName());
	
	private final MutableMap<Integer, WebRTCGStreamerPlugIn> webRTCStreamMap = Maps.mutable.empty();
	
	public Client(String clientID) {
		if (clientID == null || clientID.equals("null") || clientID.isEmpty()) {
			this.clientID = XUtils.IdGenerator();
		} else {
			this.clientID = clientID;
		}
		
	}
	
	public void setDebugSession(boolean debugSession) {
		isDebugSession = debugSession;
	}
	
	public int createAccessGStreamerPlugIn(MediaFile mediaFile) {
		var position        = webRTCStreamMap.size() + 1;
		var gStreamerPlugIn = new WebRTCGStreamerPlugIn(this, position, mediaFile);
		webRTCStreamMap.put(position, gStreamerPlugIn);
		return position;
	}
	
	
	public MutableMap<Integer, WebRTCGStreamerPlugIn> getWebRTCStreamMap() {
		return webRTCStreamMap;
	}
	
	private WsSession wsSession;
	
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
			// Iterate over the payloads using an Iterator
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
	
	public MutableList getTransactions() {
		return transactions;
	}
	
	private MutableList<String> payloads = Lists.mutable.empty();
	
	public long getLastTimeStamp() {
		return lastTimeStamp;
	}
	
	public WsSession getWsSession() {
		return wsSession;
	}
	
	public void updateLastTimeStamp(long newTime) {
		lastTimeStamp = newTime;
	}
	
	/**
	 * Checks if the client is considered an orphan.
	 * A client is considered an orphan if the last timestamp when the client was active is older than a certain timeout.
	 * The timeout is either 2 minutes or a value set in the configuration.
	 *
	 * @return true if the client is an orphan, false otherwise.
	 */
	public boolean isClientOrphan(){
		// test if lastTimeStamp is older than 2 mins or whats set in the config
		if(XUtils.MAIN_CONFIG_MODEL.session().rememberTimeOutInSeconds() == 0){
			return (System.currentTimeMillis() - lastTimeStamp) > 120 * 1_000L;
		}
		return (System.currentTimeMillis() - lastTimeStamp) > XUtils.MAIN_CONFIG_MODEL.session().rememberTimeOutInSeconds() * 1_000L;
	}
	
	@Override
	public void onUpdateLastTimeStamp(final long timeStamp) {
		updateLastTimeStamp(timeStamp);
	}
	
	
	@Override
	public void sendMessage(final JSONObject pluginData, final Integer objectPosition) {
		var response = new JSONObject();
		response.put("clientID", clientID);
		response.put("success", true);
		response.put("position", objectPosition);
		response.put("plugin", pluginData);
		response.put("accessAuth", "GENERAL");
		response.put("lastSeen", lastTimeStamp);
		
		attemptToSendMessage(response);
		if (isDebugSession) {
			log.info(pluginData.toString());
		}
	}
	
	/**
	 * Attempts to send a message to the client. If the WebSocket session is not available,
	 * the message is stored in a list to be sent later when the session becomes available.
	 *
	 * @param jsonObject The message to be sent to the client. This is a JSON object.
	 * @throws UncheckedIOException If an I/O error occurs when trying to send the message.
	 */
	private void attemptToSendMessage(JSONObject jsonObject) {
		try {
			if (wsSession != null) {
				wsSession.send(jsonObject.toString(), true);
			} else {
				payloads.add(jsonObject.toString());
			}
		} catch (UncheckedIOException e) {
			log.error(e.getMessage());
			payloads.add(jsonObject.toString());
		}
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
		onUpdateLastTimeStamp(System.currentTimeMillis());
		var response = new JSONObject();
		response.put("clientID", clientID);
		response.put("success", true);
		response.put("eventType", "remember");
		response.put("transaction", transaction);
		response.put("accessAuth", "GENERAL");
		response.put("lastSeen", lastTimeStamp);
		attemptToSendMessage(response);
		if (isDebugSession) {
			log.info(response.toString());
		}
	}
	
	public void replyToNewThreadInvalidRequest(final String transaction, final int position) {
		onUpdateLastTimeStamp(System.currentTimeMillis());
		var response = new JSONObject();
		response.put("clientID", clientID);
		response.put("success", false);
		response.put("message", "Invalid request");
		response.put("eventType", "newThread");
		response.put("transaction", transaction);
		response.put("position", position);
		response.put("accessAuth", "GENERAL");
		response.put("lastSeen", lastTimeStamp);
		attemptToSendMessage(response);
		if (isDebugSession) {
			log.info(response.toString());
		}
	}
	
	public void replyToNewThreadRequest(final String transaction, final int position) {
		onUpdateLastTimeStamp(System.currentTimeMillis());
		var response = new JSONObject();
		response.put("clientID", clientID);
		response.put("success", true);
		response.put("eventType", "newThread");
		response.put("transaction", transaction);
		response.put("position", position);
		response.put("accessAuth", "GENERAL");
		response.put("lastSeen", lastTimeStamp);
		attemptToSendMessage(response);
		if (isDebugSession) {
			log.info(response.toString());
		}
	}
	
	
	public void replyToInvalidRequest(JSONObject jsonObject) {
		onUpdateLastTimeStamp(System.currentTimeMillis());
		var response = new JSONObject();
		response.put("clientID", clientID);
		if (!jsonObject.has("transaction")) {
			response.put("error", "transaction is required");
			attemptToSendMessage(response);
		}
		if (!jsonObject.has("requestType")) {
			response.put("error", "requestType is required");
			attemptToSendMessage(response);
			
		}
		if (isDebugSession) {
			log.info(response.toString());
		}
	}
}
