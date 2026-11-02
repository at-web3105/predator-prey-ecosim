package ecosim.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Anything that has a position in the world. SpatialGrid is written against
 * Entity (not Animal), so it could index any future positioned thing
 * (burrows, obstacles, carcasses) without change.
 */
public abstract class Entity {

    // AtomicLong: ids stay unique even if entities are ever created from
    // several threads at once.
    private static final AtomicLong NEXT_ID = new AtomicLong();

    private final long id = NEXT_ID.incrementAndGet();
    protected Vec2 position;

    protected Entity(Vec2 position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    public final long id()        { return id; }
    public final Vec2 position()  { return position; }
}
