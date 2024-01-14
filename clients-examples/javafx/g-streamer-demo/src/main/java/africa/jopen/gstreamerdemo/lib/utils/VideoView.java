package africa.jopen.gstreamerdemo.lib.utils;




import dev.onvoid.webrtc.media.video.VideoFrame;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.WritableValue;
import javafx.css.StyleableProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import static java.util.Objects.nonNull;

public class VideoView extends Control {
	
	private static final String DEFAULT_STYLE_CLASS = "video-view";
	
	private final BooleanProperty resize = new SimpleBooleanProperty(true);
	
	
	public VideoView() {
		initialize();
	}
	
	public void setVideoFrame(VideoFrame frame) {
		VideoViewSkin skin = (VideoViewSkin) getSkin();
		
		if (nonNull(skin)) {
			skin.setVideoFrame(frame);
		}
	}
	
	public final BooleanProperty resizeProperty() {
		return resize;
	}
	
	public final boolean getResize() {
		return resizeProperty().get();
	}
	
	public final void setResize(boolean resize) {
		resizeProperty().set(resize);
	}
	
	@Override
	protected Skin<?> createDefaultSkin() {
		return new VideoViewSkin(this);
	}
	
	@Override
	protected Boolean getInitialFocusTraversable() {
		return Boolean.FALSE;
	}
	
	private void initialize() {
		getStyleClass().setAll(DEFAULT_STYLE_CLASS);
		
		final StyleableProperty<Boolean> prop = (StyleableProperty<Boolean>)(WritableValue<Boolean>)focusTraversableProperty();
		prop.applyStyle(null, Boolean.FALSE);
	}
}
