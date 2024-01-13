package africa.jopen.gstreamerdemo.lib;


import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RippleClientWebSocketEndpoint extends WebSocketListener {
    static Logger log = Logger.getLogger(RippleClientWebSocketEndpoint.class.getName());
    private RippleApp rippleApp;

    public void setRippleApp(RippleApp rippleApp) {
        if (rippleApp == null) {
            throw new IllegalArgumentException("RippleApp cannot be null");
        }
        this.rippleApp = rippleApp;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        log.info("WebSocket opened");
        rippleApp.onOpen("WebSocket opened");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        log.info("Received msg: " + text);
        if(text == null || text.isEmpty()) return;
        if(!RippleUtils.isJson(text)) return;
        rippleApp.onMessage(text);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
        log.log(Level.SEVERE, "Error", t);
        rippleApp.onError(t);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        log.info("WebSocket closed");
        rippleApp.onClose("WebSocket closed");
    }
}
