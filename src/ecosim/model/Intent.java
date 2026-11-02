package ecosim.model;

import java.util.Optional;

/**
 * What an animal WANTS to do this tick. Producing an Intent has no side effects
 * on the world, which is what lets every animal "think" in parallel. The World
 * then applies all intents one at a time, so conflicts (two foxes chasing the
 * same rabbit) are resolved in a single thread with no locks.
 *
 * Optional is used instead of null so "no prey" / "no mate" is explicit in the
 * type, and the World can use filter/ifPresent instead of null checks.
 */
public record Intent(Vec2 desiredVelocity, boolean graze,
                     Optional<Animal> prey, Optional<Animal> mate) {

    public static Intent move(Vec2 v)                { return new Intent(v, false, Optional.empty(), Optional.empty()); }
    public static Intent graze(Vec2 v)               { return new Intent(v, true,  Optional.empty(), Optional.empty()); }
    public static Intent attack(Vec2 v, Animal prey) { return new Intent(v, false, Optional.of(prey), Optional.empty()); }
    public static Intent mate(Vec2 v, Animal other)  { return new Intent(v, false, Optional.empty(), Optional.of(other)); }
}
