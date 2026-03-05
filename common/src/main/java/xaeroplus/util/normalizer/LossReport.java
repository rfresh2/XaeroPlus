package xaeroplus.util.normalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks lossy operations that occur during backporting.
 * Every loss is recorded with a description so the caller can audit
 * what data was degraded or discarded.
 *
 * <p>Thread-safe: each conversion should use its own instance.</p>
 */
public final class LossReport {

    public enum Severity { INFO, WARNING, ERROR }

    public record Entry(Severity severity, String category, String message) {
        @Override
        public String toString() {
            return "[" + severity + "] " + category + ": " + message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void info(String category, String message) {
        entries.add(new Entry(Severity.INFO, category, message));
    }

    public void warn(String category, String message) {
        entries.add(new Entry(Severity.WARNING, category, message));
    }

    public void error(String category, String message) {
        entries.add(new Entry(Severity.ERROR, category, message));
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean hasLosses() {
        return entries.stream().anyMatch(e -> e.severity != Severity.INFO);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int count(Severity severity) {
        return (int) entries.stream().filter(e -> e.severity == severity).count();
    }

    /** Print all entries to stderr. */
    public void dump() {
        for (Entry e : entries) {
            System.err.println(e);
        }
    }

    @Override
    public String toString() {
        if (entries.isEmpty()) return "LossReport: no losses";
        StringBuilder sb = new StringBuilder("LossReport: ")
            .append(count(Severity.ERROR)).append(" errors, ")
            .append(count(Severity.WARNING)).append(" warnings, ")
            .append(count(Severity.INFO)).append(" info\n");
        for (Entry e : entries) {
            sb.append("  ").append(e).append('\n');
        }
        return sb.toString();
    }
}
