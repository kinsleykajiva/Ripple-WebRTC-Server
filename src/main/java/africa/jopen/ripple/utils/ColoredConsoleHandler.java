package africa.jopen.ripple.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.IntStream;

public class ColoredConsoleHandler extends ConsoleHandler {
    
    // ANSI escape code
    public static final String ANSI_RESET = "\u001B[0m";
    
    @Override
    public synchronized void publish(LogRecord record) {
        String message = getFormatter().formatMessage(record);
        if (!message.isEmpty()) {
            StringBuilder coloredMessage = new StringBuilder();
            int messageLength = message.length();
	        
	        IntStream.range(0, messageLength).forEach(i -> {
		        double partRatio         = (double) i / messageLength;
		        String interpolatedColor = interpolateColor(46, 131, 83, 162, 211, 71, partRatio);
		        coloredMessage.append(interpolatedColor).append(message.charAt(i));
	        });
            
            coloredMessage.append(ANSI_RESET);
            System.out.println(coloredMessage);
        }
    }
    
    // Interpolate between two RGB colors based on a ratio
    private String interpolateColor(final int startRed, final int startGreen, final int startBlue,
                                    final int endRed, final int endGreen, final int endBlue,final  double ratio) {
        int interpolatedRed = (int) (startRed + ratio * (endRed - startRed));
        int interpolatedGreen = (int) (startGreen + ratio * (endGreen - startGreen));
        int interpolatedBlue = (int) (startBlue + ratio * (endBlue - startBlue));
        
        return String.format("\u001B[38;2;%d;%d;%dm", interpolatedRed, interpolatedGreen, interpolatedBlue);
    }
}
