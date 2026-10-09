package game.expandingwild.config;

import GameModuleFramework.features.SymbolInsertion;
import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.symbols.Symbol;
import java.io.IOException;
import java.math.BigDecimal;
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

/** Maps the common Config/Reels PAR workbook layout into Expanding Wild game configuration. */
public final class ExpandingWildParParser {
    private static final Pattern MODE_SET = Pattern.compile("(?i)^(BG|FG)-(\\d+)$");
    private static final Pattern TABLE_MODE = Pattern.compile("(?i)^(BG|FG)(?:-(\\d+))?-(.+)$");
    private static final Pattern SCREEN = Pattern.compile("(?i)^(\\d+)x(\\d+)$");
    private static final Pattern REEL_MAX = Pattern.compile("(?i)reelMax\\s*=\\s*(\\d+)");
    private static final Set<Symbol> INSERTION_PROTECTED = Set.of(
            ExpandingWildConfig.SCATTER, ExpandingWildConfig.BANNER, ExpandingWildConfig.WILD);

    public ExpandingWildConfig parse(Path workbookPath) throws IOException {
        var workbook = ExcelWorkbookReader.read(workbookPath);
        var configSheet = workbook.requireSheet("Config");
        var reelsSheet = workbook.requireSheet("Reels");
        ExpandingWildConfig config = new ExpandingWildConfig();

        Map<String, String> gameWide = readGameWideValues(configSheet);
        Matcher screen = SCREEN.matcher(required(gameWide, "screen", "Game-Wide-Config"));
        if (!screen.matches()) {
            throw new IllegalArgumentException("PAR Config screen must use REELSxROWS format, for example 5x5");
        }
        config.reelCount = Integer.parseInt(screen.group(1));
        config.visibleRows = Integer.parseInt(screen.group(2));
        config.maxWinMultiplier = decimal(required(gameWide, "maxWin", "Game-Wide-Config"), "maxWin");

        config.paytable = readPaytable(configSheet);
        config.paylines = readPaylines(configSheet);
        readScatterAwardMap(configSheet, config);

        Map<Mode, Map<Integer, List<List<Integer>>>> reelSets = readReels(reelsSheet, config.reelCount);
        Map<String, ChanceTable> chanceTables = new HashMap<>();
        Map<String, HeatMap> heatMaps = new HashMap<>();
        Map<Mode, List<WeightedTable.Entry<Integer>>> selections = new HashMap<>();
        readFeatureTables(configSheet, config.reelCount, config.visibleRows,
                chanceTables, heatMaps, selections);

        config.baseGame = buildModeConfig(Mode.BG, reelSets, selections, chanceTables, heatMaps,
                config.reelCount, config.visibleRows);
        config.freeGame = buildModeConfig(Mode.FG, reelSets, selections, chanceTables, heatMaps,
                config.reelCount, config.visibleRows);
        validateFreegameScatterRules(config.freeGame);
        config.scatterReels = eligibleScatterReels(config.baseGame, config.reelCount)
                .stream().mapToInt(Integer::intValue).toArray();
        return config;
    }

    private Map<String, String> readGameWideValues(ExcelWorkbookReader.Sheet sheet) {
        Cell title = findCell(sheet, "Game-Wide-Config");
        if (title == null) {
            throw new IllegalArgumentException("PAR Config sheet is missing Game-Wide-Config");
        }
        Map<String, String> values = new HashMap<>();
        for (int row = title.row() + 1; row <= sheet.lastRow(); row++) {
            String key = sheet.cell(row, title.column()).trim();
            String value = sheet.cell(row, title.column() + 1).trim();
            if (key.isEmpty() && value.isEmpty()) {
                break;
            }
            if (key.startsWith("MATH:")) {
                continue;
            }
            if (!key.isEmpty()) {
                values.put(key.toLowerCase(Locale.ROOT), value);
            }
        }
        return values;
    }

    private Paytable readPaytable(ExcelWorkbookReader.Sheet sheet) {
        Cell title = findCell(sheet, "PAYTABLE");
        if (title == null) {
            throw new IllegalArgumentException("PAR Config sheet is missing PAYTABLE");
        }
        int headerRow = title.row() + 1;
        int idColumn = findHeader(sheet, headerRow, title.column(), "Symbol-ID");
        int nameColumn = findHeader(sheet, headerRow, title.column(), "Symbol-Name");
        int fiveColumn = findHeader(sheet, headerRow, title.column(), "5oak");
        int fourColumn = findHeader(sheet, headerRow, title.column(), "4oak");
        int threeColumn = findHeader(sheet, headerRow, title.column(), "3oak");
        int typeColumn = findHeader(sheet, headerRow, title.column(), "TYPE");
        Map<Symbol, Map<Integer, Double>> awards = new LinkedHashMap<>();
        Set<Integer> seenSymbolIds = new java.util.HashSet<>();
        for (int row = headerRow + 1; row <= sheet.lastRow(); row++) {
            String id = sheet.cell(row, idColumn).trim();
            String name = sheet.cell(row, nameColumn).trim();
            if (id.isEmpty() && name.isEmpty()) {
                break;
            }
            if ("MATH:".equalsIgnoreCase(id) || "MATH:".equalsIgnoreCase(name)) {
                continue;
            }
            int symbolId = integer(id, "paytable symbol ID");
            if (!seenSymbolIds.add(symbolId)) {
                throw new IllegalArgumentException("PAR PAYTABLE contains duplicate symbol ID " + symbolId);
            }
            Symbol symbol = ExpandingWildConfig.fromId(symbolId);
            if (!symbol.id().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("PAR PAYTABLE symbol ID " + symbolId + " maps to "
                        + symbol.id() + " in the game but the workbook names it " + name);
            }
            String symbolType = sheet.cell(row, typeColumn).trim();
            if (!symbolType.equalsIgnoreCase("SIMPLE")
                    && !symbolType.equalsIgnoreCase("WILD")
                    && !symbolType.equalsIgnoreCase("FEATURE")) {
                throw new IllegalArgumentException("Unsupported PAR PAYTABLE type '" + symbolType
                        + "' at Config row " + row);
            }
            double three = decimal(sheet.cell(row, threeColumn), "3oak payout");
            double four = decimal(sheet.cell(row, fourColumn), "4oak payout");
            double five = decimal(sheet.cell(row, fiveColumn), "5oak payout");
            if (!symbolType.equalsIgnoreCase("SIMPLE")) {
                continue;
            }
            awards.put(symbol, Map.of(
                    3, three,
                    4, four,
                    5, five));
        }
        if (awards.isEmpty()) {
            throw new IllegalArgumentException("PAR PAYTABLE must define at least one SIMPLE symbol");
        }
        return new Paytable(awards);
    }

    private List<Payline> readPaylines(ExcelWorkbookReader.Sheet sheet) {
        Cell title = findCell(sheet, "PAYLINES");
        if (title == null) {
            throw new IllegalArgumentException("PAR Config sheet is missing PAYLINES");
        }
        List<Payline> paylines = new ArrayList<>();
        for (int row = title.row() + 1; row <= sheet.lastRow(); row++) {
            List<Integer> path = new ArrayList<>();
            boolean any = false;
            for (int reel = 0; reel < 5; reel++) {
                String value = sheet.cell(row, title.column() + reel).trim();
                any |= !value.isEmpty();
                if (!value.isEmpty()) {
                    path.add(integer(value, "payline row"));
                }
            }
            if (!any) {
                break;
            }
            if (path.size() != 5) {
                throw new IllegalArgumentException("PAR payline at Config row " + row
                        + " must define one row for each of five reels");
            }
            paylines.add(new Payline(path.stream().mapToInt(Integer::intValue).toArray()));
        }
        if (paylines.isEmpty()) {
            throw new IllegalArgumentException("PAR PAYLINES table is empty");
        }
        return List.copyOf(paylines);
    }

    private void readScatterAwardMap(ExcelWorkbookReader.Sheet sheet, ExpandingWildConfig config) {
        Cell title = findCellWithPrefix(sheet, "MAP:SCATTER-FREEGAMES");
        if (title == null) {
            throw new IllegalArgumentException("PAR Config sheet is missing MAP:SCATTER-FREEGAMES");
        }
        int headerRow = title.row() + 1;
        int scatterColumn = findHeader(sheet, headerRow, title.column(), "sym");
        int freegamesColumn = findHeader(sheet, headerRow, title.column(), "f(sym)");
        List<Integer> thresholds = new ArrayList<>();
        List<Integer> awards = new ArrayList<>();
        for (int row = headerRow + 1; row <= sheet.lastRow(); row++) {
            String scatterCount = sheet.cell(row, scatterColumn).trim();
            String freegameCount = sheet.cell(row, freegamesColumn).trim();
            if (scatterCount.isEmpty() && freegameCount.isEmpty()) {
                break;
            }
            thresholds.add(integer(scatterCount, "scatter trigger threshold"));
            awards.add(integer(freegameCount, "scatter freegame award"));
        }
        if (thresholds.size() != 1) {
            throw new IllegalArgumentException("Expanding Wild currently supports one SCATTER-FREEGAMES mapping; PAR defines "
                    + thresholds.size());
        }
        config.freeGameTriggerScatterCount = thresholds.get(0);
        config.freeGamesAwarded = awards.get(0);
    }

    private Map<Mode, Map<Integer, List<List<Integer>>>> readReels(
            ExcelWorkbookReader.Sheet sheet, int reelCount) {
        Map<Mode, Map<Integer, List<List<Integer>>>> result = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, String>> row : sheet.rows().entrySet()) {
            for (Map.Entry<Integer, String> cell : row.getValue().entrySet()) {
                Matcher group = MODE_SET.matcher(cell.getValue().trim());
                if (!group.matches() || row.getKey() != 1) {
                    continue;
                }
                Mode mode = Mode.fromCode(group.group(1));
                int setIndex = Integer.parseInt(group.group(2));
                List<List<Integer>> reels = new ArrayList<>(reelCount);
                for (int reel = 0; reel < reelCount; reel++) {
                    int column = cell.getKey() + reel;
                    String header = sheet.cell(2, column).trim();
                    if (!header.equalsIgnoreCase("REEL " + (reel + 1))) {
                        throw new IllegalArgumentException("PAR Reels " + cell.getValue()
                                + " must have consecutive reel headers starting at " + (reel + 1));
                    }
                    List<Integer> strip = new ArrayList<>();
                    for (int dataRow = 3; dataRow <= sheet.lastRow(); dataRow++) {
                        String value = sheet.cell(dataRow, column).trim();
                        if (value.isEmpty()) {
                            break;
                        }
                        int symbolId = integer(value, "reel symbol ID");
                        ExpandingWildConfig.fromId(symbolId);
                        strip.add(symbolId);
                    }
                    if (strip.isEmpty()) {
                        throw new IllegalArgumentException("PAR Reels " + cell.getValue()
                                + " reel " + (reel + 1) + " is empty");
                    }
                    reels.add(List.copyOf(strip));
                }
                Map<Integer, List<List<Integer>>> modeSets = result.computeIfAbsent(mode,
                        ignored -> new LinkedHashMap<>());
                if (modeSets.put(setIndex, List.copyOf(reels)) != null) {
                    throw new IllegalArgumentException("PAR Reels defines " + cell.getValue() + " more than once");
                }
            }
        }
        for (Mode mode : Mode.values()) {
            Map<Integer, List<List<Integer>>> sets = result.get(mode);
            if (sets == null || sets.size() != 2 || !sets.containsKey(0) || !sets.containsKey(1)) {
                throw new IllegalArgumentException("PAR Reels must define set 0 and set 1 for " + mode.label);
            }
        }
        return result;
    }

    private void readFeatureTables(ExcelWorkbookReader.Sheet sheet, int reelCount,
            int visibleRows, Map<String, ChanceTable> chanceTables,
            Map<String, HeatMap> heatMaps,
            Map<Mode, List<WeightedTable.Entry<Integer>>> selections) {
        for (Map.Entry<Integer, Map<Integer, String>> row : sheet.rows().entrySet()) {
            for (Map.Entry<Integer, String> cell : row.getValue().entrySet()) {
                String title = cell.getValue().trim();
                if (title.startsWith("CHANCETABLE:")) {
                    TableName name = TableName.parse(title.substring("CHANCETABLE:".length()));
                    if (name.setSelection()) {
                        selections.put(name.mode(),
                                readChanceTable(sheet, row.getKey(), cell.getKey()).entries());
                    } else if (name.setIndex() != null) {
                        String key = key(name.mode(), name.setIndex(), name.feature());
                        ChanceTable table = readChanceTable(sheet, row.getKey(), cell.getKey());
                        if (name.feature().equalsIgnoreCase("wildExpand-insertQuantity")) {
                            putUnique(chanceTables, key, table, "chance table " + title);
                        } else if (name.feature().equalsIgnoreCase("scatter-insertQuantity")) {
                            putUnique(chanceTables, key, table, "chance table " + title);
                        } else if (name.feature().equalsIgnoreCase("wildExpand-multiplier")) {
                            putUnique(chanceTables, key, table, "chance table " + title);
                        } else {
                            throw new IllegalArgumentException("Unsupported PAR CHANCETABLE: " + title);
                        }
                    }
                } else if (title.startsWith("HEATMAP:")) {
                    HeatMapName name = HeatMapName.parse(title.substring("HEATMAP:".length()));
                    String key = key(name.mode(), name.setIndex(), name.feature());
                    putUnique(heatMaps, key, readHeatMap(sheet, row.getKey(), cell.getKey(),
                            reelCount, visibleRows, title), "heat map " + title);
                }
            }
        }
        for (Mode mode : Mode.values()) {
            if (selections.getOrDefault(mode, List.of()).isEmpty()) {
                throw new IllegalArgumentException("PAR Config is missing " + mode.label + " setSelection CHANCETABLE");
            }
        }
    }

    private ExpandingWildConfig.SpinModeConfig buildModeConfig(Mode mode,
            Map<Mode, Map<Integer, List<List<Integer>>>> reelSets,
            Map<Mode, List<WeightedTable.Entry<Integer>>> selections,
            Map<String, ChanceTable> chanceTables, Map<String, HeatMap> heatMaps,
            int reelCount, int visibleRows) {
        List<ExpandingWildConfig.SpinSetConfig> sets = new ArrayList<>(2);
        for (int setIndex = 0; setIndex < 2; setIndex++) {
            List<SymbolInsertion.Rule> insertions = new ArrayList<>();
            List<WeightedTable.Entry<Integer>> bannerMultipliers = List.of();
            for (String feature : List.of("wildExpand-insertQuantity", "scatter-insertQuantity")) {
                String tableKey = key(mode, setIndex, feature);
                ChanceTable counts = chanceTables.get(tableKey);
                HeatMap heat = heatMaps.get(key(mode, setIndex,
                        feature.startsWith("wildExpand") ? "wildExpand-location" : "scatter-location"));
                if ((counts == null) != (heat == null)) {
                    throw new IllegalArgumentException("PAR needs both count and location tables for "
                            + mode.label + " set " + setIndex + " " + feature);
                }
                if (counts != null) {
                    Symbol symbol = feature.startsWith("wildExpand")
                            ? ExpandingWildConfig.BANNER : ExpandingWildConfig.SCATTER;
                    insertions.add(new SymbolInsertion.Rule(symbol, counts.entries(),
                            heat.weights(), heat.reelMax(), INSERTION_PROTECTED));
                }
            }
            ChanceTable multipliers = chanceTables.get(key(mode, setIndex, "wildExpand-multiplier"));
            if (multipliers != null) {
                bannerMultipliers = multipliers.entries();
            }
            List<List<Integer>> reels = reelSets.get(mode).get(setIndex);
            if (reels.size() != reelCount || reels.stream().anyMatch(strip -> strip.isEmpty())) {
                throw new IllegalArgumentException("PAR reel count does not match Game-Wide-Config");
            }
            for (List<Integer> strip : reels) {
                if (strip.stream().anyMatch(id -> !isSimpleSymbol(id))) {
                    throw new IllegalArgumentException("PAR Reels may contain only SIMPLE symbol IDs; "
                            + "special symbols are inserted by feature tables");
                }
            }
            sets.add(new ExpandingWildConfig.SpinSetConfig(
                    reels, bannerMultipliers, List.copyOf(insertions)));
        }
        return new ExpandingWildConfig.SpinModeConfig(List.copyOf(sets), selections.get(mode));
    }

    private void validateFreegameScatterRules(ExpandingWildConfig.SpinModeConfig mode) {
        for (int setIndex = 0; setIndex < mode.sets.size(); setIndex++) {
            for (SymbolInsertion.Rule rule : mode.sets.get(setIndex).symbolInsertions) {
                if (rule.symbol().equals(ExpandingWildConfig.SCATTER)
                        && rule.countWeights().stream().anyMatch(entry -> entry.value() > 0 && entry.weight() > 0)) {
                    throw new IllegalArgumentException("PAR enables scatter insertion in freegame set " + setIndex
                            + ", but freegame retriggers are not implemented; disable its non-zero count weights "
                            + "or confirm that freegame scatters should award additional freegames");
                }
            }
        }
    }

    private List<Integer> eligibleScatterReels(ExpandingWildConfig.SpinModeConfig mode, int reelCount) {
        boolean[] eligible = new boolean[reelCount];
        for (var set : mode.sets) {
            for (var rule : set.symbolInsertions) {
                if (rule.symbol().equals(ExpandingWildConfig.SCATTER)) {
                    for (int reel = 0; reel < reelCount; reel++) {
                        eligible[reel] |= rule.positionWeights().get(reel).stream().anyMatch(weight -> weight > 0);
                    }
                }
            }
        }
        List<Integer> result = new ArrayList<>();
        for (int reel = 0; reel < eligible.length; reel++) {
            if (eligible[reel]) {
                result.add(reel);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("PAR must allow scatter placement on at least one basegame reel");
        }
        return List.copyOf(result);
    }

    private ChanceTable readChanceTable(ExcelWorkbookReader.Sheet sheet,
            int titleRow, int titleColumn) {
        int headerRow = titleRow + 1;
        int valueColumn = findHeader(sheet, headerRow, titleColumn, "value");
        int weightColumn = findHeader(sheet, headerRow, titleColumn, "weight");
        List<WeightedTable.Entry<Integer>> entries = new ArrayList<>();
        for (int row = headerRow + 1; row <= sheet.lastRow(); row++) {
            String value = sheet.cell(row, valueColumn).trim();
            String weight = sheet.cell(row, weightColumn).trim();
            if (value.isEmpty() && weight.isEmpty()) {
                break;
            }
            int tableValue = integer(value, "chance table value");
            long tableWeight = longInteger(weight, "chance table weight");
            if (tableWeight < 0) {
                throw new IllegalArgumentException("PAR chance table weights cannot be negative");
            }
            if (tableWeight > 0) {
                entries.add(new WeightedTable.Entry<>(tableValue, tableWeight));
            }
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("PAR chance table at Config row " + titleRow + " is empty");
        }
        return new ChanceTable(List.copyOf(entries));
    }

    private HeatMap readHeatMap(ExcelWorkbookReader.Sheet sheet,
            int titleRow, int titleColumn, int reelCount, int visibleRows, String title) {
        Matcher limitMatcher = REEL_MAX.matcher(title);
        if (!limitMatcher.find()) {
            throw new IllegalArgumentException("PAR heat map must declare {reelMax = N}: " + title);
        }
        int maxPerReel = Integer.parseInt(limitMatcher.group(1));
        int headerRow = titleRow + 1;
        int gridColumn = findHeader(sheet, headerRow, titleColumn, "grid");
        Map<Integer, Integer> reelColumns = new HashMap<>();
        for (int column = gridColumn + 1; column <= lastColumn(sheet, headerRow); column++) {
            String header = sheet.cell(headerRow, column).trim();
            if (header.isEmpty()) {
                break;
            }
            int reel = integer(header, "heat map reel index");
            if (reel < 0 || reel >= reelCount) {
                throw new IllegalArgumentException("PAR heat map reel index is outside the configured grid: " + reel);
            }
            reelColumns.put(reel, column);
        }
        if (reelColumns.size() != reelCount) {
            throw new IllegalArgumentException("PAR heat map must define all " + reelCount + " reel columns: " + title);
        }
        List<List<Long>> matrix = new ArrayList<>(reelCount);
        for (int reel = 0; reel < reelCount; reel++) {
            matrix.add(new ArrayList<>(java.util.Collections.nCopies(visibleRows, 0L)));
        }
        boolean[] foundRows = new boolean[visibleRows];
        for (int row = headerRow + 1; row <= sheet.lastRow(); row++) {
            String rowLabel = sheet.cell(row, gridColumn).trim();
            if (rowLabel.isEmpty()) {
                break;
            }
            int visibleRow = integer(rowLabel, "heat map row index");
            if (visibleRow < 0 || visibleRow >= visibleRows) {
                throw new IllegalArgumentException("PAR heat map row index is outside the configured grid: " + visibleRow);
            }
            foundRows[visibleRow] = true;
            for (int reel = 0; reel < reelCount; reel++) {
                long weight = longInteger(sheet.cell(row, reelColumns.get(reel)), "heat map weight");
                matrix.get(reel).set(visibleRow, weight);
            }
        }
        for (int row = 0; row < visibleRows; row++) {
            if (!foundRows[row]) {
                throw new IllegalArgumentException("PAR heat map omits visible row " + row + ": " + title);
            }
        }
        return new HeatMap(matrix.stream().map(List::copyOf).toList(), maxPerReel);
    }

    private int findHeader(ExcelWorkbookReader.Sheet sheet, int row, int firstColumn, String header) {
        for (int column = firstColumn; column <= lastColumn(sheet, row); column++) {
            if (sheet.cell(row, column).trim().equalsIgnoreCase(header)) {
                return column;
            }
        }
        throw new IllegalArgumentException("PAR table is missing required header '" + header
                + "' on Config row " + row);
    }

    private int lastColumn(ExcelWorkbookReader.Sheet sheet, int row) {
        return sheet.rows().getOrDefault(row, Map.of()).keySet().stream()
                .mapToInt(Integer::intValue).max().orElse(0);
    }

    private Cell findCell(ExcelWorkbookReader.Sheet sheet, String text) {
        for (var row : sheet.rows().entrySet()) {
            for (var cell : row.getValue().entrySet()) {
                if (cell.getValue().trim().equalsIgnoreCase(text)) {
                    return new Cell(row.getKey(), cell.getKey());
                }
            }
        }
        return null;
    }

    private Cell findCellWithPrefix(ExcelWorkbookReader.Sheet sheet, String prefix) {
        for (var row : sheet.rows().entrySet()) {
            for (var cell : row.getValue().entrySet()) {
                if (cell.getValue().trim().equalsIgnoreCase(prefix)) {
                    return new Cell(row.getKey(), cell.getKey());
                }
            }
        }
        return null;
    }

    private void putUnique(Map<String, ChanceTable> target, String key, ChanceTable value, String label) {
        if (target.putIfAbsent(key, value) != null) {
            throw new IllegalArgumentException("PAR defines duplicate " + label);
        }
    }

    private void putUnique(Map<String, HeatMap> target, String key, HeatMap value, String label) {
        if (target.putIfAbsent(key, value) != null) {
            throw new IllegalArgumentException("PAR defines duplicate " + label);
        }
    }

    private String required(Map<String, String> values, String key, String table) {
        String value = values.get(key.toLowerCase(Locale.ROOT));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PAR " + table + " is missing " + key);
        }
        return value;
    }

    private int integer(String value, String label) {
        try {
            return new BigDecimal(value.trim()).intValueExact();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("PAR " + label + " must be a whole number: '" + value + "'");
        }
    }

    private long longInteger(String value, String label) {
        try {
            return new BigDecimal(value.trim()).longValueExact();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("PAR " + label + " must be a whole number: '" + value + "'");
        }
    }

    private double decimal(String value, String label) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed) || parsed < 0.0) {
                throw new NumberFormatException("not finite or negative");
            }
            return parsed;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("PAR " + label + " must be a finite non-negative number: '" + value + "'");
        }
    }

    private boolean isSimpleSymbol(int id) {
        return id >= 0 && id <= 9;
    }

    private String key(Mode mode, int setIndex, String feature) {
        return mode.code + ":" + setIndex + ":" + feature.toLowerCase(Locale.ROOT);
    }

    private enum Mode {
        BG("BG", "Basegame"), FG("FG", "Freegame");
        private final String code;
        private final String label;
        Mode(String code, String label) { this.code = code; this.label = label; }
        private static Mode fromCode(String code) {
            return valueOf(code.toUpperCase(Locale.ROOT));
        }
    }

    private record Cell(int row, int column) {}
    private record ChanceTable(List<WeightedTable.Entry<Integer>> entries) {}
    private record HeatMap(List<List<Long>> weights, int reelMax) {}

    private record TableName(Mode mode, Integer setIndex, String feature) {
        private static TableName parse(String name) {
            if (name.matches("(?i)(BG|FG)-setSelection")) {
                return new TableName(Mode.fromCode(name.substring(0, 2)), null, "setSelection");
            }
            Matcher matcher = TABLE_MODE.matcher(name);
            if (!matcher.matches() || matcher.group(2) == null) {
                throw new IllegalArgumentException("Invalid PAR chance table name: " + name);
            }
            return new TableName(Mode.fromCode(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), matcher.group(3));
        }
        private boolean setSelection() { return setIndex == null; }
    }

    private record HeatMapName(Mode mode, int setIndex, String feature) {
        private static HeatMapName parse(String name) {
            Matcher matcher = TABLE_MODE.matcher(name);
            if (!matcher.matches() || matcher.group(2) == null) {
                throw new IllegalArgumentException("Invalid PAR heat map name: " + name);
            }
            String remainder = matcher.group(3);
            int limitsStart = remainder.indexOf('{');
            String feature = (limitsStart < 0 ? remainder : remainder.substring(0, limitsStart))
                    .trim().toLowerCase(Locale.ROOT);
            return new HeatMapName(Mode.fromCode(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), feature);
        }
    }
}
