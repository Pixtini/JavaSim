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
    private static final String HEADER = "Format Version,Gameplay ID,Game ID,Stake,Total Win,Basegame Win,"
            + "Feature Win,Event Index,Event Type,Event Win,Event Payload";
    private static final String FORMAT_VERSION = "1";

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
        SavedGameplayHeader summary = null;
        List<ReplayEvent> events = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!HEADER.equals(header)) {
                throw new IOException("Unrecognized saved gameplays CSV header");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> fields = parseRow(line);
                if (fields.size() != 11 || !fields.get(0).equals(FORMAT_VERSION)) {
                    throw new IOException("Invalid saved gameplay CSV row");
                }
                long id = parseLong(fields.get(1), "gameplay ID");
                if (id != requestedId) {
                    continue;
                }
                SavedGameplayHeader rowSummary = new SavedGameplayHeader(
                        fields.get(2), parseDouble(fields.get(3), "stake"),
                        parseDouble(fields.get(4), "total win"),
                        parseDouble(fields.get(5), "basegame win"),
                        parseDouble(fields.get(6), "feature win"));
                if (summary == null) {
                    summary = rowSummary;
                } else if (!summary.equals(rowSummary)) {
                    throw new IOException("Gameplay summary differs between event rows");
                }
                int eventIndex = (int) parseLong(fields.get(7), "event index");
                if (eventIndex != events.size()) {
                    throw new IOException("Saved gameplay event rows are missing or out of order");
                }
                events.add(new ReplayEvent(fields.get(8),
                        parseDouble(fields.get(9), "event win"), fields.get(10)));
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("Could not parse saved gameplay " + requestedId, exception);
        }
        if (summary == null) {
            throw new IOException("Saved gameplay ID not found: " + requestedId);
        }
        return new SavedGameplay(requestedId, summary.gameId(), summary.stake(),
                summary.totalWin(), summary.baseGameWin(), summary.featureGameWin(), events);
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

    private record SavedGameplayHeader(String gameId, double stake, double totalWin,
            double baseGameWin, double featureGameWin) {}
}
