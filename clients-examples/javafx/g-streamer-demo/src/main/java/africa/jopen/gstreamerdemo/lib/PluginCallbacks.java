package africa.jopen.gstreamerdemo.lib;

public class PluginCallbacks {
	
	public interface RootPluginCallBacks{
		void onSocketClosed();
		void onSocketConnected();
		void onSocketError(Throwable t);
		void onSocketMessage(String message);
		void webRTCEvents(String message);
	}
	public interface GstreamerPluginCallBack extends RootPluginCallBacks{
		void onStreamUIUpdates(String message);
	}
	
}
