package africa.jopen.ripple.abstractions;

import org.json.JSONObject;

public abstract class PluginAbs {
	public abstract void updateClientLastTimeStamp();
	
	/*
	*This is to used to notify the object in reference where this plugin in use
	* */
	protected abstract void notifyClient(JSONObject pluginData,final Integer objectPosition);
}
