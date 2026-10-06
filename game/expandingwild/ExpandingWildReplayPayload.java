package game.expandingwild;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Versioned, game-owned replay data for one expanding-wild spin. */
public record ExpandingWildReplayPayload(
        int setIndex, int[] reelStops, Map<Integer, Integer> bannerMultipliers) {
    private static final Pattern JSON = Pattern.compile(
            "\\{\\\"version\\\":1,\\\"setIndex\\\":(-?\\d+),"
                    + "\\\"reelStops\\\":\\[([^]]*)\\],"
                    + "\\\"bannerMultipliers\\\":\\{([^}]*)}\\}");

    public ExpandingWildReplayPayload {
        reelStops = reelStops.clone();
        bannerMultipliers = Map.copyOf(bannerMultipliers);
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
        return "{\"version\":1,\"setIndex\":" + setIndex
                + ",\"reelStops\":[" + stops + "],\"bannerMultipliers\":{" + multipliers + "}}";
    }

    public static ExpandingWildReplayPayload fromJson(String json) {
        Matcher matcher = JSON.matcher(json);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid expanding-wild replay payload");
        }
        int setIndex = Integer.parseInt(matcher.group(1));
        int[] reelStops = parseIntegers(matcher.group(2));
        Map<Integer, Integer> multipliers = parseMultipliers(matcher.group(3));
        return new ExpandingWildReplayPayload(setIndex, reelStops, multipliers);
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
}
