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
        try {
            if (XUtils.isURL(path)) {
                final AtomicLong durationMillis = new AtomicLong();
                FFmpeg.atPath()
                        .addInput((Input) UrlInput.fromUrl(path))
                        //   .addInput(UrlInput.fromUrl(pathToSrc))

                        .addOutput(new NullOutput())
                        .setProgressListener(new ProgressListener() {
                            @Override
                            public void onProgress(FFmpegProgress progress) {

                                durationMillis.set(progress.getTimeMillis());
                            }
                        })
                        .execute();


                System.out.println("Exact duration: " + durationMillis.get() + " milliseconds");
            } else {
                FFprobeResult result = FFprobe.atPath()
                        .setShowStreams(true)
                        .setInput(path)
                        .execute();
                for (Stream stream : result.getStreams()) {

                    System.out.println("Stream #" + stream.getIndex()
                                       + " type: " + stream.getCodecType()
                                       + " duration: " + stream.getDuration() + " seconds");
                }


                try {
                    MultimediaInfo info = new MultimediaObject(new File(path)).getInfo();

                    // Get the media file codec
                    String codec = info.getFormat();
                    var meta = info.getMetadata();
                    System.out.println("Codec: " + codec);
                    System.out.println("Codec: " + meta);

                    // Get the media file length
                    long durationInMillis = info.getDuration();
                    long durationInSeconds = durationInMillis / 1000;
                    System.out.println("Duration: " + durationInSeconds + " seconds");
                } catch (EncoderException e) {
                    e.printStackTrace();
                }


            }


        } catch (Exception e) {
            // Handle exception
            logger.atSevere().withCause(e).log("Failed to process");
        }
    }
}
