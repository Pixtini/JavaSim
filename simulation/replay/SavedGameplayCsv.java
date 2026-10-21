package simulation.replay;

import game.model.ReplayEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** CSV persistence for saved gameplays, with one row per ordered replay event. */
public final class SavedGameplayCsv {
    private static final String HEADER_V1 = "Format Version,Gameplay ID,Game ID,Stake,Total Win,Basegame Win,"
            + "Feature Win,Event Index,Event Type,Event Win,Event Payload";
    private static final String HEADER = "Format Version,Gameplay ID,Game ID,Game Mode,Stake,Total Win,Basegame Win,"
            + "Feature Win,Event Index,Event Type,Event Win,Event Payload";
    private static final String FORMAT_VERSION_V1 = "1";
    private static final String FORMAT_VERSION = "2";

    private SavedGameplayCsv() {}

    public static void write(Path path, List<SavedGameplay> gameplays) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (SavedGameplay gameplay : gameplays) {
                for (int eventIndex = 0; eventIndex < gameplay.events().size(); eventIndex++) {
                    ReplayEvent event = gameplay.events().get(eventIndex);
                    writeRow(writer, List.of(
                            FORMAT_VERSION,
                            Long.toString(gameplay.id()),
                            gameplay.gameId(),
                            gameplay.gameMode(),
                            Double.toString(gameplay.stake()),
                            Double.toString(gameplay.totalWin()),
                            Double.toString(gameplay.baseGameWin()),
                            Double.toString(gameplay.featureGameWin()),
                            Integer.toString(eventIndex),
                            event.type(),
                            Double.toString(event.win()),
                            event.payload()));
                }
            }
        }
    }

    public static SavedGameplay readById(Path path, long requestedId) throws IOException {
        return readByIds(path, java.util.Set.of(requestedId)).get(requestedId);
    }

    /** Loads requested gameplays in a single file pass; unreferenced event payloads are not retained. */
    public static java.util.Map<Long, SavedGameplay> readByIds(Path path,
            java.util.Set<Long> requestedIds) throws IOException {
        java.util.Set<Long> ids = java.util.Set.copyOf(requestedIds);
        if (ids.isEmpty() || ids.stream().anyMatch(id -> id <= 0))
            throw new IllegalArgumentException("Requested gameplay IDs must be positive and nonempty");
        java.util.Map<Long, SavedGameplayHeader> summaries = new java.util.LinkedHashMap<>();
        java.util.Map<Long, List<ReplayEvent>> events = new java.util.LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            boolean versionOne = HEADER_V1.equals(header);
            if (!versionOne && !HEADER.equals(header))
                throw new IOException("Unrecognized saved gameplays CSV header");
            String expectedVersion = versionOne ? FORMAT_VERSION_V1 : FORMAT_VERSION;
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> fields = parseRow(line);
                if (fields.size() != (versionOne ? 11 : 12) || !fields.get(0).equals(expectedVersion))
                    throw new IOException("Invalid saved gameplay CSV row");
                long id = parseLong(fields.get(1), "gameplay ID");
                if (!ids.contains(id)) continue;
                int shift = versionOne ? 0 : 1;
                SavedGameplayHeader summary = new SavedGameplayHeader(fields.get(2),
                        versionOne ? "Normal" : fields.get(3),
                        parseDouble(fields.get(3 + shift), "stake"),
                        parseDouble(fields.get(4 + shift), "total win"),
                        parseDouble(fields.get(5 + shift), "basegame win"),
                        parseDouble(fields.get(6 + shift), "feature win"));
                SavedGameplayHeader previous = summaries.putIfAbsent(id, summary);
                if (previous != null && !previous.equals(summary))
                    throw new IOException("Gameplay " + id + " summary differs between event rows");
                List<ReplayEvent> sequence = events.computeIfAbsent(id, ignored -> new ArrayList<>());
                long eventIndex = parseLong(fields.get(7 + shift), "event index");
                if (eventIndex != sequence.size())
                    throw new IOException("Saved gameplay " + id + " events are missing or out of order");
                sequence.add(new ReplayEvent(fields.get(8 + shift),
                        parseDouble(fields.get(9 + shift), "event win"), fields.get(10 + shift)));
            }
            java.util.Map<Long, SavedGameplay> result = new java.util.LinkedHashMap<>();
            for (long id : ids) {
                SavedGameplayHeader summary = summaries.get(id);
                if (summary == null) throw new IOException("Saved gameplay ID not found: " + id);
                result.put(id, new SavedGameplay(id, summary.gameId(), summary.gameMode(), summary.stake(),
                        summary.totalWin(), summary.baseGameWin(), summary.featureGameWin(), events.get(id)));
            }
            return java.util.Collections.unmodifiableMap(result);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Could not parse saved gameplays: " + exception.getMessage(), exception);
        }
    }

    private static void writeRow(BufferedWriter writer, List<String> fields) throws IOException {
        for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
            if (fieldIndex > 0) {
                writer.write(',');
            }
            writer.write(quote(fields.get(fieldIndex)));
        }
        writer.newLine();
    }

    private static String quote(String field) {
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    private static List<String> parseRow(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (quoted) {
                if (current == '"' && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else if (current == '"') {
                    quoted = false;
                } else {
                    field.append(current);
                }
            } else if (current == '"' && field.isEmpty()) {
                quoted = true;
            } else if (current == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        if (quoted) {
            throw new IOException("Unterminated quoted field in saved gameplays CSV");
        }
        fields.add(field.toString());
        return fields;
    }

    private static long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + fieldName, exception);
        }
    }

    private static double parseDouble(String value, String fieldName) {
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                throw new NumberFormatException("non-finite value");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + fieldName, exception);
        }
    }

    private record SavedGameplayHeader(String gameId, String gameMode, double stake, double totalWin,
            double baseGameWin, double featureGameWin) {}
}
