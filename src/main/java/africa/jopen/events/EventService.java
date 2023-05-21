package africa.jopen.events;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class EventService {
    private final MutableList<EventListener> listeners = Lists.mutable.empty();

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(EventListener listener) {
        listeners.remove(listener);
    }

    public void fireEvent(ClientsEvents event) {
        for (EventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
