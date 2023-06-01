package africa.jopen.models;

import africa.jopen.utils.XUtils;
import com.github.kokorin.jaffree.ffmpeg.*;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.github.kokorin.jaffree.ffprobe.UrlInput;
import com.google.common.flogger.FluentLogger;
import ws.schild.jave.Encoder;

import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class GStreamMediaResource {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private String title;
    String path;
    String codec;
    long duration;

    public GStreamMediaResource(String title, String path) {
        this.title = title;
        this.path = path;
        retrieveCodecInformation();
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public String getCodec() {
        return codec;
    }

    public long getDuration() {
        return duration;
    }

    private void retrieveCodecInformation() {
        //Todo still needs review as this will be passsed to the client
        try {
            if (XUtils.isURL(path)) {
                final AtomicLong durationMillis = new AtomicLong();
                FFmpeg.atPath()
                        .addInput((Input) UrlInput.fromUrl(path))
                        .addOutput(new NullOutput())
                        .setProgressListener(progress -> durationMillis.set(progress.getTimeMillis()))
                        .execute();

                System.out.println("Exact duration: " + durationMillis.get() + " milliseconds");
            } else {
                FFprobeResult result = FFprobe.atPath()
                        .setShowStreams(true)
                        .setInput(path)
                        .execute();

                result.getStreams().forEach(stream -> duration= (long) Double.parseDouble(stream.getDuration().toString()));
            }


        } catch (Exception e) {
            // Handle exception
            logger.atSevere().withCause(e).log("Failed to process");
        }
    }
}
