package ecosim.behaviour;

import ecosim.model.Animal;
import ecosim.model.Intent;
import ecosim.model.Vec2;
import ecosim.model.WorldView;

import java.util.List;

/** Prey response: run directly away from every predator in sight. */
public final class FleeState implements AnimalState {

    public static final FleeState INSTANCE = new FleeState();

    private FleeState() {}

    @Override
    public Intent act(Animal self, WorldView view) {
        List<? extends Animal> threats = self.visibleThreats(view);
        if (threats.isEmpty()) {
            self.changeState(WanderState.INSTANCE);
            return Intent.move(Steering.wander(self, view));
        }
        // Already safe underground: keep still and wait for the danger to pass.
        if (view.inCover(self.position())) {
            return Intent.move(Vec2.ZERO);
        }
        // Run away, but bend the escape towards the nearest burrow if one is close.
        Vec2 away = Steering.flee(self, threats, view);
        return Intent.move(view.nearestWarren(self.position())
                .filter(w -> w.centre().dist(self.position()) < self.genome().vision() * 2)
                .map(w -> away.plus(Steering.seek(self, w.centre(), view).times(1.5))
                              .normalised().times(self.genome().speed()))
                .orElse(away));
    }

    @Override public String label() { return "flee"; }
}
