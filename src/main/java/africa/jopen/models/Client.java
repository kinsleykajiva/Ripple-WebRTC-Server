package africa.jopen.models;

import africa.jopen.exceptions.ClientException;
import africa.jopen.utils.XUtils;
import dev.onvoid.webrtc.PeerConnectionObserver;
import jakarta.websocket.Session;

import java.util.Objects;
import java.util.UUID;
import java.util.Vector;

public final class Client {
    private final String clientId;
    private final long lastTimeStamp;
    private final Vector<String> messages;
    private final Recorder recorder;
    private final Integer trackCounter;


    public Client(String clientId, long lastTimeStamp, Vector<String> messages, Recorder recorder, Integer trackCounter) {
        if (clientId == null) {
            try {
                throw new ClientException("clientId cannot be null");
            } catch (ClientException e) {
                throw new RuntimeException(e);
            }
        }

        this.clientId = clientId;
        this.lastTimeStamp = lastTimeStamp;
        this.messages = messages;
        this.recorder = recorder;
        this.trackCounter = trackCounter;
    }

    public Client() {
        this(XUtils.IdGenerator(), System.currentTimeMillis(), new Vector<>(), new Recorder(), 0);
    }

    public String clientId() {
        return clientId;
    }

    public long lastTimeStamp() {
        return lastTimeStamp;
    }

    public Vector<String> messages() {
        return messages;
    }

    public Recorder recorder() {
        return recorder;
    }

    public Integer trackCounter() {
        return trackCounter;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Client) obj;
        return Objects.equals(this.clientId, that.clientId) &&
                this.lastTimeStamp == that.lastTimeStamp &&
                Objects.equals(this.messages, that.messages) &&
                Objects.equals(this.recorder, that.recorder) &&
                Objects.equals(this.trackCounter, that.trackCounter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, lastTimeStamp, messages, recorder, trackCounter);
    }

    @Override
    public String toString() {
        return "Client[" +
                "clientId=" + clientId + ", " +
                "lastTimeStamp=" + lastTimeStamp + ", " +
                "messages=" + messages + ", " +
                "recorder=" + recorder + ", " +
                "trackCounter=" + trackCounter + ']';
    }

}