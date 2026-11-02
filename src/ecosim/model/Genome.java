package ecosim.model;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The heritable traits of an animal. Offspring receive a crossover of their
 * parents' genomes plus a small random mutation, so traits EVOLVE over time:
 * nobody programs "rabbits get faster", it emerges from who survives.
 *
 * Trade-off built into the model: speed and vision both cost energy every
 * tick (see Animal.upkeep), so "fastest possible" is never automatically best.
 */
public record Genome(double speed, double vision) {

    public static final double MIN_SPEED = 0.4, MAX_SPEED = 4.0;
    public static final double MIN_VISION = 10, MAX_VISION = 160;

    /**
     * Compact constructor validates invariants. An out-of-range genome is a
     * programming error (a bug in mutate or a bad literal), not something the
     * caller could recover from, so an UNCHECKED exception is the right choice.
     */
    public Genome {
        if (!(speed >= MIN_SPEED && speed <= MAX_SPEED))
            throw new IllegalArgumentException("speed out of range: " + speed);
        if (!(vision >= MIN_VISION && vision <= MAX_VISION))
            throw new IllegalArgumentException("vision out of range: " + vision);
    }

    /**
     * Returns a mutated copy. ThreadLocalRandom is used (not a shared Random)
     * because this may be called from parallel code, and a shared
     * java.util.Random would become a contention point between threads.
     */
    public Genome mutate(double rate) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new Genome(
                clamp(speed * (1 + r.nextGaussian() * rate), MIN_SPEED, MAX_SPEED),
                clamp(vision * (1 + r.nextGaussian() * rate), MIN_VISION, MAX_VISION));
    }

    /** Each gene comes from one parent at random. */
    public static Genome crossover(Genome a, Genome b) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new Genome(r.nextBoolean() ? a.speed : b.speed,
                          r.nextBoolean() ? a.vision : b.vision);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
