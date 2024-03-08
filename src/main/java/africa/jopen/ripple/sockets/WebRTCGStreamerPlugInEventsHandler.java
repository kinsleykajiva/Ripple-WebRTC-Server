package africa.jopen.ripple.sockets;

import africa.jopen.ripple.models.Client;
import africa.jopen.ripple.plugins.WebRTCGStreamerPlugIn;
import io.helidon.websocket.WsSession;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.Objects;

public class WebRTCGStreamerPlugInEventsHandler {
	static  Logger log = Logger.getLogger(WebRTCGStreamerPlugInEventsHandler.class.getName());
	private
	
	 static void resume( WsSession session ,Client client,
	                      JSONObject jsonObject, String transaction, boolean last) {
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
			
			return;
		}
		var plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
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
			return;
		}
		plugin.setTransaction(transaction);
		plugin.resumeTransmission();
	}
	 static void decreaseVolume( WsSession session ,Client client,
	                      JSONObject jsonObject, String transaction, boolean last) {
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
			
			return;
		}
		var plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
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
			return;
		}
		plugin.setTransaction(transaction);
		plugin.decreaseVolume();
	}
	 static void increaseVolume( WsSession session ,Client client,
	                      JSONObject jsonObject, String transaction, boolean last) {
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
			
			return;
		}
		var plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
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
			return;
		}
		plugin.setTransaction(transaction);
		plugin.increaseVolume();
	}
	
 static void pause( WsSession session ,Client client,
	                      JSONObject jsonObject, String transaction, boolean last) {
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
		
		return;
	}
		WebRTCGStreamerPlugIn plugin = client.getWebRTCStreamMap().get(jsonObject.getInt("threadRef"));
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
		return;
	}
	plugin.setTransaction(transaction);
	plugin.pauseTransmission();
}







}
