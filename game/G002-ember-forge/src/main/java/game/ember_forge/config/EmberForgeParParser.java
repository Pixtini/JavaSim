package game.ember_forge.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import toolkit.par.ExcelWorkbookReader;
import toolkit.par.ParWorkbookModes;

/** Reads the documented G002 table schema and checks cross-mode invariants without merging sheets. */
public final class EmberForgeParParser {
    public EmberForgeConfig parse(Path path, String mode) throws IOException {
        var workbook = ExcelWorkbookReader.read(path);
        var available = ParWorkbookModes.availableModes(workbook);
        if (available.isEmpty() || !Set.of("Normal", "EM1", "BB1").containsAll(available))
            throw new IllegalArgumentException("G002 supports Normal, EM1 and BB1 Config/Reels pairs");
        List<EmberForgeConfig> configs = new ArrayList<>();
        for (String name : available) configs.add(readMode(workbook, name));
        // Reports intentionally retain only the selected mode pair; compare all pairs that are present.
        var normal = configs.get(0);
        for (var c : configs) {
            if (c.triggerDenominator() != normal.triggerDenominator()
                    || c.freeSpins() != normal.freeSpins() || c.maxHeat() != normal.maxHeat()
                    || c.maxWinMultiplier() != normal.maxWinMultiplier()
                    || !c.payouts().equals(normal.payouts()) || !c.lineRows().equals(normal.lineRows())
                    || !c.baseReels().equals(normal.baseReels()) || !c.entryReels().equals(normal.entryReels())
                    || !c.freeReels().equals(normal.freeReels()))
                throw new IllegalArgumentException("G002 modes may differ only in cost and Basegame set weights");
        }
        String selected = ParWorkbookModes.select(workbook, mode).mode();
        return configs.stream().filter(c -> c.mode().equals(selected)).findFirst().orElseThrow();
    }

    private EmberForgeConfig readMode(ExcelWorkbookReader.Workbook workbook, String mode) {
        var pair = ParWorkbookModes.select(workbook, mode);
        var sheet = workbook.requireSheet(pair.configSheet());
        var reels = workbook.requireSheet(pair.reelsSheet());
        for (var row : reels.rows().entrySet()) {
            for (var cell : row.getValue().entrySet()) {
                int col = cell.getKey();
                if (col < 0 || col > 16 || col == 5 || col == 11
                        || (row.getKey() == 1 && col != 0 && col != 6 && col != 12))
                    throw new IllegalArgumentException("Unexpected G002 reel column or extra set");
            }
        }
        int start = title(sheet, "Game-Wide-Config", 0);
        Map<String, String> settings = new HashMap<>();
        for (int r = start + 1; !sheet.cell(r, 0).isBlank(); r++)
            if (settings.put(sheet.cell(r, 0), sheet.cell(r, 1)) != null)
                throw new IllegalArgumentException("Duplicate G002 game-wide key");
        if (!required(settings, "screen").equals("5x3"))
            throw new IllegalArgumentException("G002 screen must be 5x3");
        List<List<Double>> payouts = new ArrayList<>();
        start = title(sheet, "PAYTABLE", 0);
        requireHeader(sheet, start + 1, List.of("Symbol-ID", "Symbol-Name", "Type", "3oak", "4oak", "5oak"));
        for (int id = 1; id <= 8; id++) {
            int row = start + 1 + id;
            String type = id <= 6 ? "SIMPLE" : id == 7 ? "WILD" : "FEATURE";
            if (integer(sheet.cell(row, 0)) != id
                    || !sheet.cell(row, 1).equals(EmberForgeConfig.symbol(id).id())
                    || !sheet.cell(row, 2).equals(type))
                throw new IllegalArgumentException("G002 PAYTABLE symbol identity/type mismatch");
            List<Double> pays = new ArrayList<>();
            for (int c = 3; c <= 5; c++) pays.add(decimal(sheet.cell(row, c)));
            if (id <= 6) payouts.add(pays);
            else if (pays.stream().anyMatch(v -> v != 0))
                throw new IllegalArgumentException("G002 special symbols do not self-pay");
        }
        if (!sheet.cell(start + 10, 0).isBlank())
            throw new IllegalArgumentException("Unexpected G002 symbol");
        start = title(sheet, "PAYLINES", 0);
        requireHeader(sheet, start + 1, List.of("Line", "R1", "R2", "R3", "R4", "R5"));
        List<List<Integer>> lines = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            if (integer(sheet.cell(start + 1 + i, 0)) != i)
                throw new IllegalArgumentException("G002 line IDs must be 1..10");
            List<Integer> rows = new ArrayList<>();
            for (int col = 1; col <= 5; col++) rows.add(integer(sheet.cell(start + 1 + i, col)));
            lines.add(rows);
        }
        if (!sheet.cell(start + 12, 0).isBlank())
            throw new IllegalArgumentException("Unexpected G002 payline");
        int denominator = integer(required(settings, "triggerDenominator"));
        int freeSpins = integer(required(settings, "freeSpins"));
        start = title(sheet, "CHANCETABLE:BG-setSelection", 0);
        requireHeader(sheet, start + 1, List.of("Value", "Weight"));
        int trigger = mode.equals("BB1") ? denominator : mode.equals("EM1") ? 4 : 1;
        if (integer(sheet.cell(start + 2, 0)) != 0 || integer(sheet.cell(start + 2, 1)) != denominator - trigger
                || integer(sheet.cell(start + 3, 0)) != 1 || integer(sheet.cell(start + 3, 1)) != trigger
                || !sheet.cell(start + 4, 0).isBlank())
            throw new IllegalArgumentException("G002 set weights must encode exact Normal/EM1/BB1 entry odds");
        start = title(sheet, "CHANCETABLE:FG-setSelection", 0);
        requireHeader(sheet, start + 1, List.of("Value", "Weight"));
        if (!sheet.cell(start + 2, 0).equals("0") || !sheet.cell(start + 2, 1).equals("1")
                || !sheet.cell(start + 3, 0).isBlank())
            throw new IllegalArgumentException("G002 has exactly FG set zero");
        start = title(sheet, "MAP:SCATTER-FREEGAMES", 0);
        requireHeader(sheet, start + 1, List.of("Scatters", "Freegames"));
        if (!sheet.cell(start + 2, 0).equals("3") || integer(sheet.cell(start + 2, 1)) != freeSpins
                || !sheet.cell(start + 3, 0).isBlank())
            throw new IllegalArgumentException("G002 scatter map must award its single eight-spin feature");
        return new EmberForgeConfig(mode, decimal(required(settings, "cost")), denominator,
                freeSpins, integer(required(settings, "maxHeat")), decimal(required(settings, "maxWin")),
                payouts, lines, reelSet(reels, "BG-0", 0), reelSet(reels, "BG-1", 6), reelSet(reels, "FG-0", 12));
    }
    private static List<List<Integer>> reelSet(ExcelWorkbookReader.Sheet sheet, String name, int column) {
        if (!sheet.cell(1, column).equals(name)) throw new IllegalArgumentException("Missing G002 reels " + name);
        List<List<Integer>> strips = new ArrayList<>();
        for (int reel = 0; reel < 5; reel++) {
            if (!sheet.cell(2, column + reel).equals(Integer.toString(reel + 1)))
                throw new IllegalArgumentException("Invalid reel header");
            List<Integer> ids = new ArrayList<>();
            int row = 3;
            while (!sheet.cell(row, column + reel).isBlank()) ids.add(integer(sheet.cell(row++, column + reel)));
            for (; row <= sheet.lastRow(); row++)
                if (!sheet.cell(row, column + reel).isBlank())
                    throw new IllegalArgumentException("G002 reel contains a gap");
            strips.add(ids);
        }
        return strips;
    }
    private static int title(ExcelWorkbookReader.Sheet sheet, String value, int col) {
        int found = 0;
        for (int row = 1; row <= sheet.lastRow(); row++)
            if (sheet.cell(row, col).equals(value)) {
                if (found != 0) throw new IllegalArgumentException("Duplicate G002 table " + value);
                found = row;
            }
        if (found == 0) throw new IllegalArgumentException("Missing G002 table " + value);
        return found;
    }
    private static void requireHeader(ExcelWorkbookReader.Sheet sheet, int row, List<String> names) {
        for (int col = 0; col < names.size(); col++)
            if (!sheet.cell(row, col).equals(names.get(col)))
                throw new IllegalArgumentException("Unexpected G002 table header at row " + row);
    }
    private static String required(Map<String, String> values, String key) {
        if (!values.containsKey(key)) throw new IllegalArgumentException("Missing G002 setting " + key);
        return values.get(key);
    }
    private static int integer(String text) {
        try { return new java.math.BigDecimal(text).intValueExact(); }
        catch (ArithmeticException | NumberFormatException e) { throw new IllegalArgumentException("Expected integer: " + text, e); }
    }
    private static double decimal(String text) {
        double value = Double.parseDouble(text);
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Expected finite number");
        return value;
    }
}
