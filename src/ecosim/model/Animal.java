package ecosim.model;

import ecosim.behaviour.AnimalState;
import ecosim.behaviour.WanderState;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * Base class for every creature.
 *
 * WHY AN ABSTRACT CLASS (not an interface): all animals share real state
 * (energy, age, genome, velocity, current behaviour state) and real logic
 * (metabolism, breeding, ageing). An abstract class lets that live in one
 * place, while subclasses fill in the species-specific numbers and hooks.
 *
 * Each tick runs in two phases (see World.step):
 *   1. think()  - PARALLEL. Reads the world, may change only this animal's
 *                 own behaviour state, returns an Intent.
 *   2. apply*() - SEQUENTIAL. The World moves the animal, feeds it, breeds it.
 * Fields that other animals read during phase 1 (position, velocity, energy,
 * age, cooldown) are only ever WRITTEN in phase 2, so there are no data races.
 */
public abstract class Animal extends Entity {

    private final Genome genome;
    private double energy;
    private int age;
    private int breedCooldown;
    private Vec2 velocity;
    private boolean alive = true;
    private AnimalState state = WanderState.INSTANCE;   // State pattern: current behaviour

    protected Animal(Vec2 position, Genome genome, double energy) {
        super(position);
        this.genome = genome;
        this.energy = energy;
        double heading = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        this.velocity = Vec2.fromAngle(heading).times(genome.speed() * 0.5);
    }

    // ---- species-specific hooks (subclasses must supply) -------------------

    public abstract Species species();
    protected abstract double maxEnergy();
    protected abstract int maturityAge();
    protected abstract int maxAge();
    protected abstract double breedThreshold();
    protected abstract double breedCost();
    protected abstract double baseMetabolism();

    /** How many young per successful mating. Default 1; rabbits override. */
    protected int litterSize() { return 1; }

    /** Animals find mates by scent as well as sight, so they can still pair up when sparse. */
    protected double mateSearchRadius() { return Math.max(genome.vision(), 110); }

    /** Extra conditions on breeding that depend on surroundings. Default: none. */
    protected boolean willingToBreedHere(WorldView view) { return true; }

    /** Energy a predator gains by eating this animal. */
    public abstract double nutritionalValue();

    /** The behaviour state this species uses to find food (graze vs hunt). */
    public abstract AnimalState feedingState();

    /**
     * FACTORY METHOD: the generic breeding code in this class does not know
     * which concrete class to create, so it asks the subclass.
     */
    protected abstract Animal createOffspring(Vec2 position, Genome genome, double energy);

    /** Predators this animal can currently see. Default: none (foxes fear nothing). */
    public List<? extends Animal> visibleThreats(WorldView view) {
        return List.of();
    }

    // ---- phase 1: think (parallel) ----------------------------------------

    /** Delegates to the current state object: that is the State pattern. */
    public final Intent think(WorldView view) {
        return state.act(this, view);
    }

    /** Called by state objects to transition. Only ever called during think(). */
    public final void changeState(AnimalState next) {
        this.state = next;
    }

    /** Another animal of my species, ready to breed, that I can see. */
    public final Optional<Animal> findMate(WorldView view) {
        if (!willingToBreedHere(view)) return Optional.empty();
        return view.nearest(position, mateSearchRadius(), Animal.class,
                other -> other != this && other.species() == species() && other.readyToBreed());
    }

    // ---- phase 2: apply (sequential, called by World only) -----------------

    final void move(Vec2 desired, double width, double height) {
        velocity = velocity.lerp(desired.limit(genome.speed()), 0.3);
        double x = position.x() + velocity.x();
        double y = position.y() + velocity.y();
        // bounce off walls
        if (x < 0 || x > width)  { velocity = new Vec2(-velocity.x(), velocity.y()); x = Math.max(0, Math.min(width, x)); }
        if (y < 0 || y > height) { velocity = new Vec2(velocity.x(), -velocity.y()); y = Math.max(0, Math.min(height, y)); }
        position = new Vec2(x, y);
    }

    final void metabolise() {
        energy -= upkeep();
        age++;
        if (breedCooldown > 0) breedCooldown--;
    }

    final void gainEnergy(double amount) {
        energy = Math.min(maxEnergy(), energy + amount);
    }

    /** Big muscles and sharp eyes cost energy every tick, used or not. */
    public final double upkeep() {
        double s = genome.speed();
        return baseMetabolism() + 0.018 * s * s + 0.0012 * genome.vision();
    }

    /** Both parents pay; each young gets its own crossed-over, mutated genome. */
    final List<Animal> breedWith(Animal partner, double mutationRate) {
        this.payForBreeding();
        partner.payForBreeding();
        Vec2 spot = position.lerp(partner.position, 0.5);
        double energyEach = breedCost() * 1.6 / litterSize() * (litterSize() > 1 ? 1.5 : 1);
        return Stream.generate(() -> Genome.crossover(genome, partner.genome).mutate(mutationRate))
                .limit(litterSize())
                .map(g -> createOffspring(spot, g, energyEach))
                .toList();
    }

    private void payForBreeding() {
        energy -= breedCost();
        breedCooldown = maturityAge() / 2;
    }

    final Optional<DeathCause> naturalDeath() {
        if (energy <= 0) return Optional.of(DeathCause.STARVED);
        if (age > maxAge()) return Optional.of(DeathCause.OLD_AGE);
        return Optional.empty();
    }

    final void kill() { alive = false; }

    // ---- queries -----------------------------------------------------------

    public final boolean readyToBreed() {
        return alive && age >= maturityAge() && breedCooldown == 0 && energy >= breedThreshold();
    }

    public final boolean isHungry()      { return energy < maxEnergy() * 0.6; }
    public final boolean isSatisfied()   { return energy > maxEnergy() * 0.9; }
    public final boolean isAlive()       { return alive; }
    public final Genome genome()         { return genome; }
    public final double energy()         { return energy; }
    public final double energyFraction() { return Math.max(0, energy / maxEnergy()); }
    public final int age()               { return age; }
    public final Vec2 velocity()         { return velocity; }
    public final AnimalState state()     { return state; }
}
