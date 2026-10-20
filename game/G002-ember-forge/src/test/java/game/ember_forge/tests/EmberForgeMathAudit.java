package game.ember_forge.tests;

import game.ember_forge.config.*;
import game.ember_forge.basegame.EmberForgeBaseGame;
import game.ember_forge.features.EmberForgeFreeGame;
import java.nio.file.*;
import java.util.*;

/** Independent integer-payout oracle plus finite-state Heat expectation/second moment. */
public final class EmberForgeMathAudit {
    public record Moments(long screens, double mean, double second, double hit) {}
    public record Feature(double mean, double second) {}
    private EmberForgeMathAudit() {}
    public static Moments enumerate(EmberForgeConfig c, boolean free, boolean compareEngine) {
        var reels = free ? c.freeReels() : c.baseReels();
        long count = 1; for (var reel : reels) count = Math.multiplyExact(count, reel.size());
        if (count > 2_000_000) throw new IllegalArgumentException("Exact G002 audit workload too large");
        var bg = new EmberForgeBaseGame(c); var fg = new EmberForgeFreeGame(c);
        int[] stops = new int[5];
        long sum = 0, squares = 0, hits = 0;
        for (long index = 0; index < count; index++) {
            int ticks = oracle(c, reels, stops);
            sum += ticks; squares += (long) ticks * ticks; if (ticks > 0) hits++;
            if (compareEngine) {
                double actual = free ? fg.evaluate(stops, 1, 1, false).getWin()
                        : bg.evaluate(0, stops, 1, false).getWin();
                if (Math.abs(actual - ticks / 20.0) > 1e-8)
                    throw new AssertionError("G002 shared evaluator differs from integer oracle at " + Arrays.toString(stops));
            }
            for (int r = 4; r >= 0; r--) {
                stops[r]++; if (stops[r] < reels.get(r).size()) break; stops[r] = 0;
            }
        }
        return new Moments(count, sum / (20.0 * count), squares / (400.0 * count), (double) hits / count);
    }
    private static int oracle(EmberForgeConfig c, List<List<Integer>> reels, int[] stops) {
        int result = 0;
        for (var line : c.lineRows()) {
            int best = 0;
            for (int symbol = 1; symbol <= 6; symbol++) {
                int matches = 0;
                for (int r = 0; r < 5; r++) {
                    var strip = reels.get(r);
                    int visible = strip.get((stops[r] + line.get(r)) % strip.size());
                    if (visible != symbol && visible != 7) break;
                    matches++;
                }
                if (matches >= 3) best = Math.max(best,
                        (int) Math.round(c.payouts().get(symbol - 1).get(matches - 3) * 20));
            }
            result += best;
        }
        return result;
    }
    public static Feature feature(EmberForgeConfig c, Moments spin) {
        double[][] mean = new double[c.freeSpins() + 1][c.maxHeat() + 1];
        double[][] second = new double[c.freeSpins() + 1][c.maxHeat() + 1];
        for (int n = 1; n <= c.freeSpins(); n++) {
            for (int heat = 1; heat <= c.maxHeat(); heat++) {
                int next = Math.min(c.maxHeat(), heat + 1);
                mean[n][heat] = heat * spin.mean() + (1 - spin.hit()) * mean[n - 1][heat]
                        + spin.hit() * mean[n - 1][next];
                second[n][heat] = heat * heat * spin.second()
                        + (1 - spin.hit()) * second[n - 1][heat]
                        + spin.hit() * second[n - 1][next]
                        + 2 * heat * spin.mean() * mean[n - 1][next];
            }
        }
        return new Feature(mean[c.freeSpins()][1], second[c.freeSpins()][1]);
    }
    public static void main(String[] args) throws Exception {
        var parser = new EmberForgeParParser();
        var normal = parser.parse(EmberForgeConfig.DEFAULT_PAR, "Normal");
        Moments base = enumerate(normal, false, true), spin = enumerate(normal, true, true);
        Feature feature = feature(normal, spin);
        StringBuilder out = new StringBuilder("metric,value\n");
        out.append("base_screens,").append(base.screens()).append("\n");
        out.append("free_screens,").append(spin.screens()).append("\n");
        out.append("ordinary_base_mean,").append(base.mean()).append("\n");
        out.append("ordinary_base_second_moment,").append(base.second()).append("\n");
        out.append("ordinary_base_hit_probability,").append(base.hit()).append("\n");
        out.append("free_spin_mean_at_heat1,").append(spin.mean()).append("\n");
        out.append("free_spin_hit_probability,").append(spin.hit()).append("\n");
        out.append("feature_mean,").append(feature.mean()).append("\n");
        out.append("feature_second_moment,").append(feature.second()).append("\n");
        for (String mode : List.of("Normal", "EM1", "BB1")) {
            var c = parser.parse(EmberForgeConfig.DEFAULT_PAR, mode);
            double p = c.triggerProbability();
            double mean = (1 - p) * base.mean() + p * feature.mean();
            double second = (1 - p) * base.second() + p * feature.second();
            out.append(mode).append("_trigger,").append(p).append("\n");
            out.append(mode).append("_rtp,").append(mean / c.costMultiplier()).append("\n");
            out.append(mode).append("_round_std_base_units,").append(Math.sqrt(second - mean * mean)).append("\n");
        }
        System.out.print(out);
        System.out.printf(Locale.ROOT, "SUGGEST_D=%d%nSUGGEST_BUY=%.2f%n",
                Math.round(2 * (feature.mean() - base.mean()) / base.mean()),
                Math.rint(feature.mean() / .97));
        if (args.length == 1) Files.writeString(Path.of(args[0]), out);
    }
}
