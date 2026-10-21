package game.ember_forge.tests;

import GameModuleFramework.reels.ReelGrid;
import game.GameFactory;
import game.ember_forge.*;
import game.ember_forge.basegame.EmberForgeBaseGame;
import game.ember_forge.config.*;
import game.ember_forge.features.EmberForgeFreeGame;
import game.ember_forge.model.EmberForgeSpin;
import game.model.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.zip.*;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.result.SimulationResult;
import toolkit.player.*;

/** Deterministic source-rule, mode, replay, accounting and integration checks for G002. */
public final class EmberForgeGameTests {
    private static int assertions;
    private static final EmberForgeParParser PARSER = new EmberForgeParParser();
    private static EmberForgeConfig normal;
    public static void main(String[] args) throws Exception {
        normal = PARSER.parse(EmberForgeConfig.DEFAULT_PAR, "Normal");
        parserAndModes(); entryScreens(); evaluationAndHeat(); sessionsCapsAndReplay();
        adaptersAndSimulation();
        System.out.println("Passed " + assertions + " Ember Forge assertions.");
    }
    private static void parserAndModes() throws Exception {
        var ante = PARSER.parse(EmberForgeConfig.DEFAULT_PAR, "EM1");
        var buy = PARSER.parse(EmberForgeConfig.DEFAULT_PAR, "BB1");
        near(ante.costMultiplier(), 2, "EM1 cost"); near(buy.costMultiplier(), 47, "BB1 cost");
        near(ante.triggerProbability(), 4 * normal.triggerProbability(), "exact EM1 odds");
        near(buy.triggerProbability(), 1, "buy guaranteed");
        check(normal.freeReels().equals(ante.freeReels()) && normal.freeReels().equals(buy.freeReels()),
                "identical Freegame across modes");
        rejects(() -> PARSER.parse(EmberForgeConfig.DEFAULT_PAR, "missing"), "unknown mode");
        malformed("Normal-Config", x -> x.replace("<c r=\"B3\"><v>1</v>",
                "<c r=\"B3\"><v>2</v>"), "Normal cost");
        malformed("EM1-Config", x -> x.replace("<c r=\"B36\"><v>4</v>",
                "<c r=\"B36\"><v>3</v>"), "EM1 trigger weights");
        malformed("EM1-Reels", x -> x.replace("<c r=\"M3\"><v>6</v>",
                "<c r=\"M3\"><v>5</v>"), "divergent feature reels");
        malformed("Normal-Reels", x -> x.replace("<c r=\"A3\"><v>1</v>",
                "<c r=\"A3\"><v>8</v>"), "scatter outside entry");
        malformed("Normal-Reels", x -> x.replace("</row>",
                "<c r=\"S1\" t=\"inlineStr\"><is><t>FG-1</t></is></c></row>"), "extra set");
        malformed("xl/workbook.xml", x -> x.replace("name=\"BB1-Reels\"", "name=\"Other-Reels\""),
                "incomplete mode pair");
        rejects(() -> normal.baseReels().get(0).add(8), "immutable reels");
    }
    private static void malformed(String entry, UnaryOperator<String> change, String name) throws Exception {
        Path temporary = Files.createTempFile("ember-invalid-", ".xlsx");
        try {
            try (var zip = new ZipFile(EmberForgeConfig.DEFAULT_PAR.toFile());
                    var out = new ZipOutputStream(Files.newOutputStream(temporary))) {
                String target = resolveSheet(zip, entry);
                var entries = zip.entries(); boolean modified = false;
                while (entries.hasMoreElements()) {
                    var item = entries.nextElement(); byte[] content = zip.getInputStream(item).readAllBytes();
                    if (item.getName().equals(target)) {
                        String original = new String(content, StandardCharsets.UTF_8);
                        String replacement = change.apply(original);
                        check(!replacement.equals(original), "fixture actually changes " + name);
                        content = replacement.getBytes(StandardCharsets.UTF_8); modified = true;
                    }
                    out.putNextEntry(new ZipEntry(item.getName())); out.write(content); out.closeEntry();
                }
                check(modified, "fixture entry exists");
            }
            rejects(() -> PARSER.parse(temporary, "Normal"), name);
        } finally { Files.deleteIfExists(temporary); }
    }
    private static String resolveSheet(ZipFile zip, String name) throws Exception {
        if (name.equals("xl/workbook.xml")) return name;
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var builder = factory.newDocumentBuilder();
        var workbook = builder.parse(zip.getInputStream(zip.getEntry("xl/workbook.xml")));
        var sheets = workbook.getElementsByTagNameNS("*", "sheet");
        String relation = null;
        for (int i = 0; i < sheets.getLength(); i++) {
            var sheet = (org.w3c.dom.Element) sheets.item(i);
            if (sheet.getAttribute("name").equals(name)) relation = sheet.getAttributeNS(
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");
        }
        var relationships = builder.parse(zip.getInputStream(zip.getEntry("xl/_rels/workbook.xml.rels")))
                .getElementsByTagNameNS("*", "Relationship");
        for (int i = 0; i < relationships.getLength(); i++) {
            var item = (org.w3c.dom.Element) relationships.item(i);
            if (item.getAttribute("Id").equals(relation))
                return Path.of("xl").resolve(item.getAttribute("Target")).normalize().toString();
        }
        throw new AssertionError("Missing fixture sheet " + name);
    }
    private static void entryScreens() {
        var base = new EmberForgeBaseGame(normal);
        for (int index = 0; index < 243; index++) {
            int[] stops = new int[5]; int value = index;
            for (int i = 0; i < 5; i++) { stops[i] = value % 3; value /= 3; }
            var spin = base.evaluate(1, stops, 1, true);
            int scatters = 0;
            for (int r = 0; r < 5; r++) for (int row = 0; row < 3; row++)
                if (spin.grid().getSymbol(r, row).equals(EmberForgeConfig.SCATTER)) scatters++;
            check(scatters == 3 && spin.hasFreeSpin(), "every entry has exactly three scatters");
            near(spin.getWin(), 0, "entry never pays lines");
        }
        for (String mode : List.of("Normal", "EM1", "BB1")) {
            try {
                var c = PARSER.parse(EmberForgeConfig.DEFAULT_PAR, mode);
                // Enumerate every first RNG ticket, not a noisy frequency estimate.
                int triggers = 0;
                for (int ticket = 0; ticket < c.triggerDenominator(); ticket++) {
                    var spin = new EmberForgeBaseGame(c).spin(new TicketRandom(ticket), 1, false);
                    if (spin.hasFreeSpin()) triggers++;
                }
                check(triggers == c.triggerWeight(), mode + " exact selector tickets");
            } catch (Exception e) { throw new AssertionError(e); }
        }
    }
    private static void evaluationAndHeat() {
        var evaluator = new EmberForgeEvaluator(normal);
        var wilds = normal.strips(Collections.nCopies(5, List.of(7)));
        var grid = ReelGrid.atStops(wilds, 3, new int[5]);
        var spin = evaluator.evaluate(grid, 1, 0, true, 3, new int[5], false);
        near(spin.getWin(), 810, "all-wild chooses best 5oak on all ten lines at current Heat");
        check(spin.heat() == 3 && spin.nextHeat() == 4, "win raises next Heat only");
        near(evaluator.evaluate(grid, 2, 0, true, 5, new int[5], false).getWin(), 2700,
                "stake and Heat multiplication; maximum Heat");
        var clipped = spin.cappedAt(12.34);
        near(clipped.getWin(), 12.34, "spin clip");
        near(clipped.lineWins().stream().mapToDouble(w -> w.totalWin()).sum(), 12.34, "line detail clipped");
        var free = new EmberForgeFreeGame(normal);
        int[] loss = findFree(false), win = findFree(true);
        check(free.evaluate(loss, 1, 4, false).nextHeat() == 4, "loss preserves Heat");
        near(free.evaluate(win, 1, 5, false).getWin(), free.evaluate(win, 1, 1, false).getWin() * 5,
                "multiplier independent of payout selection");
        check(free.evaluate(win, 1, 5, false).nextHeat() == 5, "Heat saturates");
        rejects(() -> free.evaluate(win, 1, 0, false), "invalid Heat");
        rejects(() -> free.evaluate(new int[]{99,0,0,0,0}, 1, 1, false), "invalid stop");
    }
    private static int[] findFree(boolean winning) {
        var free = new EmberForgeFreeGame(normal);
        for (int i = 0; i < 248832; i++) {
            int value = i; int[] stops = new int[5];
            for (int r = 0; r < 5; r++) { stops[r] = value % 12; value /= 12; }
            if ((free.evaluate(stops, 1, 1, false).getWin() > 0) == winning) return stops;
        }
        throw new AssertionError("Missing fixture");
    }
    private static void sessionsCapsAndReplay() throws Exception {
        var buy = PARSER.parse(EmberForgeConfig.DEFAULT_PAR, "BB1");
        var session = new EmberForgeSession(buy, new Random(55));
        for (int n = 0; n < 30; n++) {
            var round = session.playRound(1.25, 10125, true);
            check(round.getFreeGameResults().size() == 8, "exactly eight spins");
            check(((EmberForgeSpin) round.getFreeGameResults().get(0)).heat() == 1, "Heat resets every round");
            near(total(round), round.getFreeGameResults().stream().mapToDouble(SpinResult::getWin).sum(),
                    "nonpay entry accounting");
            var replay = new EmberForgeSession(buy, new NoRandom()).replayRound(1.25, 10125, round.getReplayEvents());
            near(total(replay), total(round), "replay amount");
            check(replay.getReplayEvents().equals(round.getReplayEvents()), "replay screens and state");
            var forged = round.getReplayEvents().stream().map(e -> e.withWin(999999)).toList();
            near(total(session.replayRound(1.25, 10125, forged)), total(round), "replay recalculates recorded money");
            rejects(() -> session.replayRound(1.25, 10125, round.getReplayEvents().subList(0, 8)),
                    "truncated feature rejected");
        }
        // Repeated guaranteed wins reach the exact attainable 8100x bound at Heat 1,2,3,4,5,5,5,5.
        int[] emberStops = {0,10,8,6,4};
        List<ReplayEvent> maximum = new ArrayList<>();
        maximum.add(new ReplayEvent("emberforge-base", 0, "v1;1;0,0,0,0,0"));
        for (int i = 0; i < 8; i++) maximum.add(new ReplayEvent("emberforge-free", 0,
                "v1;0;" + String.join(",", Arrays.stream(emberStops).mapToObj(Integer::toString).toList())));
        var max = session.replayRound(1, 8100, maximum);
        near(total(max), 8100, "attainable full-round maximum"); check(max.isWinCapReached(), "max cap flag");
        near(total(new EmberForgeSession(buy, new MaximumRandom()).playRound(1, 8100)), 8100,
                "generated maximum round agrees with replay");
        var generatedCap = new EmberForgeSession(buy, new MaximumRandom()).playRound(1, .13, true);
        near(total(generatedCap), .13, "generated round clips at lower cap");
        check(generatedCap.getFreeGameResults().size() == 1 && generatedCap.isWinCapReached(),
                "generation stops immediately at cap");
        var capEvents = maximum.subList(0, 2);
        var capped = session.replayRound(1, .13, capEvents);
        near(total(capped), .13, "cap stops feature immediately");
        near(((EmberForgeSpin)capped.getFreeGameResults().get(0)).lineWins().stream()
                .mapToDouble(w -> w.totalWin()).sum(), .13, "capped detail reconciles");
        check(capped.isWinCapReached(), "external cap flag");
        rejects(() -> session.replayRound(1, .13, maximum), "events after cap rejected");
        rejects(() -> session.replayRound(1, 8100, List.of(new ReplayEvent("emberforge-base", 0,
                "v1;0;0,0,0,0,0"))), "BB1 requires entry");
        rejects(() -> session.replayRound(1, 8100, List.of(new ReplayEvent("emberforge-base", 0,
                "v2;1;0,0,0,0,0"))), "replay version");
        for (double invalid : new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            rejects(() -> session.playRound(invalid, 8100), "invalid stake");
            rejects(() -> session.playRound(1, invalid), "invalid cap");
        }
    }
    private static void adaptersAndSimulation() throws Exception {
        for (String mode : List.of("Normal", "EM1", "BB1")) {
            var c = PARSER.parse(EmberForgeConfig.DEFAULT_PAR, mode);
            Path exported = Files.createTempFile("ember-selected-", ".xlsx");
            try {
                toolkit.par.ParWorkbookSheetExporter.export(EmberForgeConfig.DEFAULT_PAR, exported,
                        Set.of(mode + "-Config", mode + "-Reels"));
                check(PARSER.parse(exported, mode).equals(c), "selected report pair reproduces config");
                var replayGame = GameFactory.create(EmberForgeConfig.ID, exported, mode);
                var recorded = replayGame.createSession(new Random(12)).playRound(1, 8100, true);
                near(total(replayGame.createSession(new NoRandom()).replayRound(1, 8100, recorded.getReplayEvents())),
                        total(recorded), "report workbook replay");
            } finally { Files.deleteIfExists(exported); }
            var a = new EmberForgePlayerAdapter(c, new Random(86));
            var s = new EmberForgeSession(c, new Random(86));
            near(a.costMultiplier(), c.costMultiplier(), "player mode price");
            for (int i = 0; i < 200; i++) {
                var shown = a.spin(1); var actual = s.playRound(1);
                near(shown.totalWin(), total(actual), "player uses same actual session math");
                check(shown.baseGame().reelCount() == 5 && shown.baseGame().rowCount() == 3, "player grid");
                for (var spin : shown.featureSpins()) {
                    check(spin.details().contains("Heat"), "visible Heat");
                    near(spin.wins().stream().mapToDouble(PlayerDisplayData.Win::amount).sum(), spin.totalWin(),
                            "displayed line totals");
                }
            }
            var one = run(mode, 1); var four = run(mode, 4);
            near(one.getTotalGameStats().getTotalWinnings(), four.getTotalGameStats().getTotalWinnings(),
                    "same logical partitions reproduce across worker counts");
            check(one.getFreeGameStats().getFreegameTriggers() == four.getFreeGameStats().getFreegameTriggers(),
                    "reproducible triggers");
            near(one.getTotalGameStats().getTotalWinnings(), one.getBaseGameStats().getTotalWinnings()
                    + one.getFreeGameStats().getTotalWinnings(), "simulation component accounting");
            near(one.getTotalGameStats().getRtp(), one.getTotalGameStats().getTotalWinnings() / (2000 * c.costMultiplier()),
                    "RTP uses purchase cost");
            check(one.getFreeGameStats().getRounds() == 8 * one.getFreeGameStats().getFreegameTriggers(),
                    "simulation eight feature spins per trigger");
            for (var saved : one.getSavedGameplays()) {
                var replay = new EmberForgeSession(c, new NoRandom()).replayRound(saved.stake(), 8100, saved.events());
                near(total(replay), saved.totalWin(), "saved gameplay replay");
            }
        }
    }
    private static SimulationResult run(String mode, int workers) throws Exception {
        SimConfig c = new SimConfig(); c.gameId = EmberForgeConfig.ID; c.gameMode = mode;
        c.parWorkbookPath = EmberForgeConfig.DEFAULT_PAR; c.rounds = 2000; c.stake = 1;
        c.seed = 7194; c.usePreviousSeed = true; c.threads = workers; c.partitions = 8;
        c.exportReport = false; c.maxSavedGameplays = 10;
        return new SimulationRunner(c, GameFactory.create(c.gameId, c.parWorkbookPath, mode)).run();
    }
    private static double total(GameRoundResult round) {
        return round.getBaseGameResult().getWin() + round.getFreeGameResults().stream().mapToDouble(SpinResult::getWin).sum();
    }
    private static final class TicketRandom extends Random {
        private final int ticket; private boolean first = true;
        TicketRandom(int ticket) { this.ticket = ticket; }
        @Override public int nextInt(int bound) { if (first) { first = false; return ticket; } return 0; }
    }
    private static final class NoRandom extends Random {
        @Override protected int next(int bits) { throw new AssertionError("Replay consumed RNG"); }
    }
    private static final class MaximumRandom extends Random {
        private int draw;
        @Override public int nextInt(int bound) {
            int index = draw++;
            return index < 6 ? 0 : new int[]{0,10,8,6,4}[(index - 6) % 5];
        }
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void rejects(Action action, String message) {
        assertions++;
        try { action.run(); } catch (IllegalArgumentException | UnsupportedOperationException | java.io.IOException expected) { return; }
        catch (Exception unexpected) { throw new AssertionError(message, unexpected); }
        throw new AssertionError("Expected rejection: " + message);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) <= 1e-8 * Math.max(1, Math.abs(expected)), message + ": " + actual + " vs " + expected);
    }
    private static void check(boolean condition, String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
}
