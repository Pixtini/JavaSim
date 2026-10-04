package game.expandingwild.model;

import GameModuleFramework.reels.ReelGrid;
import game.model.SpinResult;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Detailed expanding wild outcome; the parent result exposes the simulator fields. */
public final class ExpandingWildSpinResult extends SpinResult {
    private final ReelGrid stoppedGrid;
    private final ReelGrid expandedGrid;
    private final Set<Integer> expandedBannerReels;
    private final int scatterCount;
    private final List<ExpandingWildLineWin> lineWins;
    private final Map<Integer, Integer> bannerMultipliersByReel;
    private final boolean freeGameSpin;

    public ExpandingWildSpinResult(ReelGrid stoppedGrid, ReelGrid expandedGrid,
            Set<Integer> expandedBannerReels, int scatterCount,
            List<ExpandingWildLineWin> lineWins,
            Map<Integer, Integer> bannerMultipliersByReel,
            boolean triggersFreeGames, boolean freeGameSpin) {
        super(totalWin(lineWins), totalWin(lineWins) > 0.0 ? 1 : 0,
                triggersFreeGames, awardLabels(lineWins));
        this.stoppedGrid = stoppedGrid;
        this.expandedGrid = expandedGrid;
        this.expandedBannerReels = oneBased(expandedBannerReels);
        this.scatterCount = scatterCount;
        this.lineWins = List.copyOf(lineWins);
        this.bannerMultipliersByReel = oneBased(bannerMultipliersByReel);
        this.freeGameSpin = freeGameSpin;
    }

    public ReelGrid getStoppedGrid() {
        return stoppedGrid;
    }

    public ReelGrid getExpandedGrid() {
        return expandedGrid;
    }

    /** Reel indexes are one-based for readable reports and diagnostics. */
    public Set<Integer> getExpandedBannerReels() {
        return expandedBannerReels;
    }

    public int getScatterCount() {
        return scatterCount;
    }

    public List<ExpandingWildLineWin> getLineWins() {
        return lineWins;
    }

    /** Reel keys are one-based for readable reports and diagnostics. */
    public Map<Integer, Integer> getBannerMultipliersByReel() {
        return bannerMultipliersByReel;
    }

    public boolean isFreeGameSpin() {
        return freeGameSpin;
    }

    private static double totalWin(List<ExpandingWildLineWin> wins) {
        return wins.stream().mapToDouble(ExpandingWildLineWin::totalWin).sum();
    }

    private static List<String> awardLabels(List<ExpandingWildLineWin> wins) {
        return wins.stream().map(ExpandingWildLineWin::awardLabel).toList();
    }

    private static Set<Integer> oneBased(Set<Integer> reelIndexes) {
        return reelIndexes.stream().map(index -> index + 1)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Map<Integer, Integer> oneBased(Map<Integer, Integer> reelMultipliers) {
        return reelMultipliers.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                entry -> entry.getKey() + 1, Map.Entry::getValue));
    }
}
