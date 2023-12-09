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
		
		/*if (logMaintenanceThread == null) {
			logMaintenanceThread = new Thread(() -> {
				
				while (true) {ntain log files", e);
									}
								});
						Thread.sleep(24 * 60 * 60 * 1000); // Sleep for a day
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Failed to maintain log files", e);
					}
				}
			});
			logMaintenanceThread.start();
		}*/
	}
}