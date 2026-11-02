package ecosim.model;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A READ-ONLY view of the world handed to animals while they think.
 *
 * Animals depend on this interface rather than on World itself. That narrows
 * what an animal can do during the parallel phase to "look", never "change",
 * which is the core of the thread-safety argument.
 */
public interface WorldView {

    /** All animals of the given type (or a subtype) within radius of centre. */
    <T extends Animal> List<T> nearby(Vec2 centre, double radius, Class<T> type);

    GrassField grass();

    /** Burrows where prey is hidden from predators. */
    List<Warren> warrens();

    /** True if p is inside any warren. */
    default boolean inCover(Vec2 p) {
        return warrens().stream().anyMatch(w -> w.contains(p));
    }

    /** The closest warren to p, if there are any. */
    default Optional<Warren> nearestWarren(Vec2 p) {
        return warrens().stream().min(Comparator.comparingDouble(w -> w.centre().distSq(p)));
    }

    double width();

    double height();

    /**
     * The closest matching animal, if any. A generic default method: the
     * type parameter flows through, so nearest(..., Fox.class, ...) returns
     * Optional<Fox> with no casting at the call site.
     */
    default <T extends Animal> Optional<T> nearest(Vec2 centre, double radius,
                                                   Class<T> type, Predicate<? super T> filter) {
        return nearby(centre, radius, type).stream()
                .filter(filter)
                .min(Comparator.comparingDouble(a -> a.position().distSq(centre)));
    }
}
