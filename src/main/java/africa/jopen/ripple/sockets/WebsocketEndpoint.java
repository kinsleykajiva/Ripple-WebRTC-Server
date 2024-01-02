package africa.jopen.ripple.sockets;


import africa.jopen.ripple.models.Client;
import africa.jopen.ripple.models.MediaFile;
import africa.jopen.ripple.utils.JsonUtils;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;
import org.apache.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.json.JSONObject;

import java.util.Objects;

import static africa.jopen.ripple.plugins.FeatureTypes.G_STREAM;

public class WebsocketEndpoint implements WsListener {
	
	static        Logger              log         = Logger.getLogger(WebsocketEndpoint.class.getName());
	//	private final MessageQueue messageQueue = MessageQueue.instance();
	private final MutableList<Client> clientsList = Lists.mutable.empty();
	
	@Override
	public void onMessage(WsSession session, String text, boolean last) {
		log.info("Received message: " + text);
		log.info("Received message last : " + last);
		
		if (JsonUtils.isJson(text)) {
			JSONObject jsonObject = new JSONObject(text);
			if (jsonObject.has("clientID")) {
				if ((Objects.equals(jsonObject.getString("clientID"), "null")) && jsonObject.getString("eventType").equals("register")) {
					var client = new Client();
					client.setWsSession(session);
					
					clientsList.add(client);
					
					session.send(
							new JSONObject()
									.put("success", true)
									.put("accessAuth", "GENERAL")
									.put("clientID", client.getClientID())
									.toString()
							, last);
				} else {
					String clientID = jsonObject.getString("clientID");
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
						
					}
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
