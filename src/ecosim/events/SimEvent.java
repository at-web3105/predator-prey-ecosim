package ecosim.events;

import ecosim.model.Animal;
import ecosim.model.DeathCause;
import ecosim.model.Species;
import ecosim.stats.TraitStats;

import java.util.Map;

/**
 * Everything the World can announce. A SEALED interface means the compiler
 * knows the complete list of event types, and records make each event an
 * immutable message that is safe to hand to any listener.
 */
public sealed interface SimEvent permits SimEvent.Born, SimEvent.Died, SimEvent.TickEnded {

    record Born(Animal child, long tick) implements SimEvent {}

    record Died(Animal animal, DeathCause cause, long tick) implements SimEvent {}

    record TickEnded(long tick,
                     Map<Species, TraitStats> stats,
                     double grassCoverage,
                     double stepMillis,
                     boolean parallel) implements SimEvent {

        public TraitStats of(Species s) {
            return stats.getOrDefault(s, TraitStats.EMPTY);
        }
    }
}
