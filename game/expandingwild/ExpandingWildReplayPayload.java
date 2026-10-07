package game.expandingwild;

import GameModuleFramework.features.SymbolInsertion;
import GameModuleFramework.symbols.Symbol;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Versioned, game-owned replay data for one expanding-wild spin. */
public record ExpandingWildReplayPayload(int setIndex, int[] reelStops,
        Map<Integer, Integer> bannerMultipliers, List<SymbolInsertion.Placement> insertions) {
    private static final Pattern JSON = Pattern.compile(
            "\\{\\\"version\\\":([12]),\\\"setIndex\\\":(-?\\d+),"
                    + "\\\"reelStops\\\":\\[([^]]*)\\],"
                    + "\\\"bannerMultipliers\\\":\\{([^}]*)}"
                    + "(?:,\\\"insertions\\\":\\[([^]]*)])?}");
    private static final Pattern INSERTION = Pattern.compile(
            "\\{\\\"symbol\\\":\\\"([^\\\"]+)\\\","
                    + "\\\"reel\\\":(\\d+),\\\"row\\\":(\\d+)}");

    public ExpandingWildReplayPayload(int setIndex, int[] reelStops,
            Map<Integer, Integer> bannerMultipliers) {
        this(setIndex, reelStops, bannerMultipliers, List.of());
    }

    public ExpandingWildReplayPayload {
        reelStops = reelStops.clone();
        bannerMultipliers = Map.copyOf(bannerMultipliers);
        insertions = List.copyOf(insertions);
    }

    @Override
    public int[] reelStops() {
        return reelStops.clone();
    }

    public String toJson() {
        String stops = Arrays.stream(reelStops).mapToObj(Integer::toString)
                .collect(java.util.stream.Collectors.joining(","));
        String multipliers = new TreeMap<>(bannerMultipliers).entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(","));
        String insertedSymbols = insertions.stream()
                .map(placement -> "{\"symbol\":\"" + placement.symbol().id()
                        + "\",\"reel\":" + placement.reel()
                        + ",\"row\":" + placement.row() + "}")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"version\":2,\"setIndex\":" + setIndex
                + ",\"reelStops\":[" + stops + "],\"bannerMultipliers\":{" + multipliers
                + "},\"insertions\":[" + insertedSymbols + "]}";
    }

    public static ExpandingWildReplayPayload fromJson(String json) {
        Matcher matcher = JSON.matcher(json);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid expanding-wild replay payload");
        }
        int setIndex = Integer.parseInt(matcher.group(2));
        int[] reelStops = parseIntegers(matcher.group(3));
        Map<Integer, Integer> multipliers = parseMultipliers(matcher.group(4));
        List<SymbolInsertion.Placement> insertions = matcher.group(5) == null
                ? List.of() : parseInsertions(matcher.group(5));
        return new ExpandingWildReplayPayload(setIndex, reelStops, multipliers, insertions);
    }

    private static int[] parseIntegers(String values) {
        if (values.isBlank()) {
            return new int[0];
        }
        return Arrays.stream(values.split(","))
                .map(String::trim).mapToInt(Integer::parseInt).toArray();
    }

    private static Map<Integer, Integer> parseMultipliers(String values) {
        Map<Integer, Integer> multipliers = new LinkedHashMap<>();
        if (values.isBlank()) {
            return multipliers;
        }
        for (String entry : values.split(",")) {
            String[] keyValue = entry.split(":", 2);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Invalid recorded banner multiplier");
            }
            String key = keyValue[0].trim().replace("\"", "");
            multipliers.put(Integer.parseInt(key), Integer.parseInt(keyValue[1].trim()));
        }
        return multipliers;
    }

    private static List<SymbolInsertion.Placement> parseInsertions(String values) {
        List<SymbolInsertion.Placement> placements = new java.util.ArrayList<>();
        Matcher matcher = INSERTION.matcher(values);
        int previousEnd = 0;
        while (matcher.find()) {
            if (!values.substring(previousEnd, matcher.start()).replace(",", "").isBlank()) {
                throw new IllegalArgumentException("Invalid recorded symbol insertion");
            }
            placements.add(new SymbolInsertion.Placement(new Symbol(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))));
            previousEnd = matcher.end();
        }
        if (!values.substring(previousEnd).replace(",", "").isBlank()) {
            throw new IllegalArgumentException("Invalid recorded symbol insertion");
        }
        return List.copyOf(placements);
    }
}
