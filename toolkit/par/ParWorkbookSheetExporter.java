package toolkit.par;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Creates a smaller .xlsx containing only selected worksheet tabs. */
public final class ParWorkbookSheetExporter {
    private static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String DOC_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String CONTENT_NS = "http://schemas.openxmlformats.org/package/2006/content-types";

    private ParWorkbookSheetExporter() {}

    public static void export(Path source, Path destination, Set<String> includedSheets) throws IOException {
        try (ZipFile input = new ZipFile(source.toFile())) {
            Document workbook = parse(requiredEntry(input, "xl/workbook.xml"));
            Document relationships = parse(requiredEntry(input, "xl/_rels/workbook.xml.rels"));
            Document contentTypes = parse(requiredEntry(input, "[Content_Types].xml"));
            Map<String, String> targets = new java.util.LinkedHashMap<>();
            NodeList relNodes = relationships.getElementsByTagNameNS(PKG_REL_NS, "Relationship");
            for (int i = 0; i < relNodes.getLength(); i++) {
                Element rel = (Element) relNodes.item(i);
                targets.put(rel.getAttribute("Id"), rel.getAttribute("Target"));
            }

            Set<String> keptPaths = new LinkedHashSet<>();
            Set<String> keptRelationshipIds = new LinkedHashSet<>();
            NodeList sheetNodes = workbook.getElementsByTagNameNS(MAIN_NS, "sheet");
            for (int i = sheetNodes.getLength() - 1; i >= 0; i--) {
                Element sheet = (Element) sheetNodes.item(i);
                String name = sheet.getAttribute("name");
                String relationshipId = sheet.getAttributeNS(DOC_REL_NS, "id");
                if (!includedSheets.contains(name)) {
                    sheet.getParentNode().removeChild(sheet);
                    continue;
                }
                String target = targets.get(relationshipId);
                if (target == null) throw new IOException("Workbook sheet has no relationship: " + name);
                keptRelationshipIds.add(relationshipId);
                keptPaths.add(resolveSheetPath(target));
            }
            for (String required : includedSheets) {
                boolean found = false;
                NodeList remaining = workbook.getElementsByTagNameNS(MAIN_NS, "sheet");
                for (int i = 0; i < remaining.getLength(); i++) {
                    if (((Element) remaining.item(i)).getAttribute("name").equals(required)) found = true;
                }
                if (!found) throw new IOException("Workbook is missing required sheet: " + required);
            }

            relNodes = relationships.getElementsByTagNameNS(PKG_REL_NS, "Relationship");
            for (int i = relNodes.getLength() - 1; i >= 0; i--) {
                Element rel = (Element) relNodes.item(i);
                if (targets.containsKey(rel.getAttribute("Id"))
                        && isWorksheetTarget(rel.getAttribute("Type"))
                        && !keptRelationshipIds.contains(rel.getAttribute("Id"))) {
                    rel.getParentNode().removeChild(rel);
                }
            }

            NodeList overrides = contentTypes.getElementsByTagNameNS(CONTENT_NS, "Override");
            for (int i = overrides.getLength() - 1; i >= 0; i--) {
                Element override = (Element) overrides.item(i);
                String partName = override.getAttribute("PartName");
                if (partName.startsWith("/xl/worksheets/")
                        && !keptPaths.contains(partName.substring(1))) {
                    override.getParentNode().removeChild(override);
                }
            }

            try (OutputStream file = Files.newOutputStream(destination);
                    ZipOutputStream output = new ZipOutputStream(file)) {
                var entries = input.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (isOmittedSheet(name, keptPaths)) continue;
                    output.putNextEntry(new ZipEntry(name));
                    if (name.equals("xl/workbook.xml")) writeXml(workbook, output);
                    else if (name.equals("xl/_rels/workbook.xml.rels")) writeXml(relationships, output);
                    else if (name.equals("[Content_Types].xml")) writeXml(contentTypes, output);
                    else try (InputStream data = input.getInputStream(entry)) { data.transferTo(output); }
                    output.closeEntry();
                }
            }
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Could not export selected PAR sheets: " + exception.getMessage(), exception);
        }
    }

    private static boolean isOmittedSheet(String entryName, Set<String> keptPaths) {
        if (!entryName.startsWith("xl/worksheets/") || !entryName.endsWith(".xml")) return false;
        return !keptPaths.contains(entryName);
    }

    private static boolean isWorksheetTarget(String type) {
        return type.endsWith("/worksheet");
    }

    private static String resolveSheetPath(String target) {
        String normalized = target.replace('\\', '/');
        if (normalized.startsWith("/")) return normalized.substring(1);
        return normalized.startsWith("xl/") ? normalized : "xl/" + normalized;
    }

    private static InputStream requiredEntry(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) throw new IOException("Excel workbook is missing " + name);
        return zip.getInputStream(entry);
    }

    private static Document parse(InputStream input) throws Exception {
        try (input) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private static void writeXml(Document document, OutputStream output) throws Exception {
        var transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.transform(new DOMSource(document), new StreamResult(output));
    }
}
