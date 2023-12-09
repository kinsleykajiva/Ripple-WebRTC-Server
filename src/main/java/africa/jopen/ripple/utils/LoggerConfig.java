package africa.jopen.ripple.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class LoggerConfig {
	private static Thread logMaintenanceThread = null;
	
	public static void setupLogger(Logger logger) {
		logger.setUseParentHandlers(false);
		ConsoleHandler handler = new ColoredConsoleHandler();
		logger.addHandler(handler);
		/*try {
		RE, "Failed to setup logger handler", e);
		}*/
		
	}
}