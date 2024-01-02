package africa.jopen.ripple.sockets;


import africa.jopen.ripple.models.Client;
import africa.jopen.ripple.utils.JsonUtils;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;
import org.apache.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.json.JSONObject;

import java.util.Objects;

public class WebsocketEndpoint implements WsListener {
	
	static        Logger              log         = Logger.getLogger(WebsocketEndpoint.class.getName());
	//	private final MessageQueue messageQueue = MessageQueue.instance();
	private final MutableList<Client> clientsList = Lists.mutable.empty();
	
	@Override
	public void onMessage(WsSession session, String text, boolean last) {
		log.info("Received message: " + text);
		var transaction    = "";
		var isDebugSession = false;
		
		if (JsonUtils.isJson(text)) {
			JSONObject jsonObject = new JSONObject(text);
			if (jsonObject.has("transaction")) {
				transaction = jsonObject.getString("transaction");
			}
			if (jsonObject.has("isDebugSession")) {
				isDebugSession = jsonObject.getBoolean("isDebugSession");
			}
			Client client = null;
			if (jsonObject.has("clientID")) {
				switch (jsonObject.getString("requestType")) {
					case "remember":
						client = clientsList.stream()
								.peek(client1 -> log.info("peek-client: " + client1.getClientID()))
								.filter(client1 -> client1.getClientID().equals(jsonObject.getString("clientID")))
								.findFirst().orElse(null);
						log.info("client: " + client);
						if (Objects.nonNull(client)) {
							client.setDebugSession(isDebugSession);
							client.replyToRemembering(transaction);
						} else {
							log.info("Client not found");
						}
						break;
					case "register":
						boolean isRegistered = clientsList.stream()
								.anyMatch(client1 -> client1.getClientID().equals(jsonObject.getString("clientID")));
						if(isRegistered){
							log.info("Client already registered");
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
				/*String clientID = jsonObject.getString("clientID");
					if (jsonObject.getString("feature").equals(G_STREAM)) {
						var    client = clientsList.detect(client1 -> client1.getClientID().equals(clientID));
						String file   = jsonObject.getString("file");
						if (client.getWebRTCStreamMap().isEmpty()) {
							log.info("Client has no stream");
							client.createAccessGStreamerPlugIn(new MediaFile(file, 0));
						}
						if (Objects.nonNull(client)) {
							client.sendMessage(jsonObject, jsonObject.getInt("handleId"));
						}
						
					}*/
				
				
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
	}
	
	@Override
	public void onClose(WsSession session, int status, String reason) {
		log.info("Session closed: " + session);
		log.info("Session reason: " + reason);
		WsListener.super.onClose(session, status, reason);
	}
	
	@Override
	public void onError(WsSession session, Throwable t) {
		log.trace("Session error: " + session, t);
		WsListener.super.onError(session, t);
	}
	
	@Override
	public void onOpen(WsSession session) {
		log.info("Session opened: " + session);
		WsListener.super.onOpen(session);
	}
}
