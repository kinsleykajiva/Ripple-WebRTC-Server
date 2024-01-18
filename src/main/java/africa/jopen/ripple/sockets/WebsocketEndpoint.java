package africa.jopen.ripple.sockets;


import africa.jopen.ripple.models.Client;
import africa.jopen.ripple.models.MediaFile;
import africa.jopen.ripple.plugins.WebRTCGStreamerPlugIn;
import africa.jopen.ripple.utils.JsonUtils;
import africa.jopen.ripple.utils.XUtils;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;
import org.apache.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class WebsocketEndpoint implements WsListener {
	
	static        Logger              log         = Logger.getLogger(WebsocketEndpoint.class.getName());
	//	private final MessageQueue messageQueue = MessageQueue.instance();
	private final MutableList<Client> clientsList = Lists.mutable.empty();
	private final Timer               timer       = new Timer();
	
	public void startOrphansCron() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					clientsList.removeIf(Client::isClientOrphan);
				} catch (Exception e) {
					log.error("Error: " + e.getMessage(), e);
				}
			}
		};
		
		long period = XUtils.MAIN_CONFIG_MODEL.session().rememberTimeOutInSeconds() == 0 ? 120 : XUtils.MAIN_CONFIG_MODEL.session().rememberTimeOutInSeconds();
		timer.scheduleAtFixedRate(task, 60 * 1000, period * 1000);
	}
	
	private Client getClientById( String clientID ) {
		return clientsList.detect(client -> client.getClientID().equals(clientID));
	}
	
	@Override
	public void onMessage( WsSession session, String text, boolean last ) {
		log.info("Received message: " + text);
		var transaction    = "";
		var isDebugSession = false;
		
		
		try {
			
			if (JsonUtils.isJson(text)) {
				JSONObject jsonObject = new JSONObject(text);
				if (jsonObject.has("transaction")) {
					transaction = jsonObject.getString("transaction");
				}
				if (jsonObject.has("isDebugSession")) {
					isDebugSession = jsonObject.getBoolean("isDebugSession");
				}
				Client                client = null;
				WebRTCGStreamerPlugIn plugin;
				if (jsonObject.has("clientID")) {
					client = getClientById(jsonObject.getString("clientID"));
					
					switch (jsonObject.getString("requestType")) {
						case "offer":
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							break;
						case "decreaseVolume":
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							
							if (!jsonObject.has("threadRef")) {
								log.info("Required threadRef not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
							if (Objects.isNull(plugin)) {
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef Value not found,Reference error")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								break;
							}
							plugin.setTransaction(transaction);
							plugin.decreaseVolume();
							break;
						
						case "increaseVolume":
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							
							
							if (!jsonObject.has("threadRef")) {
								log.info("Required threadRef not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
							if (Objects.isNull(plugin)) {
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef Value not found,Reference error")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								break;
							}
							plugin.setTransaction(transaction);
							plugin.increaseVolume();
							break;
						
						
						case "pause":
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							
							if (!jsonObject.has("threadRef")) {
								log.info("Required threadRef not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
							if (Objects.isNull(plugin)) {
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef Value not found,Reference error")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								break;
							}
							plugin.setTransaction(transaction);
							plugin.pauseTransmission();
							break;
						case "resume":
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							if (!jsonObject.has("threadRef")) {
								log.info("Required threadRef not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
							if (Objects.isNull(plugin)) {
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef Value not found,Reference error")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								break;
							}
							plugin.setTransaction(transaction);
							plugin.resumeTransmission();
							break;
						case "iceCandidate":
							
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							if (!jsonObject.has("candidate")) {
								log.info("Required candidate not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required candidate not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							if (!jsonObject.has("sdpMLineIndex")) {
								log.info("Required sdpMLineIndex not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required sdpMLineIndex not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							
							if (!jsonObject.has("threadRef")) {
								log.info("Required threadRef not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
							if (Objects.isNull(plugin)) {
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef Value not found,Reference error")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								break;
							}
							plugin.setTransaction(transaction);
							plugin.handleIceSdp(jsonObject.getString("candidate"), jsonObject.getInt("sdpMLineIndex"));
							
							break;
						
						
						case "answer":
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							if (!jsonObject.has("answer")) {
								log.info("Required answer not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required answer not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							
							
							if (!jsonObject.has("threadRef")) {
								log.info("Required threadRef not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
							
							if (Objects.isNull(plugin)) {
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef Value not found,Reference error")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								break;
							}
							plugin.setTransaction(transaction);
							plugin.handleSdp(jsonObject.getString("answer"));
							break;
						case "startBroadCast":
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							if (!jsonObject.has("threadRef")) {
								log.info("Required threadRef not found,Invalid request");
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef not found,Invalid request")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								
								break;
							}
							plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
							if (Objects.isNull(plugin)) {
								session.send(
										new JSONObject()
												.put("success", false)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("message", "Required threadRef Value not found,Reference error")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								break;
							}
							plugin.setTransaction(transaction);
							plugin.startCall();
							break;
						case "newThread":
							if (Objects.isNull(client)) {
								log.info("Client not found");
								break;
							}
							client.setDebugSession(isDebugSession);
							int thread = 0;
							JSONObject reqstData = jsonObject.getJSONObject("data");
							if (jsonObject.getString("feature").equals("G_STREAM_BROADCASTER")) {
								
								var filePath = XUtils.MAIN_CONFIG_MODEL.storagePath().media() + reqstData.getString("file");
								
								if (!new File(filePath).exists()) {
									log.info("File not found: " + filePath);
									
									client.replyToNewThreadInvalidRequest(transaction, -1);
									break;
									
								}
								thread = client.createAccessGStreamerPlugIn(
										new MediaFile(filePath
												, 0));
								
							}
							client.replyToNewThreadRequest(transaction, thread);
							break;
						case "remember":
							log.info("client: " + client);
							if (Objects.nonNull(client)) {
								client.setDebugSession(isDebugSession);
								//client.replyToRemembering(transaction);
							} else {
								log.info("Client not found");
							}
							break;
						case "register":
							boolean isRegistered = clientsList.stream()
									.anyMatch(client1 -> client1.getClientID().equals(jsonObject.getString("clientID")));
							if (isRegistered) {
								log.info("Client already registered,maybe this client is reconnecting so let's update the session");
								client = getClientById(jsonObject.getString("clientID"));
								// check if the socket was closed
								if (client.getWsSession() == null) {
									log.info("Client socket was closed,let's update the session");
									client.setWsSession(session);
								}
								
								session.send(
										new JSONObject()
												.put("success", true)
												.put("eventType", jsonObject.getString("requestType"))
												.put("accessAuth", "GENERAL")
												.put("transaction", transaction)
												.put("clientID", jsonObject.getString("clientID"))
												.toString()
										, last);
								break;
							}
							client = new Client(jsonObject.getString("clientID"));
							client.setWsSession(session);
							client.setDebugSession(isDebugSession);
							
							clientsList.add(client);
							
							session.send(
									new JSONObject()
											.put("success", true)
											.put("eventType", jsonObject.getString("requestType"))
											.put("accessAuth", "GENERAL")
											.put("transaction", transaction)
											.put("clientID", client.getClientID())
											.toString()
									, last);
							break;
						default:
							log.info("Unknown request type: " + jsonObject.getString("requestType"));
							
							break;
					}
					
				} else {
					log.info("Received message has no clientID");
					session.send(
							new JSONObject()
									.put("success", false)
									.put("accessAuth", "DENIED")
									.toString()
							, last);
				}
				
			} else {
				log.info("Received message is not json");
			}
			// Send all messages in the queue
		/*if (text.equals("send")) {
			while (!messageQueue.isEmpty()) {
				session.send(messageQueue.pop(), last);
			}
		}*/
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error: " + e.getMessage());
		}
	}
	
	@Override
	public void onClose( WsSession session, int status, String reason ) {
		log.info("Session closed: " + session);
		log.info("Session reason: " + reason);
		clientsList.detect(client -> client.getWsSession().equals(session)).setWsSession(null);
		WsListener.super.onClose(session, status, reason);
	}
	
	@Override
	public void onError( WsSession session, Throwable t ) {
		log.trace("Session error: " + session, t);
		WsListener.super.onError(session, t);
	}
	
	@Override
	public void onOpen( WsSession session ) {
		
		log.info("Session opened: " + session);
		WsListener.super.onOpen(session);
	}
}
