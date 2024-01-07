package africa.jopen.ripple.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ColoredConsoleHandler extends ConsoleHandler {

    // ANSI escape code
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    
    
    

    @Override
    public synchronized void publish(LogRecord record) {
        if (!getFormatter().format(record).isEmpty()) {
            if (record.getLevel() == Level.SEVERE) {
                System.out.println(ANSI_GREEN + getFormatter().format(record) + ANSI_RESET);
            } else if (record.getLevel() == Level.WARNING) {
                System.out.println(ANSI_YELLOW + getFormatter().format(record) + ANSI_RESET);
            } else if (record.getLevel() == Level.INFO) {
//                System.out.println(ANSI_GREEN + getFormatter().format(record) + ANSI_RESET);
                System.out.println(ANSI_PURPLE + getFormatter().format(record) + ANSI_RESET);
            } else {
                System.out.println(ANSI_WHITE + getFormatter().format(record) + ANSI_RESET);
            }
        }
    }
}