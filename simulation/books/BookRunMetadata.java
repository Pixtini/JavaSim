package simulation.books;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;

/** Identifies the exact book inputs and their finite weighted expectation for one run. */
public record BookRunMetadata(Path workbook, String mode, String sheetName, Path gameplayCsv,
        int tableRows, int activeGameplays, long totalWeight, double expectedRtp,
        double triggerProbability, String workbookSha256, String gameplaySha256) {
    public static String sha256(Path path) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    /** Refuse to export a book that changed after simulation preflight. */
    public void verifyInputsUnchanged() throws IOException {
        if (!sha256(workbook).equals(workbookSha256) || !sha256(gameplayCsv).equals(gameplaySha256))
            throw new IOException("Book inputs changed during the run; rerun before exporting a report");
    }
}
