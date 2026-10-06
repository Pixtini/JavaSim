package toolkit.reelset;

import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Formats full reelset aggregates with symbol rows and kind columns. */
public final class ReelsetReportPrinter {
    private ReelsetReportPrinter() {}

    public static void print(ReelsetSimulationResult result, PrintStream output) {
        output.println("\nFull basegame reelset results (freegames not played)");
        output.printf("Combinations evaluated: %d%n", result.combinations());
        output.printf("Selector-weighted outcomes: %d%n", result.weightedCombinations());
        output.printf("Freegame triggers: %d%n", result.featureTriggers());
        output.printf(java.util.Locale.ROOT, "Total stake: %.2f%n", result.totalStake());
        output.printf(java.util.Locale.ROOT, "Total winnings: %.2f%n", result.totalWinnings());
        output.printf(java.util.Locale.ROOT, "Total RTP: %.8f%%%n", result.rtp() * 100.0);
        printAwardTable(result, output);
    }

    private static void printAwardTable(ReelsetSimulationResult result, PrintStream output) {
        Map<String, Map<Integer, Long>> hitsBySymbol = new TreeMap<>();
        TreeSet<Integer> matchCounts = new TreeSet<>();
        Map<String, Long> otherAwards = new TreeMap<>();

        result.awardHits().forEach((label, hits) -> {
            AwardCategory category = parseAwardLabel(label);
            if (category == null) {
                otherAwards.put(label, hits);
                return;
            }
            hitsBySymbol.computeIfAbsent(category.symbol(), ignored -> new TreeMap<>())
                    .put(category.matchCount(), hits);
            matchCounts.add(category.matchCount());
        });

        output.println("\nLine award hits:");
        if (hitsBySymbol.isEmpty()) {
            output.println("No symbol-and-kind award categories available.");
        } else {
            int symbolWidth = Math.max(6, hitsBySymbol.keySet().stream()
                    .mapToInt(String::length).max().orElse(6));
            output.printf("%-" + symbolWidth + "s", "Symbol");
            for (int matchCount : matchCounts) {
                output.printf(" | %8s", matchCount + "oak");
            }
            output.println();
            output.print("-".repeat(symbolWidth));
            for (int ignored : matchCounts) {
                output.print("-+----------");
            }
            output.println();
            hitsBySymbol.forEach((symbol, hitsByKind) -> {
                output.printf("%-" + symbolWidth + "s", symbol);
                for (int matchCount : matchCounts) {
                    output.printf(" | %8d", hitsByKind.getOrDefault(matchCount, 0L));
                }
                output.println();
            });
        }

        if (!otherAwards.isEmpty()) {
            output.println("Other award hits:");
            otherAwards.forEach((label, hits) ->
                    output.printf("  %-20s %d%n", label, hits));
        }
    }

    private static AwardCategory parseAwardLabel(String label) {
        int separator = label.lastIndexOf(' ');
        if (separator <= 0 || !label.endsWith("oak")) {
            return null;
        }
        try {
            int matchCount = Integer.parseInt(label.substring(separator + 1,
                    label.length() - "oak".length()));
            return new AwardCategory(label.substring(0, separator), matchCount);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record AwardCategory(String symbol, int matchCount) {}
}
