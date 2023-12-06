package africa.jopen.ripple.models;

import africa.jopen.ripple.utils.XUtils;
import io.helidon.websocket.WsSession;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class Client {
	private final String      clientID     = XUtils.IdGenerator();
	private final MutableList transactions = Lists.mutable.empty();
	private long      lastTimeStamp     = System.currentTimeMillis();
	private WsSession socketSession;
	
	public String getClientID() {
		return clientID;
	}
	
	public MutableList getTransactions() {
		return transactions;
	}
	
	public long getLastTimeStamp() {
		return lastTimeStamp;
	}
	
	public WsSession getSocketSession() {
		return socketSession;
	}
	public void updateLastTimeStamp(long newTime) {
		lastTimeStamp = newTime;
	}
}
