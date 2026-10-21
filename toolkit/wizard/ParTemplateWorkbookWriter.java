package toolkit.wizard;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes rule sheets and an empty Normal book table for a new-game workspace. */
final class ParTemplateWorkbookWriter {
    private ParTemplateWorkbookWriter() {}

    static void write(Path path) throws IOException {
        Map<Integer, Map<Integer, String>> config = new LinkedHashMap<>();
        row(config, 1, 0, "Game-Wide-Config");
        row(config, 2, 0, "screen", 1, "TODO: REELSxROWS");
        row(config, 3, 0, "maxWin", 1, "TODO: multiplier");
        row(config, 5, 0, "PAYLINES");
        row(config, 6, 0, "TODO: add one row per payline; one visible-row index per reel");
        row(config, 9, 0, "PAYTABLE");
        row(config, 10, 0, "Symbol-ID", 1, "Symbol-Name", 2, "5oak", 3, "4oak", 4, "3oak", 5, "TYPE");
        row(config, 11, 0, "TODO: add symbols and awards");
        row(config, 14, 0, "Basegame");
        row(config, 15, 0, "CHANCETABLE:BG-setSelection");
        row(config, 16, 0, "value", 1, "weight");
        row(config, 17, 0, "TODO", 1, "TODO");
        row(config, 20, 0, "Freegame");
        row(config, 21, 0, "CHANCETABLE:FG-setSelection");
        row(config, 22, 0, "value", 1, "weight");
        row(config, 23, 0, "TODO", 1, "TODO");

        Map<Integer, Map<Integer, String>> reels = new LinkedHashMap<>();
        Map<Integer, Map<Integer, String>> books = new LinkedHashMap<>();
        row(books, 1, 0, "Gameplay ID", 1, "Weight");
        row(reels, 1, 0, "BG-0", 6, "FG-0");
        for (int setColumn : new int[] {0, 6}) {
            for (int reel = 0; reel < 5; reel++) {
                row(reels, 2, setColumn + reel, "REEL " + (reel + 1));
            }
        }

        try (OutputStream file = Files.newOutputStream(path);
                ZipOutputStream zip = new ZipOutputStream(file, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            put(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="Config" sheetId="1" r:id="rId1"/><sheet name="Reels" sheetId="2" r:id="rId2"/><sheet name="Normal-Books" sheetId="3" r:id="rId3"/></sheets>
                    </workbook>
                    """);
            put(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                      <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
                    </Relationships>
                    """);
            put(zip, "xl/worksheets/sheet1.xml", worksheetXml(config));
            put(zip, "xl/worksheets/sheet2.xml", worksheetXml(reels));
            put(zip, "xl/worksheets/sheet3.xml", worksheetXml(books));
        }
    }

    private static String worksheetXml(Map<Integer, Map<Integer, String>> rows) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        rows.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(row -> {
            xml.append("<row r=\"").append(row.getKey()).append("\">");
            row.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(cell -> {
                xml.append("<c r=\"").append(columnName(cell.getKey()) + row.getKey())
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(xmlEscape(cell.getValue())).append("</t></is></c>");
            });
            xml.append("</row>");
        });
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static void row(Map<Integer, Map<Integer, String>> sheet, int row,
            int firstColumn, String firstValue, Object... remainingCells) {
        Map<Integer, String> cells = sheet.computeIfAbsent(row, ignored -> new LinkedHashMap<>());
        cells.put(firstColumn, firstValue);
        for (int index = 0; index < remainingCells.length; index += 2) {
            cells.put((Integer) remainingCells[index], (String) remainingCells[index + 1]);
        }
    }

    private static String columnName(int zeroBasedColumn) {
        StringBuilder name = new StringBuilder();
        int column = zeroBasedColumn + 1;
        while (column > 0) {
            int remainder = (column - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            column = (column - 1) / 26;
        }
        return name.toString();
    }

    private static String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void put(ZipOutputStream zip, String name, String contents) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(contents.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
