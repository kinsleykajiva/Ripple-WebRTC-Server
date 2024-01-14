package africa.jopen.gstreamerdemo.lib.utils;




import africa.jopen.gstreamerdemo.lib.RippleUtils;
import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoFrameBuffer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.control.SkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;


import java.nio.ByteBuffer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class VideoViewSkin extends SkinBase<VideoView> {
	
	private ImageView imageView;
	
	private PixelBuffer<ByteBuffer> pixelBuffer;
	
	private ByteBuffer byteBuffer;
	
	private Rectangle border;
	
	private ChangeListener<Bounds> controlBoundsListener;
	
	private ChangeListener<Bounds> imageBoundsListener;
	
	
	/**
	 * Creates a new VideoViewSkin.
	 *
	 * @param control The control for which this Skin should attach to.
	 */
	public VideoViewSkin(VideoView control) {
		super(control);
		
		initLayout(control);
	}
	
	@Override
	public void dispose() {
		VideoView control = getSkinnable();
		
		unregisterChangeListeners(control.resizeProperty());
		
		if (nonNull(controlBoundsListener)) {
			control.layoutBoundsProperty().removeListener(controlBoundsListener);
		}
		
		super.dispose();
	}
	
	public void setVideoFrame(VideoFrame frame) {
		VideoFrameBuffer buffer = frame.buffer;
		int width = buffer.getWidth();
		int height = buffer.getHeight();
		
		if (isNull(pixelBuffer) || pixelBuffer.getWidth() != width || pixelBuffer.getHeight() != height) {
			byteBuffer = ByteBuffer.allocate(width * height * 4);
			pixelBuffer = new PixelBuffer<>(width, height, byteBuffer, WritablePixelFormat.getByteBgraPreInstance());
			
			imageView.setImage(new WritableImage(pixelBuffer));
		}
		
		try {
			VideoBufferConverter.convertFromI420(buffer, byteBuffer, FourCC.ARGB);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		RippleUtils.invoke(() -> pixelBuffer.updateBuffer(pixBuffer -> null));
	}
	
	private void initLayout(VideoView control) {
		imageView = new ResizableImageView();
		imageView.setSmooth(false);
		imageView.setCache(false);
		imageView.setPreserveRatio(true);
		
		border = new Rectangle();
		border.getStyleClass().add("border");
		
		getChildren().add(imageView);
		
		setResizable(control.getResize());
		
		registerChangeListener(control.resizeProperty(), o -> setResizable(control.getResize()));
	}
	
	private void setResizable(boolean resizable) {
		VideoView control = getSkinnable();
		
		if (resizable) {
			if (nonNull(imageBoundsListener)) {
				imageView.layoutBoundsProperty().removeListener(imageBoundsListener);
			}
			
			imageView.setClip(null);
			
			getChildren().remove(border);
			
			control.setMaxWidth(Region.USE_COMPUTED_SIZE);
			
			controlBoundsListener = (observable, oldBounds, newBounds) -> {
				imageView.setFitWidth(newBounds.getWidth());
				imageView.setFitHeight(newBounds.getHeight());
			};
			
			control.layoutBoundsProperty().addListener(controlBoundsListener);
		}
		else {
			if (nonNull(controlBoundsListener)) {
				control.layoutBoundsProperty().removeListener(controlBoundsListener);
			}
			
			control.setMaxWidth(Region.USE_PREF_SIZE);
			
			getChildren().add(border);
			
			imageBoundsListener = (observable, oldBounds, newBounds) -> {
				border.setX(newBounds.getMinX());
				border.setY(newBounds.getMinY());
				border.setWidth(newBounds.getWidth());
				border.setHeight(newBounds.getHeight());
				
				// Rectangle clip = new Rectangle(newBounds.getWidth(), newBounds.getHeight());
				// clip.setArcWidth(border.getArcWidth());
				// clip.setArcHeight(border.getArcHeight());
				
				// imageView.setClip(clip);
			};
			
			imageView.setFitWidth(0);
			imageView.setFitHeight(control.getMaxHeight());
			imageView.layoutBoundsProperty().addListener(imageBoundsListener);
		}
	}
	
	
	
	private static class ResizableImageView extends ImageView {
		
		@Override
		public double minWidth(double height) {
			return 100;
		}
		
		@Override
		public double minHeight(double width) {
			return 100;
		}
		
	}
}


