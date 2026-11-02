package ecosim.model;

import ecosim.behaviour.AnimalState;
import ecosim.behaviour.HuntState;

public final class Fox extends Animal {

    public static final Genome STARTING_GENOME = new Genome(1.7, 60);

    public Fox(Vec2 position, Genome genome, double energy) {
        super(position, genome, energy);
    }

    @Override public Species species()           { return Species.FOX; }
    @Override protected double maxEnergy()       { return 200; }
    @Override protected int maturityAge()        { return 180; }
    @Override protected int maxAge()             { return 1600; }
    @Override protected double breedThreshold()  { return 130; }
    @Override protected double breedCost()       { return 40; }
    @Override protected double baseMetabolism()  { return 0.05; }
    @Override public double nutritionalValue()   { return 80; }
    @Override public AnimalState feedingState()  { return HuntState.INSTANCE; }
    @Override protected int litterSize()         { return 2; }
    /** Foxes roam large territories, so they track mates by scent over long range. */
    @Override protected double mateSearchRadius(){ return 260; }

    /**
     * Territoriality: a fox will not raise cubs where the area is already full
     * of foxes. This density-dependence caps fox booms so they cannot eat
     * every last rabbit (and then starve themselves).
     */
    @Override
    protected boolean willingToBreedHere(WorldView view) {
        return view.nearby(position, TERRITORY_RADIUS, Fox.class).size() <= MAX_NEIGHBOURS;
    }

    private static final double TERRITORY_RADIUS = 130;
    private static final int MAX_NEIGHBOURS = 3;   // includes this fox

    @Override
    protected Animal createOffspring(Vec2 position, Genome genome, double energy) {
        return new Fox(position, genome, energy);
    }
}
