package ecosim.behaviour;

import ecosim.model.Animal;
import ecosim.model.Intent;
import ecosim.model.Rabbit;
import ecosim.model.Vec2;
import ecosim.model.WorldView;

/** Fox feeding: chase the nearest visible rabbit, aiming slightly ahead of it. */
public final class HuntState implements AnimalState {

    public static final HuntState INSTANCE = new HuntState();
    public static final double CATCH_DISTANCE = 7;

    private HuntState() {}

    @Override
    public Intent act(Animal self, WorldView view) {
        if (self.isSatisfied()) {
            self.changeState(WanderState.INSTANCE);
            return Intent.move(Steering.wander(self, view));
        }
        if (self.readyToBreed() && self.findMate(view).isPresent()) {
            self.changeState(MateState.INSTANCE);
        }

        return view.nearest(self.position(), self.genome().vision(), Rabbit.class, r -> !view.inCover(r.position()))
                .map(prey -> {
                    double d = prey.position().dist(self.position());
                    // Lead the target. Reading prey.velocity() is safe here: velocities
                    // are only written in the sequential apply phase, never while thinking.
                    Vec2 aim = prey.position().plus(prey.velocity().times(d / (self.genome().speed() * 2)));
                    Vec2 v = Steering.seek(self, aim, view);
                    return d <= CATCH_DISTANCE ? Intent.attack(v, prey) : Intent.move(v);
                })
                .orElseGet(() -> Intent.move(Steering.wander(self, view).times(1.5)));
    }

    @Override public String label() { return "hunt"; }
}
