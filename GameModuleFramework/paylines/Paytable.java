package GameModuleFramework.paylines;

import GameModuleFramework.symbols.Symbol;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable symbol payouts keyed by the number of consecutive matching reels. */
public final class Paytable {
    private final Map<Symbol, Map<Integer, Double>> payouts;

    public Paytable(Map<Symbol, Map<Integer, Double>> payouts) {
        Map<Symbol, Map<Integer, Double>> copy = new LinkedHashMap<>();
        List<Map.Entry<Symbol, Map<Integer, Double>>> orderedPayouts =
                payouts.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(
                                java.util.Comparator.comparing(Symbol::id)))
                        .toList();
        orderedPayouts.forEach(entry -> copy.put(entry.getKey(),
                Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue()))));
        this.payouts = Collections.unmodifiableMap(copy);
    }

    public double getPayout(Symbol symbol, int matchingReels) {
        return payouts.getOrDefault(symbol, Map.of()).getOrDefault(matchingReels, 0.0);
    }

    public Iterable<Symbol> getSymbols() {
        return payouts.keySet();
    }
}
