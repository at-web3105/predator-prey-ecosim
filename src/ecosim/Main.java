package ecosim;

import ecosim.config.ConfigException;
import ecosim.config.SimConfig;
import ecosim.events.EventBus;
import ecosim.events.SimEvent;
import ecosim.model.Species;
import ecosim.model.World;
import ecosim.view.SimFrame;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point.
 *   java -cp out ecosim.Main                  opens the window
 *   java -cp out ecosim.Main --headless 5000  runs 5000 ticks and prints a census
 */
public final class Main {

    private static final Path CONFIG_FILE = Path.of("ecosim.properties");

    public static void main(String[] args) {
        SimConfig config = loadConfig();
        EventBus<SimEvent> bus = new EventBus<>();
        World world = new World(config, bus);

        if (args.length >= 1 && args[0].equals("--benchmark")) {
            benchmark();
        } else if (args.length >= 1 && args[0].equals("--headless")) {
            int ticks = args.length >= 2 ? Integer.parseInt(args[1]) : 3000;
            runHeadless(world, bus, ticks);
        } else {
            SwingUtilities.invokeLater(() -> new SimFrame(world, bus).setVisible(true));
        }
    }

    /** A missing file is normal; a broken file is reported and defaults are used. */
    private static SimConfig loadConfig() {
        if (!Files.exists(CONFIG_FILE)) {
            return SimConfig.defaults();
        }
        try {
            SimConfig c = SimConfig.load(CONFIG_FILE);
            System.out.println("Loaded settings from " + CONFIG_FILE);
            return c;
        } catch (ConfigException e) {
            System.err.println("Problem in " + CONFIG_FILE + ": " + e.getMessage());
            if (e.getCause() != null) System.err.println("  caused by: " + e.getCause());
            System.err.println("Using default settings instead.");
            return SimConfig.defaults();
        }
    }

    /**
     * Times sequential vs parallel ticks on small and large populations.
     * Parallel streams have a fixed overhead (splitting work, waking threads),
     * so they only pay off when there is enough work per tick.
     */
    private static void benchmark() {
        System.out.println("cores available: " + Runtime.getRuntime().availableProcessors());
        for (int rabbits : new int[]{200, 1000, 4000}) {
            for (boolean parallel : new boolean[]{false, true}) {
                SimConfig c = new SimConfig(1000, 640, rabbits, rabbits / 12, 0.0, 0, 0.06, 20_000, parallel);
                World w = new World(c, new EventBus<>());
                for (int i = 0; i < 50; i++) w.step();             // warm up the JIT
                long t0 = System.nanoTime();
                int ticks = 150;
                for (int i = 0; i < ticks; i++) w.step();
                double ms = (System.nanoTime() - t0) / 1e6 / ticks;
                System.out.printf("start rabbits %5d  %-10s  %.3f ms/tick  (animals at end: %d)%n",
                        rabbits, parallel ? "parallel" : "sequential", ms, w.animals().size());
            }
        }
    }

    private static void runHeadless(World world, EventBus<SimEvent> bus, int ticks) {
        bus.subscribe(SimEvent.TickEnded.class, e -> {
            if (e.tick() % 200 == 0) {
                var r = e.of(Species.RABBIT);
                var f = e.of(Species.FOX);
                System.out.printf("tick %5d  rabbits %4d (spd %.2f vis %5.1f)  foxes %3d (spd %.2f vis %5.1f)  grass %.2f  %.2fms%n",
                        e.tick(), r.count(), r.meanSpeed(), r.meanVision(),
                        f.count(), f.meanSpeed(), f.meanVision(), e.grassCoverage(), e.stepMillis());
            }
        });
        for (int i = 0; i < ticks; i++) world.step();
    }
}
