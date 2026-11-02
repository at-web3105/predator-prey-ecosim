package ecosim.stats;

import ecosim.model.Animal;

import java.util.stream.Collector;

/**
 * Summary of one species at one moment: head count and average traits.
 *
 * CUSTOM COLLECTOR: computing four averages with ordinary streams would mean
 * four passes over the population (or a messy reduce). This collector makes
 * ONE pass, and because it supplies a combiner it works correctly with
 * parallelStream(): each thread fills its own Acc, then the Accs are merged.
 * No shared mutable state, so no locks needed.
 */
public record TraitStats(long count, double meanSpeed, double meanVision, double meanEnergy) {

    public static final TraitStats EMPTY = new TraitStats(0, 0, 0, 0);

    public static Collector<Animal, ?, TraitStats> collector() {
        return Collector.of(
                Acc::new,      // supplier:    a fresh accumulator per thread
                Acc::add,      // accumulator: fold one animal in
                Acc::merge,    // combiner:    join two partial results (parallel only)
                Acc::finish,   // finisher:    turn running sums into means
                Collector.Characteristics.UNORDERED);
    }

    /** Mutable, but each instance is only ever touched by one thread at a time. */
    private static final class Acc {
        long n;
        double speed, vision, energy;

        void add(Animal a) {
            n++;
            speed += a.genome().speed();
            vision += a.genome().vision();
            energy += a.energy();
        }

        Acc merge(Acc o) {
            n += o.n;
            speed += o.speed;
            vision += o.vision;
            energy += o.energy;
            return this;
        }

        TraitStats finish() {
            return n == 0 ? EMPTY : new TraitStats(n, speed / n, vision / n, energy / n);
        }
    }
}
