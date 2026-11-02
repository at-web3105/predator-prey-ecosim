package ecosim.behaviour;

import ecosim.model.Animal;
import ecosim.model.Intent;
import ecosim.model.WorldView;

/** Default state: amble around until something more important comes up. */
public final class WanderState implements AnimalState {

    public static final WanderState INSTANCE = new WanderState();

    private WanderState() {}

    @Override
    public Intent act(Animal self, WorldView view) {
        // Transitions are checked in priority order. The new state takes over
        // NEXT tick, which avoids states bouncing back and forth recursively.
        if (!self.visibleThreats(view).isEmpty()) {
            self.changeState(FleeState.INSTANCE);
        } else if (self.readyToBreed() && self.findMate(view).isPresent()) {
            self.changeState(MateState.INSTANCE);
        } else if (self.isHungry()) {
            self.changeState(self.feedingState());   // polymorphic: graze or hunt
        }
        return Intent.move(Steering.wander(self, view));
    }

    @Override public String label() { return "wander"; }
}
