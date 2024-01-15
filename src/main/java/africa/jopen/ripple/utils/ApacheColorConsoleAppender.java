package africa.jopen.ripple.utils;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import java.io.Writer;

public class ApacheColorConsoleAppender extends ConsoleAppender {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";

    @Override
    public void append(LoggingEvent event) {
        Writer writer = super.qw;
        if(event.getLevel().equals(Level.ERROR)) {
            // If the log level is ERROR, add the ANSI escape code for red color to the log message
            try {
                writer.write(ANSI_RED);
                super.append(event);
                writer.write(ANSI_RESET);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            super.append(event);
        }
    }
}