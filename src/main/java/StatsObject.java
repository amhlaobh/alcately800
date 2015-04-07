import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transfer object to encapsulate timestam and data usage.
 */
public class StatsObject {
    /**
     * Timestamp in seconds since 1.1.1970
     */
    public final long timestamp;
    /**
     * Data usage in bytes.
     */
    public final long usage;

    static final Pattern statsFormat = Pattern.compile("(\\d+):(\\d+)");

    public StatsObject (long timestamp, long usage) {
        this.timestamp = timestamp;
        this.usage = usage;
    }

    /**
     * Parses a String into a StatsObject object -- the reverse of the toString method.
     * @param s the String to parse
     * @return the parsed object, or null if the String could not be matched
     */
    public static StatsObject fromString (String s) {
        Matcher matcher = statsFormat.matcher(s);
        if (matcher.find()) {
            return new StatsObject(Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)));
        }

        return null;
    }

    /**
     * Creates string representation of this object -- the reverse method is fromString.
     * @return String representation
     */
    public String toString() {
        return String.format("%d:%d", timestamp, usage);
    }
}
