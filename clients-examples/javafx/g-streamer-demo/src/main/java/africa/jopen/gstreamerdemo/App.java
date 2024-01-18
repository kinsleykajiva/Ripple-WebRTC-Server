package africa.jopen.gstreamerdemo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {
	static Logger log = Logger.getLogger(App.class.getName());
	
	@Override
	public void start( Stage stage ) {
		try {
			Scene scene = createScene();
			stage.setTitle("GStreamer Demo");
			stage.setScene(scene);
			stage.show();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error creating scene", e);
		}
	}
	
	private Scene createScene() throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("demo-view.fxml"));
		return new Scene(fxmlLoader.load(), 1300, 740);
	}
	
	public static void main( String[] args ) {
		launch();
	}
}