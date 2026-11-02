package ecosim.behaviour;

import ecosim.model.Animal;
import ecosim.model.Intent;
import ecosim.model.WorldView;

/** Seek out a ready partner of the same species and breed when close enough. */
public final class MateState implements AnimalState {

    public static final MateState INSTANCE = new MateState();
    public static final double MATING_DISTANCE = 9;

    private MateState() {}

    @Override
    public Intent act(Animal self, WorldView view) {
        if (!self.visibleThreats(view).isEmpty()) {
            self.changeState(FleeState.INSTANCE);
            return Intent.move(Steering.flee(self, self.visibleThreats(view), view));
        }
        if (!self.readyToBreed()) {
            self.changeState(WanderState.INSTANCE);
            return Intent.move(Steering.wander(self, view));
        }
        return self.findMate(view)
                .map(partner -> partner.position().dist(self.position()) <= MATING_DISTANCE
                        ? Intent.mate(Steering.seek(self, partner.position(), view).times(0.2), partner)
                        : Intent.move(Steering.seek(self, partner.position(), view)))
                .orElseGet(() -> {
                    self.changeState(self.isHungry() ? self.feedingState() : WanderState.INSTANCE);
                    return Intent.move(Steering.wander(self, view));
                });
    }

    @Override public String label() { return "mate"; }
}
