module africa.jopen.gstreamerdemo {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.logging;
    requires okhttp3;
    requires org.json;
    requires webrtc.java;

    opens africa.jopen.gstreamerdemo to javafx.fxml;
    exports africa.jopen.gstreamerdemo;
}