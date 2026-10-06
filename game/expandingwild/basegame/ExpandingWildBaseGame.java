package game.expandingwild.basegame;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import GameModuleFramework.reels.WildExpansion;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.features.ScatterTrigger;
import game.expandingwild.ExpandingWildWinCalculator;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.config.ExpandingWildConfigAdapter;
import game.expandingwild.features.BannerMultiplierAssignment;
import game.expandingwild.model.ExpandingWildSpinResult;
import game.expandingwild.ExpandingWildReplayPayload;
import game.model.ReplayEvent;
import java.util.Map;
import java.util.List;
import java.util.Random;

/** Executes one expanding wild basegame spin. */
public final class ExpandingWildBaseGame {
    private final ExpandingWildConfig config;
    private final Random random;
    private final List<List<ReelStrip>> reelSets;
    private final List<WeightedTable<Integer>> multiplierTables;
    private final WeightedTable<Integer> setSelectionTable;
    private final ScatterTrigger scatterTrigger;
    private final List<ExpandingWildWinCalculator> winCalculators;

    public ExpandingWildBaseGame(ExpandingWildConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.reelSets = config.baseGame.sets.stream()
                .map(ExpandingWildConfigAdapter::toReelStrips).toList();
        this.multiplierTables = config.baseGame.sets.stream()
                .map(set -> set.bannerMultiplierWeights.isEmpty()
                        ? null : new WeightedTable<>(set.bannerMultiplierWeights))
                .toList();
        this.setSelectionTable = new WeightedTable<>(config.baseGame.setSelectionWeights);
        this.scatterTrigger = new ScatterTrigger(
                ExpandingWildConfig.SCATTER,
                config.freeGameTriggerScatterCount,
                config.scatterReels);
        this.winCalculators = config.baseGame.sets.stream()
                .map(ignored -> new ExpandingWildWinCalculator(
                        config.paytable, config.paylines, ExpandingWildConfig.WILD))
                .toList();
    }

    public ExpandingWildSpinResult spin() {
        return spin(1.0);
    }

    public ExpandingWildSpinResult spin(double totalRoundStake) {
        return spin(totalRoundStake, false);
    }

    public ExpandingWildSpinResult spin(double totalRoundStake, boolean captureReplay) {
        int setIndex = setSelectionTable.draw(random);
        if (captureReplay) {
            ReelGrid.SpinOutcome outcome = ReelGrid.spinWithStops(
                    reelSets.get(setIndex), config.visibleRows, random);
            return evaluate(setIndex, outcome.grid(), outcome.stops(), totalRoundStake, null, true);
        }
        ReelGrid stoppedGrid = ReelGrid.spin(reelSets.get(setIndex), config.visibleRows, random);
        return evaluate(setIndex, stoppedGrid, null, totalRoundStake, null, false);
    }

    /** Evaluates one exact set of reel stops without consuming the random stream. */
    public ExpandingWildSpinResult spinAtStops(int[] reelStops, double totalRoundStake) {
        return spinAtStops(0, reelStops, totalRoundStake);
    }

    /** Evaluates one exact stop combination for a selected configured set. */
    public ExpandingWildSpinResult spinAtStops(int setIndex, int[] reelStops,
            double totalRoundStake) {
        ReelGrid stoppedGrid = ReelGrid.atStops(
                reelSets.get(setIndex), config.visibleRows, reelStops);
        return evaluate(setIndex, stoppedGrid, reelStops, totalRoundStake, null, false);
    }

    public ExpandingWildSpinResult replay(ReplayEvent event, double totalRoundStake) {
        if (!event.type().equals("basegame")) {
            throw new IllegalArgumentException("Expected a basegame replay event");
        }
        ExpandingWildReplayPayload payload = ExpandingWildReplayPayload.fromJson(event.payload());
        return spinAtReplayData(payload, totalRoundStake);
    }

    private ExpandingWildSpinResult spinAtReplayData(
            ExpandingWildReplayPayload payload, double totalRoundStake) {
        return evaluate(payload.setIndex(), ReelGrid.atStops(
                reelSets.get(payload.setIndex()), config.visibleRows, payload.reelStops()),
                payload.reelStops(), totalRoundStake, payload.bannerMultipliers(), true);
    }

    private ExpandingWildSpinResult evaluate(int setIndex, ReelGrid stoppedGrid, int[] reelStops,
            double totalRoundStake, Map<Integer, Integer> recordedMultipliers,
            boolean captureReplay) {
        ScatterTrigger.Result scatterResult = scatterTrigger.evaluate(stoppedGrid);
        WildExpansion.Result expansion = WildExpansion.expandColumns(
                stoppedGrid, ExpandingWildConfig.BANNER, ExpandingWildConfig.WILD);
        WeightedTable<Integer> bannerMultiplierTable = multiplierTables.get(setIndex);
        Map<Integer, Integer> multipliers;
        if (recordedMultipliers != null) {
            multipliers = bannerMultiplierTable == null && recordedMultipliers.isEmpty()
                    ? Map.of()
                    : BannerMultiplierAssignment.useRecordedChoices(expansion, recordedMultipliers);
        } else {
            multipliers = bannerMultiplierTable == null
                    ? Map.of()
                    : BannerMultiplierAssignment.assign(expansion, bannerMultiplierTable, random);
        }
        var lineWins = winCalculators.get(setIndex).calculate(
                expansion.grid(), multipliers, totalRoundStake);

        return new ExpandingWildSpinResult(
                stoppedGrid,
                expansion.grid(),
                expansion.expandedReels(),
                scatterResult.scatterCount(),
                lineWins,
                multipliers,
                scatterResult.triggered(),
                false,
                setIndex,
                captureReplay ? "basegame" : null,
                captureReplay
                        ? new ExpandingWildReplayPayload(setIndex, reelStops, multipliers).toJson()
                        : null);
    }

}
