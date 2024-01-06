package africa.jopen.ripple.models;

import africa.jopen.ripple.interfaces.CommonAbout;
import africa.jopen.ripple.plugins.WebRTCGStreamerPlugIn;
import africa.jopen.ripple.sockets.WebsocketEndpoint;
import africa.jopen.ripple.utils.XUtils;
import io.helidon.websocket.WsSession;
import org.apache.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.json.JSONObject;

public class Client implements CommonAbout {
	private  String              clientID     ;
	private final MutableList<String> transactions  = Lists.mutable.empty();
	private       long                lastTimeStamp = System.currentTimeMillis();
	private boolean isDebugSession =false;
	private Logger  log            = Logger.getLogger(Client.class.getName());
	
	private final MutableMap<Integer, WebRTCGStreamerPlugIn> webRTCStreamMap = Maps.mutable.empty();
	
	public Client(String              clientID) {
		if(clientID == null || clientID.equals("null") || clientID.isEmpty()){
			this.clientID = XUtils.IdGenerator();
		}else{
			this.clientID =clientID;
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
	
	
	public void setWsSession(WsSession wsSession) {
		this.wsSession = wsSession;
	}
	
	public MutableList getTransactions() {
		return transactions;
	}
	
	public long getLastTimeStamp() {
		return lastTimeStamp;
	}
	
	public WsSession getWsSession() {
		return wsSession;
	}
	
	public void updateLastTimeStamp(long newTime) {
		lastTimeStamp = newTime;
	}
	
	@Override
	public void onUpdateLastTimeStamp(final long timeStamp) {
		updateLastTimeStamp(timeStamp);
	}
	
	@Override
	public void sendMessage(final JSONObject pluginData, final Integer objectPosition) {
		//response.put("lastSeen", clientObject.get().lastTimeStamp());
		var response = new JSONObject();
		response.put("clientID", clientID);
		response.put("success", true);
		response.put("position", objectPosition);
		response.put("plugin", pluginData);
		response.put("accessAuth", "GENERAL");
		response.put("lastSeen", lastTimeStamp);
		wsSession.send(response.toString(), true);
		if(isDebugSession) {
			log.info(pluginData.toString());
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
		wsSession.send(response.toString(), true);
		if(isDebugSession) {
			log.info(response.toString());
		}
	}
	public void replyToNewThreadInvalidRequest(final String transaction,final int position) {
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
		wsSession.send(response.toString(), true);
		if(isDebugSession) {
			log.info(response.toString());
		}
	}
	public void replyToNewThreadRequest(final String transaction,final int position) {
		onUpdateLastTimeStamp(System.currentTimeMillis());
		var response = new JSONObject();
		response.put("clientID", clientID);
		response.put("success", true);
		response.put("eventType", "newThread");
		response.put("transaction", transaction);
		response.put("position", position);
		response.put("accessAuth", "GENERAL");
		response.put("lastSeen", lastTimeStamp);
		wsSession.send(response.toString(), true);
		if(isDebugSession) {
			log.info(response.toString());
		}
	}
	
	
	public void replyToInvalidRequest(JSONObject jsonObject) {
		onUpdateLastTimeStamp(System.currentTimeMillis());
		var response = new JSONObject();
		response.put("clientID", clientID);
		if(!jsonObject.has("transaction")){
			response.put("error", "transaction is required");
			wsSession.send(response.toString(), true);
		}
		if(!jsonObject.has("requestType")){
			response.put("error", "requestType is required");
			wsSession.send(response.toString(), true);
			
		}
		if(isDebugSession) {
			log.info(response.toString());
		}
	}
}
