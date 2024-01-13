package africa.jopen.gstreamerdemo;

import africa.jopen.gstreamerdemo.lib.RippleApp;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.logging.Logger;

public class DemoController {
    
    static  Logger log = Logger.getLogger(DemoController.class.getName());
    @FXML
    private Label  welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}