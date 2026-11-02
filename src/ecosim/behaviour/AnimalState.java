package ecosim.behaviour;

import ecosim.model.Animal;
import ecosim.model.Intent;
import ecosim.model.WorldView;

/**
 * STATE PATTERN.
 *
 * An animal's behaviour depends on what it is currently doing (wandering,
 * grazing, hunting, fleeing, looking for a mate). Instead of one giant
 * if/else or switch in Animal, each behaviour is its own class, and the
 * animal delegates to whichever state object it currently holds.
 *
 * Unlike Strategy, a state knows about its context (the Animal) because it
 * decides the transitions: it calls self.changeState(next).
 *
 * Design choice: states are STATELESS SINGLETONS. All per-animal data lives
 * in Animal, and the context is passed in as a parameter. Consequences:
 *  + one instance per state for the whole simulation (no allocation per transition)
 *  + safe to share across threads during the parallel think phase
 *  - a state cannot remember anything of its own (e.g. "how long have I been fleeing")
 *    without adding a field to Animal. See the README for the alternative.
 */
public interface AnimalState {

    /** Decide what to do this tick, possibly transitioning the animal to a new state. */
    Intent act(Animal self, WorldView view);

    /** Short name for the on-screen label. */
    String label();
}
