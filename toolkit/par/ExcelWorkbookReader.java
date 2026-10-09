package toolkit.par;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Reads the cell values and sheet names from a standard .xlsx Open XML workbook. */
public final class ExcelWorkbookReader {
    private static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String DOC_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";

    private ExcelWorkbookReader() {}

    public static Workbook read(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            List<String> sharedStrings = readSharedStrings(zip);
            Element workbook = parse(requiredEntry(zip, "xl/workbook.xml"));
            Element relationships = parse(requiredEntry(zip, "xl/_rels/workbook.xml.rels"));
            Map<String, String> sheetTargets = new LinkedHashMap<>();
            NodeList relNodes = relationships.getElementsByTagNameNS(PKG_REL_NS, "Relationship");
            for (int i = 0; i < relNodes.getLength(); i++) {
                Element relationship = (Element) relNodes.item(i);
                sheetTargets.put(relationship.getAttribute("Id"), relationship.getAttribute("Target"));
            }

            Map<String, Sheet> sheets = new LinkedHashMap<>();
            NodeList sheetNodes = workbook.getElementsByTagNameNS(MAIN_NS, "sheet");
            for (int i = 0; i < sheetNodes.getLength(); i++) {
                Element sheetElement = (Element) sheetNodes.item(i);
                String name = sheetElement.getAttribute("name");
                String relationshipId = sheetElement.getAttributeNS(DOC_REL_NS, "id");
                String target = sheetTargets.get(relationshipId);
                if (target == null) {
                    throw new IOException("Workbook sheet has no relationship: " + name);
                }
                sheets.put(name, readSheet(zip, resolveSheetPath(target), sharedStrings));
            }
            return new Workbook(sheets);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Could not read Excel workbook " + path + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static List<String> readSharedStrings(ZipFile zip) throws Exception {
        ZipEntry entry = zip.getEntry("xl/sharedStrings.xml");
        if (entry == null) {
            return List.of();
        }
        Element root = parse(zip.getInputStream(entry));
        NodeList items = root.getElementsByTagNameNS(MAIN_NS, "si");
        List<String> strings = new ArrayList<>(items.getLength());
        for (int i = 0; i < items.getLength(); i++) {
            strings.add(readText((Element) items.item(i)));
        }
        return List.copyOf(strings);
    }

    private static Sheet readSheet(ZipFile zip, String entryName,
            List<String> sharedStrings) throws Exception {
        Element root = parse(requiredEntry(zip, entryName));
        Map<Integer, Map<Integer, String>> rows = new LinkedHashMap<>();
        NodeList rowNodes = root.getElementsByTagNameNS(MAIN_NS, "row");
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element rowElement = (Element) rowNodes.item(i);
            int rowNumber = Integer.parseInt(rowElement.getAttribute("r"));
            Map<Integer, String> cells = new LinkedHashMap<>();
            NodeList cellNodes = rowElement.getElementsByTagNameNS(MAIN_NS, "c");
            for (int j = 0; j < cellNodes.getLength(); j++) {
                Element cell = (Element) cellNodes.item(j);
                String reference = cell.getAttribute("r");
                int column = columnIndex(reference);
                String value = readCell(cell, sharedStrings);
                if (!value.isEmpty()) {
                    cells.put(column, value);
                }
            }
            rows.put(rowNumber, cells);
        }
        return new Sheet(rows);
    }

    private static String readCell(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return readText(cell);
        }
        NodeList values = cell.getElementsByTagNameNS(MAIN_NS, "v");
        if (values.getLength() == 0) {
            return "";
        }
        String value = values.item(0).getTextContent();
        if ("s".equals(type)) {
            return sharedStrings.get(Integer.parseInt(value));
        }
        if ("b".equals(type)) {
            return "1".equals(value) ? "TRUE" : "FALSE";
        }
        return value;
    }

    private static String readText(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList nodes = element.getElementsByTagNameNS(MAIN_NS, "t");
        for (int i = 0; i < nodes.getLength(); i++) {
            text.append(nodes.item(i).getTextContent());
        }
        return text.toString();
    }

    private static Element parse(InputStream input) throws Exception {
        try (input) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(input).getDocumentElement();
        }
    }

    private static InputStream requiredEntry(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException("Excel workbook is missing " + name);
        }
        return zip.getInputStream(entry);
    }

    private static String resolveSheetPath(String target) {
        String normalized = target.replace('\\', '/');
        if (normalized.startsWith("/")) {
            return normalized.substring(1);
        }
        return normalized.startsWith("xl/") ? normalized : "xl/" + normalized;
    }

    private static int columnIndex(String cellReference) {
        int index = 0;
        for (int i = 0; i < cellReference.length(); i++) {
            char character = cellReference.charAt(i);
            if (character < 'A' || character > 'Z') {
                break;
            }
            index = index * 26 + (character - 'A' + 1);
        }
        return index - 1;
    }

    public record Workbook(Map<String, Sheet> sheets) {
        public Workbook {
            sheets = Collections.unmodifiableMap(new LinkedHashMap<>(sheets));
        }

        public Sheet requireSheet(String name) {
            Sheet sheet = sheets.get(name);
            if (sheet == null) {
                throw new IllegalArgumentException("PAR workbook must contain a '" + name + "' sheet");
            }
            return sheet;
        }
    }

    public record Sheet(Map<Integer, Map<Integer, String>> rows) {
        public Sheet {
            Map<Integer, Map<Integer, String>> copy = new LinkedHashMap<>();
            rows.forEach((row, cells) -> copy.put(row,
                    Collections.unmodifiableMap(new LinkedHashMap<>(cells))));
            rows = Collections.unmodifiableMap(copy);
        }

        public String cell(int row, int column) {
            return rows.getOrDefault(row, Map.of()).getOrDefault(column, "");
        }

        public int lastRow() {
            return rows.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        }
    }
}
