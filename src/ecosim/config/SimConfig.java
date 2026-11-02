package ecosim.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * All tunable numbers in one immutable record, loadable from a .properties
 * file so the ecosystem can be rebalanced without recompiling.
 */
public record SimConfig(int width, int height,
                        int initialRabbits, int initialFoxes,
                        double grassGrowth, int seasonLength,
                        double mutationRate, int maxAnimals,
                        boolean parallel) {

    public static SimConfig defaults() {
        return new SimConfig(1000, 640, 260, 22, 0.012, 1200, 0.06, 4000, true);
    }

    /**
     * Load settings, falling back to the default for any key that is absent.
     *
     * Deliberately NOT exceptional: a missing file. That is a normal case
     * (first run), so the caller checks Files.exists instead of catching.
     *
     * @throws ConfigException if the file cannot be read, a value is not a
     *         number, or a value is outside its allowed range.
     */
    public static SimConfig load(Path file) throws ConfigException {
        Properties p = new Properties();
        // try-with-resources: the reader is closed even if parsing fails
        try (Reader in = Files.newBufferedReader(file)) {
            p.load(in);
        } catch (IOException e) {
            throw new ConfigException("Could not read " + file, e);
        }

        SimConfig d = defaults();
        return new SimConfig(
                intIn(p, "width", d.width, 200, 4000),
                intIn(p, "height", d.height, 200, 4000),
                intIn(p, "initialRabbits", d.initialRabbits, 0, 5000),
                intIn(p, "initialFoxes", d.initialFoxes, 0, 2000),
                doubleIn(p, "grassGrowth", d.grassGrowth, 0.0, 0.5),
                intIn(p, "seasonLength", d.seasonLength, 0, 100_000),
                doubleIn(p, "mutationRate", d.mutationRate, 0.0, 1.0),
                intIn(p, "maxAnimals", d.maxAnimals, 10, 50_000),
                Boolean.parseBoolean(p.getProperty("parallel", String.valueOf(d.parallel))));
    }

    private static int intIn(Properties p, String key, int def, int min, int max) throws ConfigException {
        String raw = p.getProperty(key);
        if (raw == null) return def;
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < min || v > max) {
                throw new ConfigException(key + "=" + v + " is outside " + min + ".." + max);
            }
            return v;
        } catch (NumberFormatException e) {
            // translate a low-level library exception into our domain exception
            throw new ConfigException(key + " must be a whole number but was '" + raw + "'", e);
        }
    }

    private static double doubleIn(Properties p, String key, double def, double min, double max) throws ConfigException {
        String raw = p.getProperty(key);
        if (raw == null) return def;
        try {
            double v = Double.parseDouble(raw.trim());
            if (!(v >= min && v <= max)) {
                throw new ConfigException(key + "=" + v + " is outside " + min + ".." + max);
            }
            return v;
        } catch (NumberFormatException e) {
            throw new ConfigException(key + " must be a number but was '" + raw + "'", e);
        }
    }
}
