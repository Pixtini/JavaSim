package GameModuleFramework.paylines;

import GameModuleFramework.symbols.Symbol;

/** One configured symbol and match-count payout category. */
public record PaytableAward(Symbol symbol, int matchingSymbols, double payout) {}
