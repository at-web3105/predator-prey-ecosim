package ecosim.behaviour;

import ecosim.model.Animal;
import ecosim.model.Intent;
import ecosim.model.Vec2;
import ecosim.model.WorldView;

/** Rabbit feeding: eat where standing if there is grass, else walk to the best patch in sight. */
public final class GrazeState implements AnimalState {

    public static final GrazeState INSTANCE = new GrazeState();
    private static final double WORTH_EATING = 0.15;

    private GrazeState() {}

    @Override
    public Intent act(Animal self, WorldView view) {
        if (!self.visibleThreats(view).isEmpty()) {
            self.changeState(FleeState.INSTANCE);
            return Intent.move(Steering.flee(self, self.visibleThreats(view), view));
        }
        if (self.isSatisfied()) {
            self.changeState(WanderState.INSTANCE);
        } else if (self.readyToBreed() && self.findMate(view).isPresent()) {
            self.changeState(MateState.INSTANCE);
        }

        if (view.grass().levelAt(self.position()) > WORTH_EATING) {
            return Intent.graze(Vec2.ZERO);          // stop and eat
        }
        return view.grass().richestNear(self.position(), self.genome().vision())
                .map(patch -> Intent.move(Steering.seek(self, patch, view)))
                .orElseGet(() -> Intent.move(Steering.wander(self, view).times(1.6)));
    }

    @Override public String label() { return "graze"; }
}
