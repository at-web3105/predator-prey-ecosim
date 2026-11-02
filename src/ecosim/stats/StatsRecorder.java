package ecosim.stats;

import ecosim.events.EventBus;
import ecosim.events.SimEvent;
import ecosim.model.DeathCause;
import ecosim.model.Species;

import java.util.EnumMap;
import java.util.Map;

/**
 * Listens to the World's events and keeps the history the graphs draw.
 * The World has no idea this class exists: that is the Observer decoupling.
 */
public final class StatsRecorder {

    /** One point on the graphs. */
    public record Sample(long tick, TraitStats rabbits, TraitStats foxes, double grass) {}

    private final RingBuffer<Sample> history;
    private final Map<DeathCause, Long> deaths = new EnumMap<>(DeathCause.class);
    private long births;
    private SimEvent.TickEnded latest;

    public StatsRecorder(EventBus<SimEvent> bus, int capacity, int sampleEvery) {
        this.history = new RingBuffer<>(capacity);

        // Each subscription is a lambda: no Observer subclasses needed.
        bus.subscribe(SimEvent.Born.class, e -> births++);
        bus.subscribe(SimEvent.Died.class, e -> deaths.merge(e.cause(), 1L, Long::sum));
        bus.subscribe(SimEvent.TickEnded.class, e -> {
            latest = e;
            if (e.tick() % sampleEvery == 0) {
                history.add(new Sample(e.tick(), e.of(Species.RABBIT), e.of(Species.FOX), e.grassCoverage()));
            }
        });
    }

    public RingBuffer<Sample> history()   { return history; }
    public long births()                  { return births; }
    public long deaths(DeathCause cause)  { return deaths.getOrDefault(cause, 0L); }
    public SimEvent.TickEnded latest()    { return latest; }

    public void reset() {
        history.clear();
        deaths.clear();
        births = 0;
        latest = null;
    }
}
