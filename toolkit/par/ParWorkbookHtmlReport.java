package toolkit.par;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/** Writes a portable HTML view of the source sheets included with a simulation report. */
public final class ParWorkbookHtmlReport {
    private ParWorkbookHtmlReport() {}

    public static void write(Path workbookPath, Path htmlPath) throws IOException {
        var workbook = ExcelWorkbookReader.read(workbookPath);
        try (var output = Files.newBufferedWriter(htmlPath, StandardCharsets.UTF_8)) {
            output.write("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">");
            output.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
            output.write("<title>Simulation PAR configuration</title><style>");
            output.write(":root{color-scheme:light;font:15px/1.5 Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif;color:#172033;background:#f3f6fb}*{box-sizing:border-box}body{margin:0}header{padding:30px clamp(20px,5vw,64px);color:#fff;background:#172554}header h1{margin:0 0 8px;font-size:clamp(1.6rem,3vw,2.4rem)}header p{margin:0;color:#dbeafe;max-width:900px}main{max-width:1500px;margin:auto;padding:24px clamp(16px,4vw,48px) 56px}section{margin:22px 0;padding:22px;background:#fff;border:1px solid #dbe3ef;border-radius:14px;box-shadow:0 3px 14px #0f172a0a}h2{margin:0 0 7px;color:#172554;font-size:1.25rem}h3{margin:18px 0 8px;color:#334155;font-size:1rem}.note,.source{color:#53627a}.table-wrap{width:100%;overflow:auto;border:1px solid #dbe3ef;border-radius:9px}table{border-collapse:separate;border-spacing:0;min-width:100%;font-size:.88rem}th,td{padding:8px 10px;text-align:left;white-space:pre;border-right:1px solid #e4eaf2;border-top:1px solid #e4eaf2}thead th{position:sticky;top:0;z-index:1;color:#1e3a8a;background:#eff6ff;font-weight:700}tbody tr:nth-child(even){background:#f8fafd}tbody th{position:sticky;left:0;background:#f8fafc;color:#334155}td:empty{background:#fbfcfe}.block{margin:16px 0}.badge{display:inline-block;padding:3px 8px;border-radius:999px;color:#1e3a8a;background:#dbeafe;font-size:.8rem}footer{padding:12px 0;color:#64748b;font-size:.85rem}@media(max-width:600px){section{padding:15px}th,td{padding:7px}");
            output.write("}</style></head><body><header><h1>Simulation PAR configuration</h1><p>Balancing inputs copied from the selected workbook for this simulation.</p><p class=\"source\">Source workbook: ");
            output.write(escape(workbookPath.getFileName().toString()));
            output.write("</p></header><main>");
            for (Map.Entry<String, ExcelWorkbookReader.Sheet> sheetEntry : workbook.sheets().entrySet()) {
                if (!sheetEntry.getKey().equals("Config") && !sheetEntry.getKey().equals("Reels")) {
                    continue;
                }
                ExcelWorkbookReader.Sheet sheet = sheetEntry.getValue();
                output.write("<section><h2>");
                output.write(escape(sheetEntry.getKey()));
                output.write("</h2>");
                if (sheetEntry.getKey().equals("Reels")) writeReelSets(output, sheet);
                else writeConfigBlocks(output, sheet);
                output.write("</section>");
            }
            output.write("<footer>Values are rendered from the Config and Reels sheets saved with this simulation.</footer></main></body></html>");
        }
    }

    private static void writeConfigBlocks(java.io.Writer output, ExcelWorkbookReader.Sheet sheet)
            throws IOException {
        List<List<Map<Integer, String>>> blocks = new ArrayList<>();
        List<Map<Integer, String>> block = new ArrayList<>();
        for (int rowNumber = 1; rowNumber <= sheet.lastRow(); rowNumber++) {
            Map<Integer, String> cells = sheet.rows().getOrDefault(rowNumber, Map.of());
            if (cells.isEmpty()) {
                if (!block.isEmpty()) { blocks.add(block); block = new ArrayList<>(); }
            } else block.add(cells);
        }
        if (!block.isEmpty()) blocks.add(block);
        for (int index = 0; index < blocks.size(); index++) {
            List<Map<Integer, String>> rows = blocks.get(index);
            String title = blockTitle(rows, index + 1);
            output.write("<div class=\"block\"><h3>");
            output.write(escape(title));
            output.write("</h3><div class=\"table-wrap\"><table><tbody>");
            int lastColumn = rows.stream().flatMap(row -> row.keySet().stream())
                    .mapToInt(Integer::intValue).max().orElse(0);
            for (Map<Integer, String> cells : rows) {
                output.write("<tr>");
                for (int column = 0; column <= lastColumn; column++) {
                    String value = cells.getOrDefault(column, "");
                    output.write(column == 0 ? "<th>" : "<td>");
                    output.write(escape(value));
                    output.write(column == 0 ? "</th>" : "</td>");
                }
                output.write("</tr>");
            }
            output.write("</tbody></table></div></div>");
        }
    }

    private static String blockTitle(List<Map<Integer, String>> rows, int index) {
        Map<Integer, String> first = rows.get(0);
        if (first.size() == 1) return first.values().iterator().next();
        String title = first.values().stream().filter(value -> !value.isBlank())
                .filter(value -> value.contains("PAYLINES") || value.contains("PAYTABLE")
                        || value.contains("CHANCETABLE") || value.contains("HEATMAP"))
                .findFirst().orElse("");
        return title.isEmpty() ? "Configuration table " + index : title;
    }

    private static void writeReelSets(java.io.Writer output, ExcelWorkbookReader.Sheet sheet)
            throws IOException {
        Map<Integer, String> setNames = sheet.rows().getOrDefault(1, Map.of());
        Map<Integer, String> reelHeaders = sheet.rows().getOrDefault(2, Map.of());
        for (int set = 0; set < 4; set++) {
            int startColumn = set * 6;
            String title = setNames.getOrDefault(startColumn, "Set " + set);
            output.write("<div class=\"block\"><h3><span class=\"badge\">");
            output.write(escape(title));
            output.write("</span> Reel strips</h3><p class=\"note\">Each row is a reel; columns show its symbol ID at each stop.</p>");
            output.write("<div class=\"table-wrap\"><table><thead><tr><th>Reel</th>");
            for (int rowNumber = 3; rowNumber <= sheet.lastRow(); rowNumber++) {
                Map<Integer, String> row = sheet.rows().getOrDefault(rowNumber, Map.of());
                boolean hasValues = false;
                for (int reel = 0; reel < 5; reel++) {
                    hasValues |= !row.getOrDefault(startColumn + reel, "").isEmpty();
                }
                if (hasValues) {
                    output.write("<th>Stop ");
                    output.write(Integer.toString(rowNumber - 2));
                    output.write("</th>");
                }
            }
            output.write("</tr></thead><tbody>");
            for (int reel = 0; reel < 5; reel++) {
                output.write("<tr><th>");
                output.write(escape(reelHeaders.getOrDefault(startColumn + reel, "Reel " + (reel + 1))));
                output.write("</th>");
                for (int rowNumber = 3; rowNumber <= sheet.lastRow(); rowNumber++) {
                    Map<Integer, String> row = sheet.rows().getOrDefault(rowNumber, Map.of());
                    boolean hasValues = false;
                    for (int checkedReel = 0; checkedReel < 5; checkedReel++) {
                        hasValues |= !row.getOrDefault(startColumn + checkedReel, "").isEmpty();
                    }
                    if (!hasValues) continue;
                    output.write("<td>");
                    output.write(escape(row.getOrDefault(startColumn + reel, "")));
                    output.write("</td>");
                }
                output.write("</tr>");
            }
            output.write("</tbody></table></div></div>");
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
