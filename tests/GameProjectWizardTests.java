import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import toolkit.par.ExcelWorkbookReader;
import toolkit.wizard.GameProjectWizard;

/** Checks that the new-game wizard creates a complete, readable intake workspace. */
public final class GameProjectWizardTests {
    private GameProjectWizardTests() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("javasim-new-game-wizard-");
        try {
            var first = GameProjectWizard.create(temp.resolve("game"), "Wild Roads");
            require(first.gameCode().equals("G001-wild-roads"), "first game code generated");
            require(first.directory().getFileName().toString().equals(first.gameCode()),
                    "game code prefixes the workspace folder name");
            require(first.suggestedPackage().equals("game.wild_roads"), "valid package suggested");
            Path source = first.directory().resolve("source");
            for (String name : List.of("GDD.md", "edge-cases.md", "flowchart.md",
                    "game-creation-form.md", "PAR.xlsx")) {
                require(Files.isRegularFile(source.resolve(name)), name + " template created");
            }
            require(Files.isRegularFile(first.directory().resolve("README.md")),
                    "workspace instructions created");
            require(Files.readString(first.directory().resolve("README.md"))
                            .contains("**Workspace folder:** `G001-wild-roads/`"),
                    "workspace README shows the prefixed folder name");
            require(Files.isRegularFile(first.directory().resolve("AGENT-REVIEW.md")),
                    "agent review log created");
            var par = ExcelWorkbookReader.read(source.resolve("PAR.xlsx"));
            require(par.sheets().keySet().equals(java.util.Set.of("Config", "Reels", "Normal-Books")),
                    "placeholder PAR includes an empty Normal book table");
            require(par.requireSheet("Normal-Books").cell(1, 0).equals("Gameplay ID")
                    && par.requireSheet("Normal-Books").cell(1, 1).equals("Weight"), "book headers match the shared loader");
            require(Files.isDirectory(source.resolve("books")), "harvest destination directory created");
            require(par.requireSheet("Config").cell(1, 0).equals("Game-Wide-Config"),
                    "Config starter table is readable");
            require(par.requireSheet("Reels").cell(1, 0).equals("BG-0"),
                    "Reels starter set is readable");

            var second = GameProjectWizard.create(temp.resolve("game"), "Moon Temple");
            require(second.gameCode().equals("G002-moon-temple"), "game codes increment");
            boolean duplicateRejected = false;
            try {
                GameProjectWizard.create(temp.resolve("game"), "Wild Roads");
            } catch (IllegalArgumentException expected) {
                duplicateRejected = true;
            }
            require(duplicateRejected, "existing workspace is not overwritten");
            System.out.println("Passed GameProjectWizard workspace checks.");
        } finally {
            try (var paths = Files.walk(temp)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
