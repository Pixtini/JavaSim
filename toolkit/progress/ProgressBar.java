package toolkit.progress;

import java.io.PrintStream;
import java.util.Objects;

/** Thread-safe console progress display for long-running simulations. */
public final class ProgressBar implements AutoCloseable {
    private static final long DISPLAY_THRESHOLD = 100_000;
    private static final int BAR_WIDTH = 30;
    private static final int LOG_INTERVAL_PERCENT = 10;

    private final String label;
    private final long total;
    private final PrintStream output;
    private final boolean interactive;
    private final boolean enabled;
    private long completed;
    private int lastPercent = -1;
    private int lastLogInterval = -1;
    private long lastDrawNanos;
    private boolean closed;

    public ProgressBar(String label, long total, PrintStream output) {
        this.label = Objects.requireNonNull(label, "label");
        if (total <= 0) {
            throw new IllegalArgumentException("Progress total must be positive");
        }
        this.total = total;
        this.output = Objects.requireNonNull(output, "output");
        this.interactive = System.console() != null;
        this.enabled = total >= DISPLAY_THRESHOLD;
        this.lastDrawNanos = System.nanoTime();
    }

    /** Adds completed work and prints only when a visible progress step is reached. */
    public synchronized void advance(long amount) {
        if (closed || !enabled || amount <= 0) {
            return;
        }
        completed = Math.min(total, Math.addExact(completed, amount));
        int percent = (int) (completed * 100.0 / total);
        if (interactive) {
            long now = System.nanoTime();
            if (percent > lastPercent || now - lastDrawNanos >= 1_000_000_000L
                    || completed == total) {
                drawInteractive(percent);
                lastDrawNanos = now;
                lastPercent = percent;
            }
        } else {
            int interval = percent / LOG_INTERVAL_PERCENT;
            if (interval > lastLogInterval || completed == total) {
                drawLogLine(percent);
                lastLogInterval = interval;
            }
        }
    }

    private void drawLogLine(int percent) {
        int filled = (int) (BAR_WIDTH * (completed / (double) total));
        output.printf("%s [%s%s] %3d%% (%d/%d)%n", label,
                "=".repeat(filled), " ".repeat(BAR_WIDTH - filled),
                percent, completed, total);
    }

    private void drawInteractive(int percent) {
        int filled = (int) (BAR_WIDTH * (completed / (double) total));
        output.printf("\r%s [%s%s] %3d%% (%d/%d)", label,
                "=".repeat(filled), " ".repeat(BAR_WIDTH - filled),
                percent, completed, total);
        if (completed == total) {
            output.println();
        }
        output.flush();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        if (enabled && completed < total) {
            if (interactive) {
                output.println();
            } else {
                int percent = (int) (completed * 100.0 / total);
                output.printf("%s stopped at %d%% (%d/%d)%n",
                        label, percent, completed, total);
            }
        }
        closed = true;
    }
}
