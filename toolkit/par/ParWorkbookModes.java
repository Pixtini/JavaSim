package toolkit.par;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Discovers and selects isolated mode-specific Config/Reels sheet pairs in a PAR workbook. */
public final class ParWorkbookModes {
    private static final String DEFAULT_MODE = "Normal";

    private ParWorkbookModes() {}

    public static List<String> availableModes(Path workbookPath) throws IOException {
        return availableModes(ExcelWorkbookReader.read(workbookPath));
    }

    public static List<String> availableModes(ExcelWorkbookReader.Workbook workbook) {
        return discover(workbook).values().stream()
                .map(ModeSheets::mode)
                .toList();
    }

    public static ModeSheets select(Path workbookPath, String requestedMode) throws IOException {
        return select(ExcelWorkbookReader.read(workbookPath), requestedMode);
    }

    public static ModeSheets select(ExcelWorkbookReader.Workbook workbook, String requestedMode) {
        String selectedMode = requestedMode == null || requestedMode.isBlank()
                ? DEFAULT_MODE : requestedMode.trim();
        ModeSheets sheets = discover(workbook).get(normalize(selectedMode));
        if (sheets == null) {
            throw new IllegalArgumentException("PAR workbook does not contain a complete Config/Reels pair for mode '"
                    + selectedMode + "'. Available modes: "
                    + String.join(", ", availableModes(workbook)));
        }
        return sheets;
    }

    private static Map<String, ModeSheets> discover(ExcelWorkbookReader.Workbook workbook) {
        Map<String, MutablePair> pairs = new LinkedHashMap<>();
        for (String sheetName : workbook.sheets().keySet()) {
            SheetIdentity identity = identify(sheetName);
            if (identity == null) {
                continue;
            }
            String key = normalize(identity.mode());
            MutablePair pair = pairs.computeIfAbsent(key,
                    ignored -> new MutablePair(identity.mode()));
            pair.add(identity, sheetName);
        }

        if (pairs.isEmpty()) {
            throw new IllegalArgumentException(
                    "PAR workbook must contain a Config/Reels pair, optionally prefixed by a mode name");
        }

        Map<String, ModeSheets> complete = new LinkedHashMap<>();
        List<String> incomplete = new ArrayList<>();
        for (Map.Entry<String, MutablePair> entry : pairs.entrySet()) {
            MutablePair pair = entry.getValue();
            if (pair.configSheet == null || pair.reelsSheet == null) {
                incomplete.add(pair.mode + " (missing "
                        + (pair.configSheet == null ? "Config" : "")
                        + (pair.configSheet == null && pair.reelsSheet == null ? " and " : "")
                        + (pair.reelsSheet == null ? "Reels" : "") + ")");
                continue;
            }
            complete.put(entry.getKey(), new ModeSheets(
                    pair.mode, pair.configSheet, pair.reelsSheet));
        }
        if (!incomplete.isEmpty()) {
            throw new IllegalArgumentException("PAR workbook has incomplete mode sheet pairs: "
                    + String.join(", ", incomplete));
        }
        return complete;
    }

    private static SheetIdentity identify(String sheetName) {
        String trimmed = sheetName.trim();
        if (trimmed.equalsIgnoreCase("Config")) {
            return new SheetIdentity(DEFAULT_MODE, SheetType.CONFIG);
        }
        if (trimmed.equalsIgnoreCase("Reels")) {
            return new SheetIdentity(DEFAULT_MODE, SheetType.REELS);
        }
        if (endsWithModeSuffix(trimmed, "-Config")) {
            String mode = trimmed.substring(0, trimmed.length() - "-Config".length()).trim();
            if (mode.isEmpty()) {
                throw new IllegalArgumentException("PAR sheet name has an empty mode prefix: " + sheetName);
            }
            return new SheetIdentity(canonicalMode(mode), SheetType.CONFIG);
        }
        if (endsWithModeSuffix(trimmed, "-Reels")) {
            String mode = trimmed.substring(0, trimmed.length() - "-Reels".length()).trim();
            if (mode.isEmpty()) {
                throw new IllegalArgumentException("PAR sheet name has an empty mode prefix: " + sheetName);
            }
            return new SheetIdentity(canonicalMode(mode), SheetType.REELS);
        }
        return null;
    }

    private static boolean endsWithModeSuffix(String value, String suffix) {
        return value.length() > suffix.length() && value.regionMatches(
                true, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    private static String canonicalMode(String mode) {
        return normalize(mode).equals(normalize(DEFAULT_MODE)) ? DEFAULT_MODE : mode;
    }

    private static String normalize(String mode) {
        return mode.trim().toLowerCase(Locale.ROOT);
    }

    private enum SheetType { CONFIG, REELS }

    private record SheetIdentity(String mode, SheetType type) {}

    private static final class MutablePair {
        private final String mode;
        private String configSheet;
        private String reelsSheet;

        private MutablePair(String mode) {
            this.mode = mode;
        }

        private void add(SheetIdentity identity, String sheetName) {
            String existing = identity.type() == SheetType.CONFIG ? configSheet : reelsSheet;
            if (existing != null) {
                throw new IllegalArgumentException("PAR workbook defines more than one "
                        + identity.type() + " sheet for mode '" + mode + "': "
                        + existing + " and " + sheetName);
            }
            if (identity.type() == SheetType.CONFIG) {
                configSheet = sheetName;
            } else {
                reelsSheet = sheetName;
            }
        }
    }

    public record ModeSheets(String mode, String configSheet, String reelsSheet) {
        public ModeSheets {
            if (mode == null || mode.isBlank() || configSheet == null || reelsSheet == null) {
                throw new IllegalArgumentException("Mode sheet selection must be complete");
            }
        }

        public Set<String> sheetNames() {
            return Set.of(configSheet, reelsSheet);
        }
    }
}
