package game.walkingwild.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Records deterministic reel stops and placements needed to reconstruct one G001 event. */
public record WalkingWildReplayPayload(int setIndex, int scatterCount, int[] reelStops,
        List<WildPosition> scatterInsertions, List<WildPosition> newWildPositions,
        List<WildPosition> movedWildPositions) {
    public WalkingWildReplayPayload {
        reelStops = reelStops.clone();
        scatterInsertions = List.copyOf(scatterInsertions);
        newWildPositions = List.copyOf(newWildPositions);
        movedWildPositions = List.copyOf(movedWildPositions);
    }

    @Override public int[] reelStops() { return reelStops.clone(); }

    public String toPayload() {
        return "v2|" + setIndex + "|" + scatterCount + "|"
                + Arrays.stream(reelStops).mapToObj(Integer::toString)
                        .collect(Collectors.joining(","))
                + "|" + positions(scatterInsertions)
                + "|" + positions(newWildPositions)
                + "|" + positions(movedWildPositions);
    }

    public static WalkingWildReplayPayload parse(String payload) {
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 7 || !parts[0].equals("v2")) {
            throw new IllegalArgumentException("Invalid G001 replay payload");
        }
        int[] stops = Arrays.stream(parts[3].split(",", -1))
                .map(String::trim).mapToInt(Integer::parseInt).toArray();
        if (stops.length != 6) throw new IllegalArgumentException("G001 replay must contain six reel stops");
        int setIndex = Integer.parseInt(parts[1]);
        int scatters = Integer.parseInt(parts[2]);
        if (scatters < 0 || scatters > 24) throw new IllegalArgumentException("Invalid G001 scatter count");
        return new WalkingWildReplayPayload(setIndex, scatters, stops,
                parsePositions(parts[4]), parsePositions(parts[5]), parsePositions(parts[6]));
    }

    private static String positions(List<WildPosition> positions) {
        return positions.stream().map(position -> position.reel() + ":" + position.row())
                .collect(Collectors.joining(","));
    }

    private static List<WildPosition> parsePositions(String value) {
        if (value.isEmpty()) return List.of();
        List<WildPosition> positions = new ArrayList<>();
        for (String encoded : value.split(",")) {
            String[] coordinates = encoded.split(":", -1);
            if (coordinates.length != 2) throw new IllegalArgumentException("Invalid G001 replay position");
            int reel = Integer.parseInt(coordinates[0]);
            int row = Integer.parseInt(coordinates[1]);
            if (reel < 0 || reel >= 6 || row < 0 || row >= 4) {
                throw new IllegalArgumentException("G001 replay position outside the screen");
            }
            positions.add(new WildPosition(reel, row));
        }
        return List.copyOf(positions);
    }
}
