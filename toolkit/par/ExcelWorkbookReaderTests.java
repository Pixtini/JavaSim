package toolkit.par;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/** Smoke tests for parsing the current PAR workbook's Open XML structure. */
public final class ExcelWorkbookReaderTests {
    private ExcelWorkbookReaderTests() {}

    public static void main(String[] args) throws Exception {
        Path workbookPath = args.length == 0
                ? Path.of("game/expandingwild/expandingWildPAR.xlsx")
                : Path.of(args[0]);
        var workbook = ExcelWorkbookReader.read(workbookPath);
        require(workbook.sheets().containsKey("Config"), "Config sheet found");
        require(workbook.sheets().containsKey("Reels"), "Reels sheet found");
        require("Game-Wide-Config".equals(workbook.requireSheet("Config").cell(2, 1)),
                "Config cell value read");
        require("BG-0".equals(workbook.requireSheet("Reels").cell(1, 0)),
                "Reels section title read");
        require("1".equals(workbook.requireSheet("Reels").cell(3, 0)),
                "Numeric reel stop read");
        Path htmlPath = Files.createTempFile("javasim-par-", ".html");
        try {
            ParWorkbookHtmlReport.write(workbookPath, htmlPath);
            String html = Files.readString(htmlPath, StandardCharsets.UTF_8);
            require(html.contains("Simulation PAR configuration"), "report HTML has a title");
            require(html.contains("Config") && html.contains("Reels"),
                    "report HTML includes both PAR sheets");
            require(html.contains("class=\"table-wrap\"") && html.contains("Each row is a reel"),
                    "report HTML formats configuration blocks and reel sets for readability");
            require(html.contains("Stop 60") && html.contains("REEL 1"),
                    "report HTML transposes reel strips to one reel per row");
        } finally {
            Files.deleteIfExists(htmlPath);
        }
        System.out.println("ExcelWorkbookReader parsed " + workbook.sheets().size()
                + " sheets from " + workbookPath);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
