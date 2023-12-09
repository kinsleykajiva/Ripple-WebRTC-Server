package africa.jopen.ripple.utils;

import africa.jopen.ripple.exceptions.ClientException;
import africa.jopen.ripple.models.Client;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Version;

import java.util.Optional;
import java.util.logging.Logger;

public class ConnectionsManager {
	private static final Logger LOGGER = Logger.getLogger(ConnectionsManager.class.getName());
	
	static {
		LoggerConfig.setupLogger(LOGGER);
	}
	
	private static final MutableList<Client> CLIENTS = Lists.mutable.empty();
	
	private ConnectionsManager() {
		LOGGER.info("started now ConnectionsManager");
		GStreamerUtils.configurePaths();
		Gst.init(Version.of(1, 16));
	}
	
	private static class Holder {
		private static final ConnectionsManager INSTANCE = new ConnectionsManager();
	}
	
	public static ConnectionsManager getInstance() {
		return Holder.INSTANCE;
	}
	
	public boolean checkIfClientExists(String id) {
		
		return CLIENTS.anySatisfy(client -> client.getClientID().equals(id));
	}
	
	public Optional<Client> getClient(String id) {
		//  check if client exists using the client d property
		return CLIENTS.select(client -> client.getClientID().equals(id)).stream().findFirst();
	}
	
	
	
	
	
	
}
