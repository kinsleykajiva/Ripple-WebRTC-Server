package africa.jopen.models;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.json.JSONObject;

import java.util.Objects;

public final class RoomModel {
    private final ImmutableList<RTCModel> connections;
    private final boolean hasOffer;
    private final boolean hasAnswer;

    public RoomModel(ImmutableList<RTCModel> connections, boolean hasOffer, boolean hasAnswer) {
        this.connections = connections;
        this.hasOffer = hasOffer;
        this.hasAnswer = hasAnswer;
    }

    public RoomModel() {
        this(Lists.immutable.empty(), false, false);
    }

    public RoomModel(RTCModel connection) {
        this(Lists.mutable.with(connection).toImmutable(), connection.offer() != null, connection.answer() != null);
    }

    public RoomModel add(RTCModel rtc) {
        MutableList<RTCModel> updatedConnections = connections.toList();
        updatedConnections.add(rtc);
        return new RoomModel(updatedConnections.toImmutable(), hasOffer || rtc.offer() != null, hasAnswer || rtc.answer() != null);
    }

    public int getUsersCount() {
        return connections.size();
    }

    public JSONObject getOffer() {
        for (RTCModel connection : connections) {
            JSONObject offer = connection.offer();
            if (offer != null) {
                return offer;
            }
        }
        throw new RuntimeException("No offers");
    }

    public JSONObject getAnswer() {
        for (RTCModel connection : connections) {
            JSONObject answer = connection.answer();
            if (answer != null) {
                return answer;
            }
        }
        throw new RuntimeException("No answers");
    }

    public boolean empty() {
        return connections.isEmpty();
    }

    public ImmutableList<RTCModel> connections() {
        return connections;
    }

    public boolean hasOffer() {
        return hasOffer;
    }

    public boolean hasAnswer() {
        return hasAnswer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RoomModel) obj;
        return Objects.equals(this.connections, that.connections) &&
                this.hasOffer == that.hasOffer &&
                this.hasAnswer == that.hasAnswer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connections, hasOffer, hasAnswer);
    }

    @Override
    public String toString() {
        return "RoomModel[" +
                "connections=" + connections + ", " +
                "hasOffer=" + hasOffer + ", " +
                "hasAnswer=" + hasAnswer + ']';
    }

}
