package africa.jopen.ripple.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.*;



public class LoggerConfig {

	
	/*public static void setupLogger(Logger logger) {
		logger.setUseParentHandlers(false);
		ConsoleHandler handler = new ColoredConsoleHandler();
		logger.addHandler(handler);
		
		
	}*/
	
	
	public static void setupLogger(Logger logger) {
		logger.setUseParentHandlers(false);
		Handler consoleHandler = new ColoredConsoleHandler();
		consoleHandler.setFormatter(new SimpleFormatter() {
			private static final String format = "%5$s%6$s%n";
			
			@Override
			public synchronized String format(LogRecord lr) {
				return String.format(format,
						lr.getSourceClassName(),
						lr.getSourceMethodName(),
						lr.getLoggerName(),
						lr.getLevel().getLocalizedName(),
						lr.getMessage(),
						lr.getThrown() == null ? "" : '\n' + getStackTrace(lr.getThrown()));
			}
		});
		logger.addHandler(consoleHandler);
	}
	
	private static String getStackTrace(Throwable thrown) {
		StringWriter sw = new StringWriter();
		PrintWriter  pw = new PrintWriter(sw);
		thrown.printStackTrace(pw);
		return sw.toString();
	}
}