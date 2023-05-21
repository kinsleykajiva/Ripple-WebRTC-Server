package africa.jopen.utils;

import com.google.common.flogger.FluentLogger;
import dev.onvoid.webrtc.media.video.I420Buffer;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoFrameBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoFileRenderer implements Runnable {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	private final Queue<VideoFrame> frameQueue = new ConcurrentLinkedQueue<VideoFrame>();
	private final File file;
	private Thread thread = null;
	
	private int outputFileWidth;
	private int outputFileHeight;
	private int outputFrameSize;
	
	private FileOutputStream fileOutputStream;
	
	private AtomicBoolean headerWritten = new AtomicBoolean(false);
	
	public VideoFileRenderer(File file) {
		
		this.file = file;
	}
	
	public synchronized void queue(VideoFrame videoFrame) throws IOException {
		
		if (headerWritten.getAndSet(true) == false) {
			
			// first time in, set some things up
			
			final VideoFrameBuffer videoFrameBuffer = videoFrame.buffer;
			
			outputFileWidth = videoFrameBuffer.getWidth();
			outputFileHeight = videoFrameBuffer.getHeight();
			outputFrameSize = outputFileWidth * outputFileHeight * 3 / 2;
			
			final String fileHeader = String.format("YUV4MPEG2 C420 W%d H%d Ip F30:1 A1:1\n", outputFileWidth, outputFileHeight);
			
			logger.atInfo().log("Video file: %s\n%s", file.getName(), fileHeader);
			
			fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(fileHeader.getBytes(Charset.forName("US-ASCII")));
			
		}
		
		videoFrame.retain();
		frameQueue.add(videoFrame);
		
		if (thread != null) {
			
			if (thread.isAlive()) return;
		}
		
		Thread thread = new Thread(this);
		thread.run();
	}
	
	@Override
	public void run() {
		
		VideoFrame currentFrame;
		
		while ((currentFrame = frameQueue.poll()) != null) {
			
			renderCurrentFrame(currentFrame);
			
		}
	}
	
	private void renderCurrentFrame(VideoFrame currentFrame) {
		
		final VideoFrameBuffer videoFrameBuffer = currentFrame.buffer;
		videoFrameBuffer.retain();
		
		final I420Buffer i420Buffer = videoFrameBuffer.toI420();
		i420Buffer.retain();
		
		final ByteBuffer outputFrameBuffer = ByteBuffer.allocate(i420Buffer.getDataY().capacity() + 4 + i420Buffer.getDataU().capacity() + 4 + i420Buffer.getDataV().capacity() + 4);
		
		outputFrameBuffer.put(i420Buffer.getDataY());
		outputFrameBuffer.putInt(i420Buffer.getStrideY());
		
		outputFrameBuffer.put(i420Buffer.getDataU());
		outputFrameBuffer.putInt(i420Buffer.getStrideU());
		
		outputFrameBuffer.put(i420Buffer.getDataV());
		outputFrameBuffer.putInt(i420Buffer.getStrideV());
		
		i420Buffer.release();
		videoFrameBuffer.release();
		currentFrame.release();
		
		try {
			
			fileOutputStream.write("FRAME\n".getBytes(Charset.forName("US-ASCII")));
			fileOutputStream.write(
					outputFrameBuffer.array(), outputFrameBuffer.arrayOffset(), outputFrameSize);
			
		} catch (IOException e) {
			throw new RuntimeException("Error writing video to disk", e);
		}
		
	}
}
