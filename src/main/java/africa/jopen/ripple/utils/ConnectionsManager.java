package africa.jopen.ripple.utils;

import africa.jopen.ripple.exceptions.ClientException;
import africa.jopen.ripple.models.Client;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Version;

import java.util.Optional;
import java.util.logging.Logger;

import static africa.jopen.ripple.utils.BannerTxT.BANNER_TEXT;

public class ConnectionsManager {
	private static final Logger LOGGER = Logger.getLogger(ConnectionsManager.class.getName());
	
	static {
		LoggerConfig.setupLogger(LOGGER);
	}
	
	private static final MutableList<Client> CLIENTS = Lists.mutable.empty();
	
	private ConnectionsManager() {
		GStreamerUtils.configurePaths();
		Gst.init(Version.of(1, 16));
	}
	public void setBANNER(){
		LOGGER.info(BANNER_TEXT);
		LOGGER.warning("FEATURES " );
		LOGGER.severe("       *WebRTC " );
		LOGGER.severe("       *GStreamer with version " + Gst.getVersionString() );
		LOGGER.severe("       *Java version " + System.getProperty("java.version") );
		
	}
	
	private static class Holder {
		private static final ConnectionsManager INSTANCE = new ConnectionsManager();
	}
	
	public static ConnectionsManager getInstance() {
		return Holder.INSTANCE;
	}
	
	
	public Optional<Client> getClient(String id) {
		//  check if client exists using the client d property
		return CLIENTS.select(client -> client.getClientID().equals(id)).stream().findFirst();
	}
	
	
	
	
	
	
}
