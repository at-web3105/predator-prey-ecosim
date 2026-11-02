package ecosim.config;

/**
 * Thrown when the settings file exists but contains bad values.
 *
 * CHECKED (extends Exception) on purpose: a typo in a text file is an
 * expected, recoverable situation that the caller must decide how to handle
 * (Main falls back to defaults and tells the user). Making it checked forces
 * every caller of SimConfig.load to deal with it, so it cannot be forgotten.
 */
public class ConfigException extends Exception {

    public ConfigException(String message) {
        super(message);
    }

    /** Keeps the original exception as the cause (exception chaining). */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
