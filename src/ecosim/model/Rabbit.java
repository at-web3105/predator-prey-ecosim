package ecosim.model;

import ecosim.behaviour.AnimalState;
import ecosim.behaviour.GrazeState;

import java.util.List;

public final class Rabbit extends Animal {

    public static final Genome STARTING_GENOME = new Genome(1.5, 45);

    public Rabbit(Vec2 position, Genome genome, double energy) {
        super(position, genome, energy);
    }

    @Override public Species species()           { return Species.RABBIT; }
    @Override protected double maxEnergy()       { return 100; }
    @Override protected int maturityAge()        { return 55; }
    @Override protected int maxAge()             { return 1000; }
    @Override protected double breedThreshold()  { return 55; }
    @Override protected double breedCost()       { return 15; }
    @Override protected double baseMetabolism()  { return 0.06; }
    @Override public double nutritionalValue()   { return 60; }
    @Override public AnimalState feedingState()  { return GrazeState.INSTANCE; }
    @Override protected int litterSize()         { return 3; }

    @Override
    protected Animal createOffspring(Vec2 position, Genome genome, double energy) {
        return new Rabbit(position, genome, energy);
    }

    /** Rabbits are the only species that has something to run from. */
    @Override
    public List<Fox> visibleThreats(WorldView view) {
        // covariant return: List<Fox> is a valid override of List<? extends Animal>
        return view.nearby(position, genome().vision(), Fox.class);
    }
}
