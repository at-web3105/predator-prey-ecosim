package ecosim.model;

import ecosim.behaviour.HuntState;
import ecosim.config.SimConfig;
import ecosim.events.EventBus;
import ecosim.events.SimEvent;
import ecosim.stats.TraitStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Owns every animal and the grass, and advances the simulation one tick at a time.
 *
 * Each tick:
 *   1. rebuild the spatial index                          (sequential)
 *   2. every animal thinks and returns an Intent          (PARALLEL, read-only world)
 *   3. intents are applied in a random order              (sequential, resolves conflicts)
 *   4. dead removed, newborns added, grass regrows        (grass regrowth is parallel by row)
 *   5. statistics gathered with a custom collector        (PARALLEL)
 *   6. TickEnded published to observers
 */
public final class World implements WorldView {

    private static final double GRASS_BITE = 0.25;
    private static final double GRASS_ENERGY = 12;
    private static final double GRID_CELL = 40;

    private final SimConfig config;
    private final EventBus<SimEvent> bus;
    private final List<Animal> animals = new ArrayList<>();
    private final SpatialGrid<Animal> grid;
    private GrassField grass;
    private List<Warren> warrens = List.of();
    private long tick;
    private boolean parallel;

    public World(SimConfig config, EventBus<SimEvent> bus) {
        this.config = config;
        this.bus = bus;
        this.grid = new SpatialGrid<>(config.width(), config.height(), GRID_CELL);
        this.parallel = config.parallel();
        reset();
    }

    public void reset() {
        animals.clear();
        tick = 0;
        grass = new GrassField(config.width(), config.height(), 10, 0.5);
        warrens = placeWarrens(7);
        Vec2 centre = new Vec2(config.width() / 2.0, config.height() / 2.0);
        double spread = Math.max(config.width(), config.height());
        spawn(config.initialRabbits(), centre, spread, Rabbit::new, Rabbit.STARTING_GENOME, 60);
        spawn(config.initialFoxes(), centre, spread, Fox::new, Fox.STARTING_GENOME, 110);
    }

    /** Scatter warrens, keeping them away from the edges and from each other. */
    private List<Warren> placeWarrens(int count) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<Warren> out = new ArrayList<>();
        int attempts = 0;
        while (out.size() < count && attempts++ < 1000) {
            Vec2 c = new Vec2(60 + r.nextDouble() * (config.width() - 120),
                              60 + r.nextDouble() * (config.height() - 120));
            if (out.stream().allMatch(w -> w.centre().dist(c) > 180)) {
                out.add(new Warren(c, 22));
            }
        }
        return List.copyOf(out);   // immutable: safe to read from any thread
    }

    // ---- spawning ---------------------------------------------------------

    /** Interface for "a constructor that makes some kind of Animal", so spawn works for any species. */
    @FunctionalInterface
    public interface AnimalMaker {
        Animal make(Vec2 position, Genome genome, double energy);
    }

    /** Constructor references (Rabbit::new, Fox::new) are passed in as AnimalMakers. */
    public void spawn(int count, Vec2 around, double spread, AnimalMaker maker, Genome base, double energy) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            Vec2 p = new Vec2(
                    clamp(around.x() + (r.nextDouble() - 0.5) * spread, 0, config.width()),
                    clamp(around.y() + (r.nextDouble() - 0.5) * spread, 0, config.height()));
            animals.add(maker.make(p, base.mutate(config.mutationRate()), energy));
        }
    }

    public void addRabbits(Vec2 at, int n) { spawn(n, at, 40, Rabbit::new, Rabbit.STARTING_GENOME, 60); }
    public void addFoxes(Vec2 at, int n)   { spawn(n, at, 40, Fox::new, Fox.STARTING_GENOME, 110); }

    // ---- the tick ---------------------------------------------------------

    public void step() {
        long start = System.nanoTime();
        tick++;
        grid.rebuild(animals);

        // Phase 1: think. toList() keeps encounter order even in parallel,
        // so intents.get(i) belongs to animals.get(i).
        Stream<Animal> thinkers = parallel ? animals.parallelStream() : animals.stream();
        List<Intent> intents = thinkers.map(a -> a.think(this)).toList();

        // Phase 2: apply, one animal at a time, in random order so nobody
        // always gets first pick of the prey.
        List<Integer> order = IntStream.range(0, animals.size()).boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(order);

        List<Animal> newborns = new ArrayList<>();
        for (int i : order) {
            Animal a = animals.get(i);
            if (!a.isAlive()) continue;               // eaten earlier this tick
            Intent it = intents.get(i);

            a.move(it.desiredVelocity(), config.width(), config.height());

            if (it.graze()) {
                a.gainEnergy(grass.graze(a.position(), GRASS_BITE) * GRASS_ENERGY);
            }

            it.prey()
                    .filter(Animal::isAlive)                       // someone else may have got it first
                    .filter(p -> p.position().dist(a.position()) <= HuntState.CATCH_DISTANCE + 3)
                    .ifPresent(p -> {
                        p.kill();
                        a.gainEnergy(p.nutritionalValue());
                        bus.publish(new SimEvent.Died(p, DeathCause.EATEN, tick));
                    });

            it.mate()
                    .filter(m -> a.readyToBreed() && m.readyToBreed())  // partner may already have bred
                    .filter(m -> animals.size() + newborns.size() < config.maxAnimals())
                    .ifPresent(m -> {
                        for (Animal child : a.breedWith(m, config.mutationRate())) {
                            newborns.add(child);
                            bus.publish(new SimEvent.Born(child, tick));
                        }
                    });

            a.metabolise();
            a.naturalDeath().ifPresent(cause -> {
                a.kill();
                bus.publish(new SimEvent.Died(a, cause, tick));
            });
        }

        animals.removeIf(a -> !a.isAlive());
        animals.addAll(newborns);
        grass.regrow(currentGrowthRate(), parallel);

        double ms = (System.nanoTime() - start) / 1e6;
        bus.publish(new SimEvent.TickEnded(tick, censusBySpecies(), grass.coverage(), ms, parallel));
    }

    /**
     * groupingBy + our custom TraitStats collector, in parallel when enabled.
     * EnumMap::new as the map factory gives a compact map ordered by species.
     */
    public Map<Species, TraitStats> censusBySpecies() {
        Stream<Animal> s = parallel ? animals.parallelStream() : animals.stream();
        return s.collect(Collectors.groupingBy(
                Animal::species,
                () -> new EnumMap<>(Species.class),
                TraitStats.collector()));
    }

    /** Grass grows faster in "summer" and slower in "winter". */
    public double currentGrowthRate() {
        return config.grassGrowth() * (1 + 0.6 * seasonPhase());
    }

    /** +1 = midsummer, -1 = midwinter, 0 if seasons are disabled. */
    public double seasonPhase() {
        if (config.seasonLength() <= 0) return 0;
        return Math.sin(2 * Math.PI * tick / config.seasonLength());
    }

    // ---- WorldView (read-only, used while animals think) -------------------

    @Override
    public <T extends Animal> List<T> nearby(Vec2 centre, double radius, Class<T> type) {
        return grid.within(centre, radius, type);
    }

    @Override public GrassField grass()   { return grass; }
    @Override public List<Warren> warrens() { return warrens; }
    @Override public double width()       { return config.width(); }
    @Override public double height()      { return config.height(); }

    // ---- accessors for the UI ---------------------------------------------

    /** Read-only view so the UI cannot add or remove animals behind the World's back. */
    public List<Animal> animals()         { return Collections.unmodifiableList(animals); }
    public long tick()                    { return tick; }
    public boolean isParallel()           { return parallel; }
    public void setParallel(boolean p)    { this.parallel = p; }
    public SimConfig config()             { return config; }

    /** Count animals matching a type, e.g. count(Fox.class). */
    public <T extends Animal> long count(Class<T> type) {
        return animals.stream().filter(type::isInstance).count();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
