package game.expandingwild.model;

import GameModuleFramework.reels.ReelGrid;
import game.model.SpinResult;
import game.model.ReplayEvent;
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
        this(stoppedGrid, expandedGrid, expandedBannerReels, scatterCount, lineWins,
                bannerMultipliersByReel, triggersFreeGames, freeGameSpin, -1,
                totalWin(lineWins), null);
    }

    public ExpandingWildSpinResult(ReelGrid stoppedGrid, ReelGrid expandedGrid,
            Set<Integer> expandedBannerReels, int scatterCount,
            List<ExpandingWildLineWin> lineWins,
            Map<Integer, Integer> bannerMultipliersByReel,
            boolean triggersFreeGames, boolean freeGameSpin, int setIndex) {
        this(stoppedGrid, expandedGrid, expandedBannerReels, scatterCount, lineWins,
                bannerMultipliersByReel, triggersFreeGames, freeGameSpin, setIndex, null, null);
    }

    public ExpandingWildSpinResult(ReelGrid stoppedGrid, ReelGrid expandedGrid,
            Set<Integer> expandedBannerReels, int scatterCount,
            List<ExpandingWildLineWin> lineWins,
            Map<Integer, Integer> bannerMultipliersByReel,
            boolean triggersFreeGames, boolean freeGameSpin, int setIndex,
            String replayType, String replayPayload) {
        this(stoppedGrid, expandedGrid, expandedBannerReels, scatterCount, lineWins,
                bannerMultipliersByReel, triggersFreeGames, freeGameSpin, setIndex,
                totalWin(lineWins), replayType == null ? null
                        : new ReplayEvent(replayType, totalWin(lineWins), replayPayload));
    }

    private ExpandingWildSpinResult(ReelGrid stoppedGrid, ReelGrid expandedGrid,
            Set<Integer> expandedBannerReels, int scatterCount,
            List<ExpandingWildLineWin> lineWins,
            Map<Integer, Integer> bannerMultipliersByReel,
            boolean triggersFreeGames, boolean freeGameSpin, int setIndex, double totalWin,
            ReplayEvent replayEvent) {
        super(totalWin, totalWin > 0.0 ? 1 : 0,
                triggersFreeGames, awardLabels(lineWins), setIndex, replayEvent);
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

    @Override
    public ExpandingWildSpinResult cappedAt(double maximumWin) {
        double total = getWin();
        if (total <= maximumWin) {
            return this;
        }

        double remaining = maximumWin;
        java.util.ArrayList<ExpandingWildLineWin> cappedWins = new java.util.ArrayList<>();
        for (ExpandingWildLineWin lineWin : lineWins) {
            if (remaining <= 0.0) {
                break;
            }
            double cappedLineWin = Math.min(lineWin.totalWin(), remaining);
            double cappedBaseWin = lineWin.multiplier() == 0.0
                    ? 0.0 : cappedLineWin / lineWin.multiplier();
            cappedWins.add(new ExpandingWildLineWin(lineWin.paylineNumber(), lineWin.symbol(),
                    lineWin.matchingReels(), cappedBaseWin, lineWin.multiplier(), cappedLineWin,
                    lineWin.symbolsOnLine()));
            remaining -= cappedLineWin;
        }
        return new ExpandingWildSpinResult(stoppedGrid, expandedGrid, zeroBasedReels(),
                scatterCount, cappedWins, zeroBasedMultipliers(), hasFreeSpin(), freeGameSpin,
                getSetIndex(), maximumWin,
                getReplayEvent().map(event -> event.withWin(maximumWin)).orElse(null));
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

    private Map<Integer, Integer> zeroBasedMultipliers() {
        return bannerMultipliersByReel.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.getKey() - 1, Map.Entry::getValue));
    }

    private Set<Integer> zeroBasedReels() {
        return expandedBannerReels.stream().map(reel -> reel - 1)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
