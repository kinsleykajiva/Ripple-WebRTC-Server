package africa.jopen.ripple.models;

import africa.jopen.ripple.interfaces.CommonAbout;
import africa.jopen.ripple.plugins.WebRTCGStreamerPlugIn;
import africa.jopen.ripple.utils.XUtils;
import io.helidon.websocket.WsSession;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.json.JSONObject;

public class Client implements CommonAbout {
	private  String              clientID     ;
	private final MutableList<String> transactions  = Lists.mutable.empty();
	private       long                lastTimeStamp = System.currentTimeMillis();
	
	private final MutableMap<Integer, WebRTCGStreamerPlugIn> webRTCStreamMap = Maps.mutable.empty();
	
	public Client(String              clientID) {
		if(clientID == null || clientID.equals("null") || clientID.isEmpty()){
			this.clientID = XUtils.IdGenerator();
		}
	}
	
	public void createAccessGStreamerPlugIn(MediaFile mediaFile) {
		var position        = webRTCStreamMap.size() + 1;
		var gStreamerPlugIn = new WebRTCGStreamerPlugIn(this, position, mediaFile);
		webRTCStreamMap.put(position, gStreamerPlugIn);
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
		response.put("handleId", objectPosition);
		response.put("accessAuth", "GENERAL");
		response.put("lastSeen", lastTimeStamp);
		wsSession.send(pluginData.toString(), true);
		
		//	response = successResponse(XUtils.IdGenerator(), Events.EVENT_TYPE, Events.ICE_CANDIDATES_EVENT,"Ice Shared", response);
	}
}
