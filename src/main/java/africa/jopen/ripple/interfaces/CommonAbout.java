package africa.jopen.ripple.interfaces;

import org.json.JSONObject;

public interface CommonAbout {
	
	
	void onUpdateLastTimeStamp(final long timeStamp);
	void sendMessage(final JSONObject pluginData,final Integer objectPosition);
}
