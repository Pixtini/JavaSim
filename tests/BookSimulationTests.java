import GameModuleFramework.probability.WeightedTable;
import game.*;
import game.model.*;
import simulation.books.*;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.replay.*;
import simulation.reporting.Print;
import simulation.result.SimulationResult;
import toolkit.par.*;
import toolkit.replay.ReplayerMain;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Shared book selection, replay integrity, complete statistics, report and real-game integration tests. */
public final class BookSimulationTests {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("javasim-book-tests-");
        try {
            selectionAndStatistics(root.resolve("selection"));
            invalidInputs(root.resolve("invalid"));
            realGamesAndReports(root.resolve("real"));
            System.out.println("Passed " + assertions + " book simulation assertions.");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
    private static void selectionAndStatistics(Path dir) throws Exception {
        var config = fixture(dir, "<r><a>1</a><b>1</b></r><r><a>2</a><b>3</b></r>"
                + "<r><a>2</a><b>1</b></r><r><a>3</a><b>0</b></r>");
        var game = new FixtureGame(1);
        var book = GameplayBook.load(config, game);
        check(book.metadata().tableRows() == 4 && book.metadata().activeGameplays() == 2, "duplicates aggregate and zero disables");
        check(book.metadata().totalWeight() == 5, "combined weights");
        near(book.metadata().expectedRtp(), 8, "independently known weighted return");
        near(book.metadata().triggerProbability(), .8, "weighted feature probability");
        for (int ticket = 0; ticket < 5; ticket++) {
            final int value = ticket;
            var selected = book.draw(new Random() { @Override public int nextInt(int bound) {
                check(bound == 5, "selector uses exactly total weight"); return value;
            }});
            check(selected.gameplayId() == (ticket == 0 ? 1 : 2), "all selector ticket boundaries");
        }
        var one = new SimulationRunner(config, game).run();
        config.threads = 4;
        var four = new SimulationRunner(config, game).run();
        check(one.getTotalGameStats().getWinDist().equals(four.getTotalGameStats().getWinDist()), "fixed partitions reproduce across workers");
        near(one.getTotalGameStats().getTotalWinnings(), four.getTotalGameStats().getTotalWinnings(), "reproducible totals");
        long wins = one.getTotalGameStats().getWinDist().getOrDefault(10.0, 0L);
        check(one.getTotalGameStats().getWinDist().keySet().equals(Set.of(0.0, 10.0)), "only available positive-weight rounds occur");
        check(one.getTotalGameStats().getRounds() == config.rounds && one.getBaseGameStats().getRounds() == config.rounds, "round counts");
        check(one.getFreeGameStats().getFreegameTriggers() == wins, "trigger stats");
        check(one.getFreeGameStats().getRounds() == 2 * wins, "individual feature-spin stats");
        check(one.getTotalGameStats().getWinCaps() == wins, "cap stats preserved");
        near(one.getBaseGameStats().getTotalWinnings(), 2 * wins, "base total");
        near(one.getFreeGameStats().getTotalWinnings(), 8 * wins, "feature total");
        near(one.getTotalGameStats().getTotalWinnings(), 10 * wins, "combined total");
        check(one.getBaseGameSetStats().get(1).getRounds() == wins, "base set breakdown");
        check(one.getFreeGameSetStats().get(0).getRounds() == 2 * wins, "free set breakdown");
        check(one.getFreeGameStats().getAwardCounts().get("feature") == 2 * wins, "named award stats");
        check(one.getTotalGameStats().getHits() == wins, "hit stats");
        check(one.getFreeGameStats().getWinDist().equals(Map.of(8.0, wins)), "feature distribution counts sessions");
        check(one.getSavedGameplays().size() == config.maxSavedGameplays, "book selection still harvests replays");
        for (var saved : one.getSavedGameplays()) {
            check(saved.gameMode().equals("Normal") && saved.gameId().equals(config.gameId), "harvest identity");
            check(ReplayValidation.matches(saved, game.createSession(new Random()).replayRound(saved.stake(), 10, saved.events())), "harvest replays");
        }
        // Independent cost and stake: replay at stake 2, cost 2x, payout still scales only with base stake.
        config.stake = 2;
        var scaled = new SimulationRunner(config, new FixtureGame(2)).run();
        check(scaled.getTotalGameStats().getWinDist().keySet().equals(Set.of(0.0, 20.0)), "requested stake scales whole cached round");
        near(scaled.getTotalGameStats().getRtp(), one.getTotalGameStats().getRtp() / 2, "purchase cost affects exposure, not pay");
        near(config.bookMetadata.expectedRtp(), 4, "cost-aware book expectation");
        config.maxSavedGameplays = 0;
        check(new SimulationRunner(config, new FixtureGame(2)).run().getSavedGameplays().isEmpty(), "capture can be disabled");
        // When switched off no book file is consulted and the regular session path is used.
        config.useBooks = false;
        rejects(() -> new SimulationRunner(config, game).run(), "fixture fresh-spin path intentionally fails");
    }
    private static void invalidInputs(Path root) throws Exception {
        String[] invalidRows = {
            "", "<r><a>1</a><b>0</b></r>", "<r><a>1</a><b>-1</b></r>",
            "<r><a>0</a><b>1</b></r>", "<r><a>1.5</a><b>1</b></r>",
            "<r><a>1</a><b>0.5</b></r>", "<r><a>1</a><b>9223372036854775808</b></r>",
            "<r><a>1</a><b>9223372036854775807</b></r><r><a>2</a><b>1</b></r>",
            "<r><a>1</a><b>9223372036854775807</b></r><r><a>1</a><b>1</b></r>",
            "<r><a>999</a><b>1</b></r>", "<r><a>1</a></r>",
            "<r><a>1</a><b>1</b></r><r></r><r><a>2</a><b>1</b></r>"
        };
        int index = 0;
        for (String rows : invalidRows) {
            var c = fixture(root.resolve("case" + index++), rows);
            rejects(() -> new SimulationRunner(c, new FixtureGame(1)).run(), "invalid weights/IDs fail before workers");
        }
        var c = fixture(root.resolve("files"), "<r><a>1</a><b>1</b></r>");
        Path csv = c.parWorkbookPath.getParent().resolve("books/saved_gameplays_normal.csv");
        byte[] bytes = Files.readAllBytes(csv);
        Files.delete(csv);
        rejects(() -> new SimulationRunner(c, new FixtureGame(1)).run(), "missing CSV");
        Files.write(csv, bytes);
        c.gameMode = "EM1";
        rejects(() -> new SimulationRunner(c, new FixtureGame(1)).run(), "missing mode pair/book");
        c.gameMode = "Normal";
        writeWorkbook(c.parWorkbookPath, List.of("Normal-Config", "Normal-Reels"), "");
        rejects(() -> new SimulationRunner(c, new FixtureGame(1)).run(), "missing Books sheet");
        writeWorkbook(c.parWorkbookPath, List.of("Normal-Config", "Normal-Reels", "Normal-Books"), "<r><a>1</a><b>1</b></r>");
        String text = new String(bytes, StandardCharsets.UTF_8);
        Files.writeString(csv, text.replace("\"book-fixture\"", "\"another-game\""));
        rejects(() -> new SimulationRunner(c, new FixtureGame(1)).run(), "wrong game");
        Files.writeString(csv, text.replace("\"Normal\"", "\"BB1\""));
        rejects(() -> new SimulationRunner(c, new FixtureGame(1)).run(), "wrong CSV mode");
        Files.writeString(csv, text.replace("\"0.0\"", "\"99.0\""));
        rejects(() -> new SimulationRunner(c, new FixtureGame(1)).run(), "recorded money differs from replay");
        Files.writeString(csv, text);
        rejects(() -> SavedGameplayCsv.readByIds(csv, Set.of(999L)), "bulk reader missing ID");
        check(SavedGameplayCsv.readByIds(csv, Set.of(1L,2L)).size() == 2, "bulk reader loads requested groups once");
        String reordered = text.replace("\"1\",\"fixture-free\"", "\"9\",\"fixture-free\"");
        Files.writeString(csv, reordered);
        rejects(() -> SavedGameplayCsv.readById(csv, 2), "out-of-order events");
        Files.writeString(csv, text);
        Game unsupported = new FixtureGame(1) { @Override public GameSession createSession(Random r) {
            return () -> { throw new AssertionError("No fresh spin allowed"); };
        }};
        rejects(() -> new SimulationRunner(c, unsupported).run(), "game without replay cannot use books");
        Game randomReplay = new FixtureGame(1) { @Override public GameSession createSession(Random r) {
            return new GameSession() {
                @Override public GameRoundResult playRound() { throw new AssertionError(); }
                @Override public GameRoundResult replayRound(double s, double cap, List<ReplayEvent> e) { r.nextInt(); return null; }
            };
        }};
        rejects(() -> new SimulationRunner(c, randomReplay).run(), "replay cannot draw new random events");
        var metadata = GameplayBook.load(c, new FixtureGame(1)).metadata();
        Files.writeString(csv, text + "\n");
        rejects(metadata::verifyInputsUnchanged, "changed input invalidates report provenance");
    }
    private static void realGamesAndReports(Path root) throws Exception {
        Files.createDirectories(root);
        for (String mode : List.of("Normal", "EM1", "BB1")) {
            SimConfig c = new SimConfig(); c.gameId = "G002-ember-forge"; c.gameMode = mode;
            c.parWorkbookPath = Path.of("game/G002-ember-forge/source/PAR.xlsx");
            c.useBooks = true; c.rounds = 2000; c.seed = 8182; c.usePreviousSeed = true;
            c.threads = 2; c.partitions = 8; c.maxSavedGameplays = 10; c.exportReport = true;
            var game = GameFactory.create(c.gameId, c.parWorkbookPath, mode);
            var result = new SimulationRunner(c, game).run();
            check(c.bookMetadata.activeGameplays() > 0, "real mode loads positive-weight supplied rounds");
            near(result.getTotalGameStats().getTotalWinnings(), result.getBaseGameStats().getTotalWinnings()
                    + result.getFreeGameStats().getTotalWinnings(), "real book component accounting");
            var console = new StringWriter();
            Path reports = root.resolve(mode);
            new Print(result, reports).printToConsole(c, new PrintWriter(console));
            Path folder;
            try (var folders = Files.list(reports)) { folder = folders.findFirst().orElseThrow(); }
            Path par = folder.resolve("PAR.xlsx");
            var sheets = ExcelWorkbookReader.read(par).sheets().keySet();
            check(sheets.equals(Set.of(mode+"-Config", mode+"-Reels", mode+"-Books")), "report contains selected mode and book only");
            check(Files.readString(folder.resolve("config.html")).contains(mode + "-Books"), "HTML renders book weights");
            check(console.toString().contains("Book CSV:"), "report retains the book CSV path");
            String reportText = Files.readString(folder.resolve("simulation_stats.txt"));
            for (String hidden : List.of("Logical partitions:", "Use gameplay books:", "Book sheet:",
                    "Book rows / active gameplays:", "Book total weight:", "Expected book RTP:",
                    "Expected book trigger probability:", "Source PAR SHA-256:", "Source book CSV SHA-256:")) {
                check(!console.toString().contains(hidden) && !reportText.contains(hidden),
                        "technical metadata omitted from console and text report: " + hidden);
            }
            for (int id = 1; id <= 10; id++) {
                var output = new ByteArrayOutputStream();
                ReplayerMain.replay(folder.resolve("saved_gameplays.csv"), id, new PrintStream(output));
                check(output.toString(StandardCharsets.UTF_8).contains("Replay validation: PASSED"), "real selected report replay");
            }
            var exported = new SimConfig(); exported.gameId = c.gameId; exported.gameMode = mode;
            exported.parWorkbookPath = par; exported.stake = 1;
            var reproducedBook = GameplayBook.load(exported, GameFactory.create(c.gameId, par, mode));
            near(reproducedBook.metadata().expectedRtp(), c.bookMetadata.expectedRtp(), "exported book bundle remains runnable");
        }
    }
    private static SimConfig fixture(Path dir, String rows) throws Exception {
        Files.createDirectories(dir.resolve("books"));
        Path workbook = dir.resolve("PAR.xlsx");
        writeWorkbook(workbook, List.of("Normal-Config", "Normal-Reels", "Normal-Books"), rows);
        var records = new ArrayList<SavedGameplay>(); var game = new FixtureGame(1);
        for (long id = 1; id <= 3; id++) {
            List<ReplayEvent> events = switch ((int)id) {
                case 1 -> List.of(new ReplayEvent("fixture-base", 0, "loss"));
                case 2 -> List.of(new ReplayEvent("fixture-base", 2, "entry"),
                        new ReplayEvent("fixture-free", 3, "3"), new ReplayEvent("fixture-free", 5, "5"));
                default -> List.of(new ReplayEvent("fixture-base", 4, "excluded"));
            };
            records.add(new SavedGameplay(id, "book-fixture", "Normal", 1, id==1 ? 0 : id==2 ? 10 : 4,
                    id==1 ? 0 : id==2 ? 2 : 4, id==2 ? 8 : 0, events));
        }
        SavedGameplayCsv.write(dir.resolve("books/saved_gameplays_normal.csv"), records);
        SimConfig c = new SimConfig(); c.gameId = "book-fixture"; c.parWorkbookPath = workbook;
        c.rounds = 10000; c.seed = 91234; c.usePreviousSeed = true; c.threads = 1; c.partitions = 8;
        c.useBooks = true; c.exportReport = false; c.maxSavedGameplays = 10; return c;
    }
    /** Minimal independent workbook fixture, with table rows described by small XML fragments. */
    private static void writeWorkbook(Path path, List<String> names, String rows) throws Exception {
        String ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
        StringBuilder sheets = new StringBuilder(), rels = new StringBuilder(), overrides = new StringBuilder();
        try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (int i=0; i<names.size(); i++) {
                sheets.append("<sheet name=\"").append(names.get(i)).append("\" sheetId=\"").append(i+1).append("\" r:id=\"rId").append(i+1).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(i+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i+1).append(".xml\"/>");
                overrides.append("<Override PartName=\"/xl/worksheets/sheet").append(i+1).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                StringBuilder cells = new StringBuilder();
                if (names.get(i).endsWith("-Books")) {
                    cells.append("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Gameplay ID</t></is></c><c r=\"B1\" t=\"inlineStr\"><is><t>Weight</t></is></c></row>");
                    int row=2;
                    var matcher=java.util.regex.Pattern.compile("<r>(.*?)</r>").matcher(rows);
                    while (matcher.find()) {
                        String part=matcher.group(1); cells.append("<row r=\"").append(row).append("\">");
                        for (String[] tag : new String[][]{{"a","A"},{"b","B"}}) {
                            var value=java.util.regex.Pattern.compile("<"+tag[0]+">(.*?)</"+tag[0]+">").matcher(part);
                            if (value.find()) cells.append("<c r=\"").append(tag[1]).append(row).append("\"><v>").append(value.group(1)).append("</v></c>");
                        }
                        cells.append("</row>"); row++;
                    }
                }
                entry(zip, "xl/worksheets/sheet"+(i+1)+".xml", "<worksheet xmlns=\""+ns+"\"><sheetData>"+cells+"</sheetData></worksheet>");
            }
            entry(zip,"xl/workbook.xml", "<workbook xmlns=\""+ns+"\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"+sheets+"</sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"+rels+"</Relationships>");
            entry(zip,"[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"xml\" ContentType=\"application/xml\"/><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"+overrides+"</Types>");
        }
    }
    private static void entry(ZipOutputStream out, String name, String text) throws IOException {
        out.putNextEntry(new ZipEntry(name)); out.write(text.getBytes(StandardCharsets.UTF_8)); out.closeEntry();
    }
    private static class FixtureGame implements Game {
        private final double cost;
        FixtureGame(double cost) { this.cost=cost; }
        @Override public int[] getPaytable() { return new int[2]; }
        @Override public List<String> getAwardLabels() { return List.of("base", "feature"); }
        @Override public int getBaseGameSetCount() { return 2; }
        @Override public int getFreeGameSetCount() { return 1; }
        @Override public double getMaxWinMultiplier() { return 10; }
        @Override public double getCostMultiplier() { return cost; }
        @Override public GameSession createSession(Random random) {
            return new GameSession() {
                @Override public GameRoundResult playRound() { throw new IllegalStateException("Fresh generation called"); }
                @Override public GameRoundResult replayRound(double stake, double cap, List<ReplayEvent> events) {
                    String type = events.get(0).payload();
                    boolean trigger=type.equals("entry");
                    double base=trigger ? 2 : type.equals("excluded") ? 4 : 0;
                    var bg=new SpinResult(base*stake, base>0?1:0, trigger, base>0?List.of("base"):List.of(), trigger?1:0,
                            new ReplayEvent("fixture-base",base*stake,type));
                    List<SpinResult> features=new ArrayList<>();
                    if (trigger) for (double win : new double[]{3,5}) features.add(new SpinResult(win*stake,1,false,
                            List.of("feature"),0,new ReplayEvent("fixture-free",win*stake, Integer.toString((int)win))));
                    return new GameRoundResult(bg,features).cappedAt(cap);
                }
            };
        }
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void rejects(Action action, String message) {
        assertions++;
        try { action.run(); } catch (Exception expected) { return; }
        throw new AssertionError("Expected rejection: " + message);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual-expected)<=1e-8*Math.max(1,Math.abs(expected)), message+": "+actual+" vs "+expected);
    }
    private static void check(boolean condition, String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
}
