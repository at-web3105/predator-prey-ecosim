package ecosim.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * OBSERVER PATTERN (generic, lambda-based).
 *
 * The World (subject) publishes events without knowing who is listening.
 * Listeners (statistics, graph, HUD counters, console log) subscribe to just
 * the event types they care about.
 *
 * LAMBDAS REPLACE A CLASS HIERARCHY: the textbook Observer needs an Observer
 * interface and one named class per listener. Here a listener is just a
 * Consumer<T>, so most listeners are a single lambda or method reference
 * written where they are needed.
 *
 * GENERICS: EventBus<E> works for any event family. subscribe() is itself
 * generic in T extends E so a handler for Died receives a Died, with no casts,
 * and accepts Consumer<? super T> so a handler written for a supertype
 * (e.g. Consumer<SimEvent>) can listen to a specific subtype.
 */
public final class EventBus<E> {

    private record Subscription<E>(Class<? extends E> type, Consumer<E> handler) {}

    // Copy-on-write: listeners can safely subscribe/unsubscribe while an
    // event is being delivered (iteration sees a snapshot).
    private final List<Subscription<E>> subscriptions = new CopyOnWriteArrayList<>();

    /** Returns a Runnable that unsubscribes the handler when run. */
    public <T extends E> Runnable subscribe(Class<T> type, Consumer<? super T> handler) {
        Subscription<E> sub = new Subscription<>(type, event -> handler.accept(type.cast(event)));
        subscriptions.add(sub);
        return () -> subscriptions.remove(sub);
    }

    public void publish(E event) {
        for (Subscription<E> s : subscriptions) {
            if (s.type().isInstance(event)) {
                s.handler().accept(event);
            }
        }
    }
}
