package africa.jopen.gstreamerdemo;

import africa.jopen.gstreamerdemo.lib.PluginCallbacks;
import africa.jopen.gstreamerdemo.lib.RippleApp;
import africa.jopen.gstreamerdemo.lib.utils.VideoView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

public class DemoController implements Initializable, PluginCallbacks.GstreamerPluginCallBack {
	
	static Logger log = Logger.getLogger(DemoController.class.getName());
	
	private       RippleApp    rippleApp;
	private final List<String> mediaStreamFiles = Arrays.asList(
			"Shakespeare.mp4",
			"HeartAndSoulRiddimInstrumental.mp4",
			"MellowSleazyTmanXpressKwelinyeKeynote.mp4",
			"TheMessageRiddimMixDonCorleon.mp4"
	);
	
	private Set<String> chosenElements = new HashSet<>();
	private Random      random         = new Random();
	
	public String pickRandomElement() {
		if (chosenElements.size() == mediaStreamFiles.size()) {
			chosenElements.clear();
		}
		
		int    randomIndex   = random.nextInt(mediaStreamFiles.size());
		String chosenElement = mediaStreamFiles.get(randomIndex);
		
		while (chosenElements.contains(chosenElement)) {
			randomIndex = (randomIndex + 1) % mediaStreamFiles.size();
			chosenElement = mediaStreamFiles.get(randomIndex);
		}
		
		chosenElements.add(chosenElement);
		return chosenElement;
	}
	
	@FXML
	private AnchorPane AnchorstreamsVids;
	
	@FXML
	private ScrollPane ScrolllStreamsVids;
	
	@FXML
	private Button btnStartStreaming;
	
	@FXML
	private       GridPane GridstreamsVids;
	@FXML
	private       Label    txtConnectionStatus;
	private       int      currentColumn = 0;
	private       int      currentRow    = 0;
	private final int      maxColumns    = 2; // change this to the maximum number of columns you want
	
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		rippleApp = new RippleApp("http://localhost:8080/", this, PluginCallbacks.FeaturesAvailable.G_STREAM_BROADCASTER);
		txtConnectionStatus.setText("Disconnected");
		txtConnectionStatus.setStyle("-fx-text-fill: red;");
		btnStartStreaming.setOnMouseClicked(event -> {
			
			var fileMedia = pickRandomElement();
			log.info("fileMedia: " + fileMedia);
			rippleApp.requestNewThread(fileMedia);
			
		});
		
		
		rippleApp.runAfterDelay(() -> {
			Platform.runLater(() -> {
				rippleApp.connect();
			});
			
		}, 2);
	}
	
	
	@Override
	public void onClientClosed() {
		Platform.runLater(() -> {
			btnStartStreaming.setDisable(true);
			txtConnectionStatus.setText("Disconnected");
			txtConnectionStatus.setStyle("-fx-text-fill: red;");
			
			
		});
		
	}
	
	@Override
	public void onClientConnected() {
		Platform.runLater(() -> {
			btnStartStreaming.setDisable(false);
			txtConnectionStatus.setText("Connected");
			txtConnectionStatus.setStyle("-fx-text-fill: green;");
			
		});
	}
	
	@Override
	public void onClientError(Throwable t) {
		Platform.runLater(() -> {
			btnStartStreaming.setDisable(true);
			txtConnectionStatus.setStyle("-fx-text-fill: red;");
		});
	}
	
	@Override
	public void onClientMessage(String message) {
	
	}
	
	@Override
	public void webRTCEvents(String message) {
	
	}
	
	
	@Override
	public void onStreamUIUpdates(@NotNull String message) {
	
	}
	
	@Override
	@Blocking
	public void onStreamUIUpdates(@Nullable VideoView videoView) {
		
		if (videoView == null) {
			// means remove the videoView from the UI
		} else {
			// means add the videoView to the UI
			GridPane.setHgrow(videoView, Priority.ALWAYS);
			GridPane.setVgrow(videoView, Priority.ALWAYS);
			GridPane.setFillWidth(videoView, true);
			GridPane.setFillHeight(videoView, true);
			
			// Set padding and margin to zero
			GridPane.setMargin(videoView, new Insets(0));
			videoView.setPadding(new Insets(0));
			
			// Add a border to the videoView
			BorderStroke borderStroke = new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT);
			videoView.setBorder(new Border(borderStroke));
			
			GridstreamsVids.add(videoView, currentColumn, currentRow);
			
			// Update currentColumn and currentRow for the next VideoView
			currentColumn++;
			if (currentColumn >= maxColumns) {
				currentColumn = 0;
				currentRow++;
			}
		}
	}
}