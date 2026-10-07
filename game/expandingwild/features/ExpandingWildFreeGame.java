package game.expandingwild.features;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import GameModuleFramework.reels.WildExpansion;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.features.SymbolInsertion;
import game.expandingwild.ExpandingWildWinCalculator;
import game.expandingwild.ExpandingWildReplayPayload;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.config.ExpandingWildConfigAdapter;
import game.expandingwild.model.ExpandingWildSpinResult;
import game.model.ReplayEvent;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Plays free spins on scatter-free reels with random expanding-banner multipliers. */
public final class ExpandingWildFreeGame {
    private final ExpandingWildConfig config;
    private final Random random;
    private final List<List<ReelStrip>> freeGameReelSets;
    private final List<WeightedTable<Integer>> bannerMultiplierTables;
    private final List<ExpandingWildWinCalculator> winCalculators;
    private final WeightedTable<Integer> setSelectionTable;
    private final List<List<SymbolInsertion.Rule>> insertionRules;

    public ExpandingWildFreeGame(ExpandingWildConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.freeGameReelSets = config.freeGame.sets.stream()
                .map(ExpandingWildConfigAdapter::toReelStrips).toList();
        this.bannerMultiplierTables = config.freeGame.sets.stream()
                .map(set -> set.bannerMultiplierWeights.isEmpty()
                        ? null : new WeightedTable<>(set.bannerMultiplierWeights))
                .toList();
        this.winCalculators = config.freeGame.sets.stream()
                .map(ignored -> new ExpandingWildWinCalculator(
                        config.paytable, config.paylines, ExpandingWildConfig.WILD))
                .toList();
        this.setSelectionTable = new WeightedTable<>(config.freeGame.setSelectionWeights);
        this.insertionRules = config.freeGame.sets.stream()
                .map(set -> set.symbolInsertions)
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
                    freeGameReelSets.get(setIndex), config.visibleRows, random);
            return evaluate(setIndex, outcome.grid(), outcome.stops(), totalRoundStake,
                    null, null, true);
        }
        ReelGrid stoppedGrid = ReelGrid.spin(
                freeGameReelSets.get(setIndex), config.visibleRows, random);
        return evaluate(setIndex, stoppedGrid, null, totalRoundStake, null, null, false);
    }

    public ExpandingWildSpinResult replay(ReplayEvent event, double totalRoundStake) {
        if (!event.type().equals("freegame")) {
            throw new IllegalArgumentException("Expected a freegame replay event");
        }
        ExpandingWildReplayPayload payload = ExpandingWildReplayPayload.fromJson(event.payload());
        ReelGrid stoppedGrid = ReelGrid.atStops(
                freeGameReelSets.get(payload.setIndex()), config.visibleRows, payload.reelStops());
        return evaluate(payload.setIndex(), stoppedGrid, payload.reelStops(), totalRoundStake,
                payload.bannerMultipliers(), payload.insertions(), true);
    }

    private ExpandingWildSpinResult evaluate(int setIndex, ReelGrid stoppedGrid,
            int[] reelStops, double totalRoundStake,
            Map<Integer, Integer> recordedMultipliers,
            List<SymbolInsertion.Placement> recordedInsertions, boolean captureReplay) {
        SymbolInsertion.Result insertionResult = recordedInsertions == null
                ? SymbolInsertion.apply(stoppedGrid, insertionRules.get(setIndex), random)
                : SymbolInsertion.applyRecorded(
                        stoppedGrid, insertionRules.get(setIndex), recordedInsertions);
        ReelGrid insertedGrid = insertionResult.grid();
        WildExpansion.Result expansion = WildExpansion.expandColumns(
                insertedGrid, ExpandingWildConfig.BANNER, ExpandingWildConfig.WILD);
        WeightedTable<Integer> bannerMultiplierTable = bannerMultiplierTables.get(setIndex);
        Map<Integer, Integer> multipliers;
        if (recordedMultipliers != null) {
            multipliers = BannerMultiplierAssignment.useRecordedChoices(
                    expansion, recordedMultipliers);
        } else {
            multipliers = bannerMultiplierTable == null
                    ? Map.of()
                    : BannerMultiplierAssignment.assign(expansion, bannerMultiplierTable, random);
        }
        var lineWins = winCalculators.get(setIndex).calculate(
                expansion.grid(), multipliers, totalRoundStake);

        return new ExpandingWildSpinResult(
                insertedGrid,
                expansion.grid(),
                expansion.expandedReels(),
                0,
                lineWins,
                multipliers,
                false,
                true,
                setIndex,
                captureReplay ? "freegame" : null,
                captureReplay
                        ? new ExpandingWildReplayPayload(setIndex, reelStops, multipliers,
                                insertionResult.placements()).toJson()
                        : null);
    }

}
