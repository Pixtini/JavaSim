package game.walkingwild.config;

import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.probability.WeightedTable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import toolkit.par.ExcelWorkbookReader;
import toolkit.par.ParWorkbookModes;

/** Parses one isolated mode from the G001 Config/Reels PAR sheets. */
public final class WalkingWildParParser {
    private static final Pattern SCREEN = Pattern.compile("(?i)^(\\d+)x(\\d+)$");
    private static final Pattern REEL_SET = Pattern.compile("(?i)^(BG|FG)-(\\d+)$");

    public WalkingWildConfig parse(Path workbookPath, String requestedMode) throws IOException {
        var workbook = ExcelWorkbookReader.read(workbookPath);
        var modeSheets = ParWorkbookModes.select(workbook, requestedMode);
        var configSheet = workbook.requireSheet(modeSheets.configSheet());
        var reelsSheet = workbook.requireSheet(modeSheets.reelsSheet());

        Map<String, String> gameWide = readGameWide(configSheet);
        Matcher screen = SCREEN.matcher(required(gameWide, "screen"));
        if (!screen.matches()) throw invalid("Game-Wide-Config screen must use REELSxROWS format");
        int reels = Integer.parseInt(screen.group(1));
        int rows = Integer.parseInt(screen.group(2));
        if (reels != 6 || rows != 4) throw invalid("G001 PAR screen must be 6x4");

        Paytable paytable = readPaytable(configSheet);
        List<Payline> paylines = readPaylines(configSheet, reels, rows);
        Map<Integer, Integer> scatterAwards = readScatterMap(configSheet);
        Map<String, List<List<Integer>>> reelSets = readReels(reelsSheet, reels);
        List<WeightedTable.Entry<Integer>> baseSetWeights = readChanceTable(
                configSheet, findTitle(configSheet, "CHANCETABLE:BG-setSelection"));
        List<WeightedTable.Entry<Integer>> freeSetWeights = readChanceTable(
                configSheet, findTitle(configSheet, "CHANCETABLE:FG-setSelection"));

        List<WalkingWildConfig.BaseSet> baseSets = new ArrayList<>();
        for (int set = 0; set <= 1; set++) {
            String key = "BG-" + set;
            List<List<Integer>> strips = requireValue(reelSets, key, "reel set");
            List<WeightedTable.Entry<Integer>> counts = readChanceTable(configSheet,
                    findTitle(configSheet, "CHANCETABLE:" + key + "-scatter-insertQuantity"));
            WalkingWildConfig.HeatMap locations = readHeatMap(configSheet,
                    findTitle(configSheet, "HEATMAP:" + key + "-scatter-location"), reels, rows, 1);
            baseSets.add(new WalkingWildConfig.BaseSet(set, strips, counts, locations));
        }

        List<WalkingWildConfig.FreeSet> freeSets = new ArrayList<>();
        for (int set = 0; set <= 0; set++) {
            String key = "FG-" + set;
            List<List<Integer>> strips = requireValue(reelSets, key, "reel set");
            Map<Integer, List<WeightedTable.Entry<Integer>>> wildCounts = readTwoDimensionalChanceTable(
                    configSheet, findTitle(configSheet, "2DCHANCETABLE:" + key + "-wild-dependancyInsertQuantity"));
            WalkingWildConfig.HeatMap locations = readHeatMap(configSheet,
                    findTitle(configSheet, "HEATMAP:" + key + "-wild-location"), reels, rows,
                    Integer.MAX_VALUE);
            freeSets.add(new WalkingWildConfig.FreeSet(set, strips, wildCounts, locations));
        }

        return new WalkingWildConfig(modeSheets.mode(), reels, rows,
                decimal(required(gameWide, "maxwin"), "maxWin"),
                decimal(required(gameWide, "cost"), "cost"), paytable, paylines,
                scatterAwards, baseSetWeights, baseSets, freeSetWeights, freeSets);
    }

    private Map<String, String> readGameWide(ExcelWorkbookReader.Sheet sheet) {
        Cell title = findTitle(sheet, "Game-Wide-Config");
        Map<String, String> values = new HashMap<>();
        for (int row = title.row() + 1; row <= sheet.lastRow(); row++) {
            String key = sheet.cell(row, title.column()).trim();
            String value = sheet.cell(row, title.column() + 1).trim();
            if (key.isEmpty() && value.isEmpty()) break;
            if (!key.isEmpty() && !key.startsWith("MATH:")) {
                values.put(key.toLowerCase(Locale.ROOT).trim(), value);
            }
        }
        return values;
    }

    private Paytable readPaytable(ExcelWorkbookReader.Sheet sheet) {
        Cell title = findTitle(sheet, "PAYTABLE");
        int header = title.row() + 1;
        Map<String, Integer> columns = headers(sheet, header, title.column());
        int idColumn = required(columns, "symbol-id", "PAYTABLE header");
        int nameColumn = required(columns, "symbol-name", "PAYTABLE header");
        int typeColumn = required(columns, "type", "PAYTABLE header");
        Map<GameModuleFramework.symbols.Symbol, Map<Integer, Double>> awards = new LinkedHashMap<>();
        Set<Integer> seenIds = new java.util.HashSet<>();
        for (int row = header + 1; row <= sheet.lastRow(); row++) {
            String idText = sheet.cell(row, idColumn).trim();
            String name = sheet.cell(row, nameColumn).trim();
            if (idText.isEmpty() && name.isEmpty()) break;
            if (idText.startsWith("MATH:") || name.startsWith("MATH:")) continue;
            int id = integer(idText, "PAYTABLE symbol ID");
            if (!seenIds.add(id)) throw invalid("PAYTABLE repeats symbol ID " + id);
            var symbol = WalkingWildConfig.symbol(id);
            if (!symbol.id().equalsIgnoreCase(name)) {
                throw invalid("PAYTABLE ID " + id + " is " + symbol.id() + " but workbook names it " + name);
            }
            String type = sheet.cell(row, typeColumn).trim();
            if (type.equalsIgnoreCase("SIMPLE") || type.equalsIgnoreCase("WILD")) {
                Map<Integer, Double> symbolAwards = new LinkedHashMap<>();
                for (int count = 3; count <= 6; count++) {
                    Integer column = columns.get(count + "oak");
                    if (column == null) throw invalid("PAYTABLE is missing " + count + "oak");
                    double value = decimal(sheet.cell(row, column), count + "oak payout");
                    if (value < 0) throw invalid("PAYTABLE payouts cannot be negative");
                    symbolAwards.put(count, value);
                }
                awards.put(symbol, symbolAwards);
            } else if (!type.equalsIgnoreCase("FEATURE")) {
                throw invalid("Unsupported PAYTABLE type " + type);
            }
        }
        if (!awards.containsKey(WalkingWildConfig.WILD)) throw invalid("PAYTABLE must define Wild payouts");
        for (int id = 1; id <= 8; id++) {
            if (!awards.containsKey(WalkingWildConfig.symbol(id))) throw invalid("PAYTABLE missing symbol ID " + id);
        }
        return new Paytable(awards);
    }

    private List<Payline> readPaylines(ExcelWorkbookReader.Sheet sheet, int reels, int rows) {
        Cell title = findTitle(sheet, "PAYLINES");
        List<Payline> result = new ArrayList<>();
        for (int row = title.row() + 1; row <= sheet.lastRow(); row++) {
            int[] positions = new int[reels];
            boolean empty = true;
            for (int reel = 0; reel < reels; reel++) {
                String cell = sheet.cell(row, title.column() + reel).trim();
                if (cell.isEmpty()) {
                    if (!empty) throw invalid("PAYLINES row " + row + " is incomplete");
                    break;
                }
                empty = false;
                positions[reel] = integer(cell, "payline row");
                if (positions[reel] >= rows) throw invalid("PAYLINES row index is outside the 4-row screen");
            }
            if (empty) break;
            result.add(new Payline(positions));
        }
        if (result.isEmpty()) throw invalid("PAYLINES table is empty");
        return List.copyOf(result);
    }

    private Map<Integer, Integer> readScatterMap(ExcelWorkbookReader.Sheet sheet) {
        Cell title = findTitle(sheet, "MAP:SCATTER-FREEGAMES");
        int header = title.row() + 1;
        Map<String, Integer> columns = headers(sheet, header, title.column());
        int scatterCol = required(columns, "sym", "scatter map header");
        int freeGamesCol = required(columns, "f(sym)", "scatter map header");
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int row = header + 1; row <= sheet.lastRow(); row++) {
            String count = sheet.cell(row, scatterCol).trim();
            String award = sheet.cell(row, freeGamesCol).trim();
            if (count.isEmpty() && award.isEmpty()) break;
            int scatterCount = integer(count, "scatter count");
            int spins = integer(award, "freegame award");
            if (scatterCount <= 0 || spins <= 0 || result.put(scatterCount, spins) != null) {
                throw invalid("Scatter map contains invalid or duplicate entries");
            }
        }
        if (result.isEmpty()) throw invalid("PAR is missing scatter awards");
        return Map.copyOf(result);
    }

    private Map<String, List<List<Integer>>> readReels(ExcelWorkbookReader.Sheet sheet, int reelCount) {
        Map<String, List<List<Integer>>> result = new LinkedHashMap<>();
        for (var row : sheet.rows().entrySet()) {
            if (row.getKey() != 1) continue;
            for (var cell : row.getValue().entrySet()) {
                Matcher matcher = REEL_SET.matcher(cell.getValue().trim());
                if (!matcher.matches()) continue;
                String mode = matcher.group(1).toUpperCase(Locale.ROOT);
                int setId = Integer.parseInt(matcher.group(2));
                String key = mode + "-" + setId;
                List<List<Integer>> strips = new ArrayList<>();
                for (int reel = 0; reel < reelCount; reel++) {
                    int column = cell.getKey() + reel;
                    if (!sheet.cell(2, column).trim().equalsIgnoreCase("REEL " + (reel + 1))) {
                        throw invalid("Reels " + key + " must define sequential reel headers");
                    }
                    List<Integer> strip = new ArrayList<>();
                    for (int dataRow = 3; dataRow <= sheet.lastRow(); dataRow++) {
                        String value = sheet.cell(dataRow, column).trim();
                        if (value.isEmpty()) break;
                        int symbolId = integer(value, "reel symbol ID");
                        WalkingWildConfig.symbol(symbolId);
                        if (symbolId == 9) throw invalid("Scatter symbols are inserted by the PAR, not reel strips");
                        if (mode.equals("FG") && symbolId == 0) {
                            throw invalid("Freegame reel strips must not contain untracked wild symbols");
                        }
                        strip.add(symbolId);
                    }
                    if (strip.isEmpty()) throw invalid("Reels " + key + " contains an empty strip");
                    strips.add(List.copyOf(strip));
                }
                if (result.put(key, List.copyOf(strips)) != null) throw invalid("Duplicate reel set " + key);
            }
        }
        if (result.keySet().stream().noneMatch(key -> key.startsWith("BG-"))
                || result.keySet().stream().noneMatch(key -> key.startsWith("FG-"))) {
            throw invalid("Reels sheet must define basegame and freegame strips");
        }
        return result;
    }

    private List<WeightedTable.Entry<Integer>> readChanceTable(ExcelWorkbookReader.Sheet sheet, Cell title) {
        int header = title.row() + 1;
        Map<String, Integer> columns = headers(sheet, header, title.column());
        int valueCol = required(columns, "value", "chance table header");
        int weightCol = required(columns, "weight", "chance table header");
        List<WeightedTable.Entry<Integer>> entries = new ArrayList<>();
        for (int row = header + 1; row <= sheet.lastRow(); row++) {
            String value = sheet.cell(row, valueCol).trim();
            String weight = sheet.cell(row, weightCol).trim();
            if (value.isEmpty() && weight.isEmpty()) break;
            long parsedWeight = longInteger(weight, "chance table weight");
            int parsedValue = integer(value, "chance table value");
            if (parsedWeight < 0) throw invalid("Chance table weights cannot be negative");
            if (parsedWeight > 0) entries.add(new WeightedTable.Entry<>(parsedValue, parsedWeight));
        }
        if (entries.isEmpty()) throw invalid("Chance table at row " + title.row() + " has no positive weights");
        return List.copyOf(entries);
    }

    private Map<Integer, List<WeightedTable.Entry<Integer>>> readTwoDimensionalChanceTable(
            ExcelWorkbookReader.Sheet sheet, Cell title) {
        int header = title.row() + 1;
        int countColumn = title.column();
        Map<Integer, Integer> presentColumns = new LinkedHashMap<>();
        for (int column = countColumn + 1; column <= lastColumn(sheet, header); column++) {
            String value = sheet.cell(header, column).trim();
            if (value.isEmpty()) break;
            presentColumns.put(integer(value, "persisted wild table axis"), column);
        }
        Map<Integer, List<WeightedTable.Entry<Integer>>> result = new LinkedHashMap<>();
        Map<Integer, List<WeightedTable.Entry<Integer>>> byPresent = new LinkedHashMap<>();
        for (int row = header + 1; row <= sheet.lastRow(); row++) {
            String addedValue = sheet.cell(row, countColumn).trim();
            if (addedValue.isEmpty()) break;
            int added = integer(addedValue, "added wild count");
            for (var axis : presentColumns.entrySet()) {
                long weight = longInteger(sheet.cell(row, axis.getValue()), "2D chance table weight");
                if (weight < 0) throw invalid("2D chance table weights cannot be negative");
                if (weight > 0) byPresent.computeIfAbsent(axis.getKey(), ignored -> new ArrayList<>())
                        .add(new WeightedTable.Entry<>(added, weight));
            }
        }
        byPresent.forEach((present, entries) -> {
            int maxAdded = entries.stream().mapToInt(WeightedTable.Entry::value).max().orElse(0);
            if (present + maxAdded > 5) throw invalid("2D chance table can exceed five active wilds");
            result.put(present, List.copyOf(entries));
        });
        for (int present = 0; present <= 5; present++) {
            if (!result.containsKey(present)) throw invalid("2D chance table missing persisted-wild column " + present);
        }
        return Map.copyOf(result);
    }

    private WalkingWildConfig.HeatMap readHeatMap(ExcelWorkbookReader.Sheet sheet, Cell title,
            int reelCount, int visibleRows, int maxPerReel) {
        int header = title.row() + 1;
        Map<String, Integer> columns = headers(sheet, header, title.column());
        int gridColumn = required(columns, "grid", "heat map header");
        Map<Integer, Integer> reelColumns = new HashMap<>();
        for (int column = gridColumn + 1; column <= lastColumn(sheet, header); column++) {
            String value = sheet.cell(header, column).trim();
            if (value.isEmpty()) break;
            reelColumns.put(integer(value, "heat map reel index"), column);
        }
        List<List<Long>> weights = new ArrayList<>();
        for (int reel = 0; reel < reelCount; reel++) weights.add(new ArrayList<>(java.util.Collections.nCopies(visibleRows, 0L)));
        boolean[] foundRows = new boolean[visibleRows];
        for (int row = header + 1; row <= sheet.lastRow(); row++) {
            String rowLabel = sheet.cell(row, gridColumn).trim();
            if (rowLabel.isEmpty()) break;
            int visibleRow = integer(rowLabel, "heat map row index");
            if (visibleRow < 0 || visibleRow >= visibleRows) throw invalid("Heat map row is outside the screen");
            foundRows[visibleRow] = true;
            for (int reel = 0; reel < reelCount; reel++) {
                Integer column = reelColumns.get(reel);
                if (column == null) throw invalid("Heat map is missing reel " + reel);
                long weight = longInteger(sheet.cell(row, column), "heat map weight");
                if (weight < 0) throw invalid("Heat map weights cannot be negative");
                weights.get(reel).set(visibleRow, weight);
            }
        }
        for (int row = 0; row < visibleRows; row++) if (!foundRows[row]) throw invalid("Heat map missing row " + row);
        return new WalkingWildConfig.HeatMap(weights, maxPerReel);
    }

    private Map<String, Integer> headers(ExcelWorkbookReader.Sheet sheet, int row, int startColumn) {
        Map<String, Integer> result = new HashMap<>();
        for (int column = startColumn; column <= lastColumn(sheet, row); column++) {
            String value = sheet.cell(row, column).trim();
            if (value.isEmpty()) continue;
            result.put(value.toLowerCase(Locale.ROOT), column);
        }
        return result;
    }

    private Cell findTitle(ExcelWorkbookReader.Sheet sheet, String title) {
        for (var row : sheet.rows().entrySet()) {
            for (var cell : row.getValue().entrySet()) {
                if (cell.getValue().trim().toLowerCase(Locale.ROOT).startsWith(title.toLowerCase(Locale.ROOT))) {
                    return new Cell(row.getKey(), cell.getKey());
                }
            }
        }
        throw invalid("PAR Config is missing " + title);
    }

    private int lastColumn(ExcelWorkbookReader.Sheet sheet, int row) {
        return sheet.rows().getOrDefault(row, Map.of()).keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private static int required(Map<String, Integer> values, String key, String label) {
        Integer value = values.get(key.toLowerCase(Locale.ROOT));
        if (value == null) throw invalid(label + " is missing " + key);
        return value;
    }

    private static <T> T requireValue(Map<String, T> values, String key, String label) {
        T value = values.get(key);
        if (value == null) throw invalid("PAR is missing " + label + " " + key);
        return value;
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw invalid("Game-Wide-Config is missing " + key);
        return value.trim();
    }

    private static int integer(String value, String label) {
        try { return Integer.parseInt(value.trim()); }
        catch (RuntimeException e) { throw invalid("Invalid " + label + ": " + value); }
    }

    private static long longInteger(String value, String label) {
        try { return Long.parseLong(value.trim()); }
        catch (RuntimeException e) { throw invalid("Invalid " + label + ": " + value); }
    }

    private static double decimal(String value, String label) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed)) throw invalid(label + " must be finite");
            return parsed;
        } catch (NumberFormatException e) { throw invalid("Invalid " + label + ": " + value); }
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private record Cell(int row, int column) {}
}
