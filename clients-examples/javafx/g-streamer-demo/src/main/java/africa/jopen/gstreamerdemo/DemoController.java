package africa.jopen.gstreamerdemo;

import africa.jopen.gstreamerdemo.lib.PluginCallbacks;
import africa.jopen.gstreamerdemo.lib.RippleApp;
import africa.jopen.gstreamerdemo.lib.utils.VideoView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class DemoController implements Initializable, PluginCallbacks.GstreamerPluginCallBack {
	
	static Logger log = Logger.getLogger(DemoController.class.getName());
	
	private RippleApp rippleApp;
	
	@FXML
	private AnchorPane AnchorstreamsVids;
	
	@FXML
	private ScrollPane ScrolllStreamsVids;
	
	@FXML
	private Button btnStartStreaming;
	
	@FXML
	private GridPane GridstreamsVids;
	@FXML
	private Label    txtConnectionStatus;
	private int currentColumn = 0;
	private int currentRow = 0;
	private final int maxColumns = 2; // change this to the maximum number of columns you want
	
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		rippleApp = new RippleApp("http://localhost:8080/", this, PluginCallbacks.FeaturesAvailable.G_STREAM_BROADCASTER);
		txtConnectionStatus.setText("Disconnected");
		txtConnectionStatus.setStyle("-fx-text-fill: red;");
		btnStartStreaming.setOnMouseClicked(event -> {
		
		
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
	
	@Override @Blocking
	public void onStreamUIUpdates(@Nullable VideoView videoView) {
		if(videoView ==  null){
			// means remove the videoView from the UI
		}else{
			// means add the videoView to the UI
			GridPane.setHgrow(videoView, Priority.ALWAYS);
			GridPane.setVgrow(videoView, Priority.ALWAYS);
			GridPane.setFillWidth(videoView, true);
			GridPane.setFillHeight(videoView, true);
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