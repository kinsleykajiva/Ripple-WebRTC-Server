package africa.jopen.gstreamerdemo;

import africa.jopen.gstreamerdemo.lib.PluginCallbacks;
import africa.jopen.gstreamerdemo.lib.RippleApp;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class DemoController implements Initializable , PluginCallbacks.GstreamerPluginCallBack {
    
    static  Logger log = Logger.getLogger(DemoController.class.getName());
    @FXML
    private Label  welcomeText;
    private RippleApp rippleApp;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rippleApp = new RippleApp("http://localhost:8080/",this);
    }
    
    @Override
    public void onSocketClosed() {
    
    }
    
    @Override
    public void onSocketConnected() {
    
    }
    
    @Override
    public void onSocketError(Throwable t) {
    
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