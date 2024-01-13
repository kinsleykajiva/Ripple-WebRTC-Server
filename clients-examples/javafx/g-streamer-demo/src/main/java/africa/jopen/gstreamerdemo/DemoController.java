package africa.jopen.gstreamerdemo;

import africa.jopen.gstreamerdemo.lib.PluginCallbacks;
import africa.jopen.gstreamerdemo.lib.RippleApp;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class DemoController implements Initializable , PluginCallbacks.GstreamerPluginCallBack {
    
    static  Logger log = Logger.getLogger(DemoController.class.getName());
   
    private RippleApp rippleApp;
    
    @FXML
    private AnchorPane AnchorstreamsVids;
    
    @FXML
    private ScrollPane ScrolllStreamsVids;
    
    @FXML
    private Button btnStartStreaming;
    @FXML
    private Label txtConnectionStatus;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rippleApp = new RippleApp("http://localhost:8080/",this);
        txtConnectionStatus.setText("Disconnected");
        btnStartStreaming.setOnMouseClicked(event -> {
            rippleApp.connect();
        
        });
        
        /*rippleApp.runAfterDelay(()->{
            rippleApp.connect();
        },2);*/
    }
    
    @Override
    public void onSocketClosed() {
        btnStartStreaming.setDisable(true);
        txtConnectionStatus.setText("Disconnected");
        txtConnectionStatus.setStyle("-fx-text-fill: red;");
        // change text color to red
        
        
        
    }
    
    @Override
    public void onSocketConnected() {
        btnStartStreaming.setDisable(false);
        txtConnectionStatus.setText("Connected");
        txtConnectionStatus.setStyle("-fx-text-fill: green;");
    }
    
    @Override
    public void onSocketError(Throwable t) {
        btnStartStreaming.setDisable(true);
        txtConnectionStatus.setStyle("-fx-text-fill: red;");
    }
    
    @Override
    public void onSocketMessage(String message) {
    
    }
    
    @Override
    public void webRTCEvents(String message) {
    
    }
    
    @Override
    public void onStreamUIUpdates(String message) {
    
    }
}