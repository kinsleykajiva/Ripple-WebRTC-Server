package africa.jopen.gstreamerdemo.lib;

public class PluginCallbacks {
	
	public interface RootPluginCallBacks{
		void onClientClosed();
		void onClientConnected();
		void onClientError(Throwable t);
		void onClientMessage(String message);
		void webRTCEvents(String message);
	}
	public interface GstreamerPluginCallBack extends RootPluginCallBacks{
		void onStreamUIUpdates(String message);
	}
	
}
