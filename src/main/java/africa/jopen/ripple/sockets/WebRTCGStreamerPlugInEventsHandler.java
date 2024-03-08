package africa.jopen.ripple.sockets;

import africa.jopen.ripple.models.Client;
import africa.jopen.ripple.models.MediaFile;
import africa.jopen.ripple.plugins.WebRTCGStreamerPlugIn;
import africa.jopen.ripple.utils.XUtils;
import io.helidon.websocket.WsSession;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;

public class WebRTCGStreamerPlugInEventsHandler {
	static Logger log = Logger.getLogger(WebRTCGStreamerPlugInEventsHandler.class.getName());
	
	
	static void answer( WsSession session, Client client,
	                    JSONObject jsonObject, String transaction, boolean last ) {
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
			
			return;
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
		plugin.handleSdp(jsonObject.getString("answer"));
	}
	
	static void iceCandidate( WsSession session, Client client,
	                          JSONObject jsonObject, String transaction, boolean last ) {
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
			
			return;
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
			
			return;
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
		plugin.handleIceSdp(jsonObject.getString("candidate"), jsonObject.getInt("sdpMLineIndex"));
		
	}
	
	static void newThread( WsSession session, Client client,
	                       JSONObject jsonObject, String transaction, boolean last ) {
		
		int        thread    = 0;
		JSONObject reqstData = jsonObject.getJSONObject("data");
		if (jsonObject.getString("feature").equals("G_STREAM_BROADCASTER")) {
			
			var filePath = XUtils.MAIN_CONFIG_MODEL.storagePath().media() + reqstData.getString("file");
			
			if (!new File(filePath).exists()) {
				log.info("File not found: " + filePath);
				
				client.replyToNewThreadInvalidRequest(transaction, -1);
				return;
				
			}
			thread = client.createAccessGStreamerPlugIn(
					new MediaFile(filePath
							, 0));
			
		}
		client.replyToNewThreadRequest(transaction, thread, jsonObject.getString("feature"));
	}
	
	static void startBroadCast( WsSession session, Client client,
	                            JSONObject jsonObject, String transaction, boolean last ) {
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
		plugin.startCall();
	}
	
	static void resume( WsSession session, Client client,
	                    JSONObject jsonObject, String transaction, boolean last ) {
		
		
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
	
	static void decreaseVolume( WsSession session, Client client,
	                            JSONObject jsonObject, String transaction, boolean last ) {
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
	
	static void increaseVolume( WsSession session, Client client,
	                            JSONObject jsonObject, String transaction, boolean last ) {
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
	
	static void pause( WsSession session, Client client,
	                   JSONObject jsonObject, String transaction, boolean last ) {
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
