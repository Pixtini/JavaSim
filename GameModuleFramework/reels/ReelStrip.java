package GameModuleFramework.reels;

import GameModuleFramework.symbols.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Immutable circular reel strip. */
public final class ReelStrip {
    private final List<Symbol> symbols;

    public ReelStrip(List<Symbol> symbols) {
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("A reel strip must contain symbols");
        }
        this.symbols = List.copyOf(symbols);
        this.symbols.forEach(symbol -> Objects.requireNonNull(symbol, "reel symbol"));
    }

    public List<Symbol> spinWindow(Random random, int windowHeight) {
        Objects.requireNonNull(random, "random");
        if (windowHeight <= 0) {
            throw new IllegalArgumentException("Window height must be positive");
        }

        int stop = random.nextInt(symbols.size());
        List<Symbol> window = new ArrayList<>(windowHeight);
        for (int row = 0; row < windowHeight; row++) {
            window.add(symbols.get((stop + row) % symbols.size()));
        }
        return List.copyOf(window);
    }

    public ReelStrip without(Symbol excludedSymbol) {
        List<Symbol> filtered = symbols.stream()
                .filter(symbol -> !symbol.equals(excludedSymbol))
                .toList();
        if (filtered.isEmpty()) {
            throw new IllegalStateException("Removing the symbol would leave an empty reel");
        }
        return new ReelStrip(filtered);
    }

    public List<Symbol> getSymbols() {
        return symbols;
    }
}
