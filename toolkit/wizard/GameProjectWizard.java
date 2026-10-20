package toolkit.wizard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Creates a complete game workspace layout without generating game logic. */
public final class GameProjectWizard {
    private static final Pattern CODE_IN_FOLDER = Pattern.compile("^G(\\d{3,})-");
    private static final Pattern CODE_FOLDER_NAME = Pattern.compile("^G\\d{3,}-(.+)$");
    private static final Pattern CODE_IN_README = Pattern.compile("Game code:.*?G(\\d{3,})-");

    private GameProjectWizard() {}

    public static CreatedWorkspace create(Path gamesRoot, String gameName) throws IOException {
        String displayName = gameName == null ? "" : gameName.trim();
        if (displayName.isEmpty()) throw new IllegalArgumentException("Enter a game name.");
        String codeSuffix = slug(displayName);
        String packageName = codeSuffix.replace('-', '_');
        if (!Character.isLetter(packageName.charAt(0)) && packageName.charAt(0) != '_') {
            packageName = "g_" + packageName;
        }

        Path root = gamesRoot.toAbsolutePath().normalize();
        Files.createDirectories(root);
        ensureGameNameIsAvailable(root, codeSuffix);
        String gameCode = String.format("G%03d-%s", nextCodeNumber(root), codeSuffix);
        Path destination = root.resolve(gameCode);
        if (Files.exists(destination)) {
            throw new IllegalArgumentException("Game folder already exists: " + destination);
        }

        Path staging = root.resolve(".game-workspace-" + UUID.randomUUID());
        try {
            Files.createDirectory(staging);
            Path source = Files.createDirectory(staging.resolve("source"));
            Path implementation = staging.resolve("src/main/java/game").resolve(packageName);
            Path tests = staging.resolve("src/test/java/game").resolve(packageName).resolve("tests");
            Files.createDirectories(implementation);
            Files.createDirectories(tests);
            Files.writeString(implementation.resolve("README.md"),
                    "# Game implementation package\n\nAdd this game's Java classes here. Keep the package name `game."
                            + packageName + "` and put shared framework code outside this game module.\n",
                    StandardCharsets.UTF_8);
            Files.writeString(tests.resolve("README.md"),
                    "# Game tests\n\nAdd deterministic tests for this game's rules here.\n",
                    StandardCharsets.UTF_8);
            Files.writeString(staging.resolve("README.md"),
                    readme(displayName, gameCode, packageName),
                    StandardCharsets.UTF_8);
            Files.writeString(staging.resolve("AGENT-REVIEW.md"), agentReviewTemplate(),
                    StandardCharsets.UTF_8);
            Files.writeString(source.resolve("GDD.md"), gddTemplate(displayName),
                    StandardCharsets.UTF_8);
            Files.writeString(source.resolve("edge-cases.md"), edgeCasesTemplate(),
                    StandardCharsets.UTF_8);
            Files.writeString(source.resolve("flowchart.md"), flowchartTemplate(displayName),
                    StandardCharsets.UTF_8);
            Files.writeString(source.resolve("game-creation-form.md"), gameCreationForm(),
                    StandardCharsets.UTF_8);
            ParTemplateWorkbookWriter.write(source.resolve("PAR.xlsx"));
            try {
                Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(staging, destination);
            }
            return new CreatedWorkspace(displayName, gameCode, destination,
                    "game." + packageName);
        } catch (IOException | RuntimeException exception) {
            deleteTree(staging);
            throw exception;
        }
    }

    private static void ensureGameNameIsAvailable(Path gamesRoot, String codeSuffix) throws IOException {
        try (var children = Files.list(gamesRoot)) {
            for (Path child : children.filter(Files::isDirectory).toList()) {
                Matcher matcher = CODE_FOLDER_NAME.matcher(child.getFileName().toString());
                if (matcher.matches() && matcher.group(1).equals(codeSuffix)) {
                    throw new IllegalArgumentException("A workspace for this game name already exists: " + child);
                }
            }
        }
    }

    private static int nextCodeNumber(Path gamesRoot) throws IOException {
        int highest = 0;
        try (var children = Files.list(gamesRoot)) {
            for (Path child : children.filter(Files::isDirectory).toList()) {
                Matcher folderMatcher = CODE_IN_FOLDER.matcher(child.getFileName().toString());
                if (folderMatcher.find()) highest = Math.max(highest, Integer.parseInt(folderMatcher.group(1)));
                Path readme = child.resolve("README.md");
                if (Files.isRegularFile(readme)) {
                    Matcher readmeMatcher = CODE_IN_README.matcher(
                            Files.readString(readme, StandardCharsets.UTF_8));
                    if (readmeMatcher.find()) {
                        highest = Math.max(highest, Integer.parseInt(readmeMatcher.group(1)));
                    }
                }
            }
        }
        return highest + 1;
    }

    private static String slug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (normalized.isEmpty()) throw new IllegalArgumentException("Game name must include letters or numbers.");
        return normalized;
    }

    private static String readme(String name, String code, String packageName) {
        return """
                # New Game Workspace: %s

                **Game code:** `%s`  
                **Workspace folder:** `%s/`  
                **Suggested Java package:** `game.%s`

                This wizard creates a complete game workspace, but does not generate or register game code. Keep this game together in this `%s/` folder: source material, PAR, Java implementation, and game-specific tests.

                ## Step 0: prepare and review the source material

                1. Put the approved GDD, edge cases, flowchart, and PAR values in the matching files under `source/`. Keep source versions and dates in each document.
                2. Complete `source/game-creation-form.md` alongside the GDD. Confirm stake units, evaluation rules, feature triggers/retriggers, win cap, and set behavior.
                3. Treat the generated PAR workbook as a blank structural example. Replace its TODO cells and adapt tables to this game's rules; it is not yet a valid simulator configuration.
                4. Ask the agent to review the source package before writing game logic. The agent records conflicts, missing config, open questions, and proposed questions in `AGENT-REVIEW.md`.
                5. Resolve the review items with the game owner. Do not invent balance values or silently choose between conflicting sources.

                ## After the review is agreed

                Implement game-specific config and logic in `src/main/java/game/%s/`, with Java package `game.%s`. Add deterministic rule tests under `src/test/java/game/%s/tests/`. Reuse `GameModuleFramework` where its behavior matches the rules, and keep Basegame and randomly triggered Feature Game logic separate. Register `%s` in `game/GameFactory.java`, then use `./build.sh` to compile every game workspace into the shared runtime classpath and run a seeded simulation through the existing engine.

                Check RTP by mode and set, triggers, hit rate, distributions, caps, and report assets. Document assumptions and validation evidence in the game handover. Update `docs/architecture.md` when the implementation changes the framework or completes a task.

                ## Workspace files

                - `source/GDD.md`: game rules and approved math.
                - `source/edge-cases.md`: required boundary behavior.
                - `source/flowchart.md`: round and feature flow.
                - `source/PAR.xlsx`: Config and Reels worksheet templates.
                - `source/game-creation-form.md`: implementation intake checklist.
                - `AGENT-REVIEW.md`: agent-authored questions, gaps, and proposals.
                - `src/main/java/game/%s/`: this game's Java implementation and config.
                - `src/test/java/game/%s/tests/`: deterministic tests for this game.
                """.formatted(name, code, code, packageName, code, packageName,
                        packageName, packageName, code, code, packageName, packageName);
    }

    private static String gddTemplate(String name) {
        return """
                # %s Game Design Document

                **Owner:**  
                **Status/version/date:**  
                **Authoritative source:**

                ## Game overview

                [Describe the player experience and the intended mathematical model.]

                ## Basegame

                - Screen / reels:
                - Symbols and special behavior:
                - Outcome generation:
                - Win evaluation and win selection:
                - Paytable units and stake basis:
                - Basegame set selection:
                - Feature trigger conditions:

                ## Feature Games

                [Describe each randomly triggered feature separately. Include award, spin rules, retriggers, set selection, and return-to-round behavior.]

                ## Round-level rules

                - Win cap and whether it clips a spin or stops later events:
                - Rounding rules:
                - Total round win calculation:
                - Other rules:

                ## Expected math and balance targets

                - Target RTP:
                - Basegame / feature RTP targets:
                - Hit rate / feature frequency targets:
                - Volatility or distribution targets:

                ## Decisions still requiring approval

                | Topic | Open decision | Owner | Status |
                |---|---|---|---|
                | | | | Open |

                ## Source references

                - [Document name, version, date, and location]
                """.formatted(name);
    }

    private static String edgeCasesTemplate() {
        return """
                # Edge Cases and Expected Behavior

                Record expected outcomes from the approved rules. Add a source reference for each case.

                | ID | Situation / setup | Expected behavior | Source reference | Test status |
                |---|---|---|---|---|
                | EC-001 | No win on the basegame | | | Not tested |
                | EC-002 | Minimum winning outcome | | | Not tested |
                | EC-003 | Multiple simultaneous wins | | | Not tested |
                | EC-004 | Feature trigger at the exact threshold | | | Not tested |
                | EC-005 | Feature retrigger or retrigger is disallowed | | | Not tested |
                | EC-006 | Win reaches or exceeds the round cap | | | Not tested |
                | EC-007 | Zero-weight or unavailable configuration choice | | | Not tested |
                | EC-008 | Feature outcome has no win | | | Not tested |

                ## Game-specific cases

                - [Add case]
                """;
    }

    private static String flowchartTemplate(String name) {
        return """
                # %s Game Flowchart

                Replace or extend this placeholder after confirming the rules in `GDD.md`.

                ```mermaid
                flowchart TD
                    Start([Round start]) --> Base[Play one Basegame outcome]
                    Base --> Resolve[Resolve Basegame wins]
                    Resolve --> Trigger{Random Feature Game triggered?}
                    Trigger -->|No| Cap[Apply round cap and finish]
                    Trigger -->|Yes| Feature[Play Feature Game outcome(s)]
                    Feature --> Retrigger{Retrigger or another mode?}
                    Retrigger -->|Yes| Feature
                    Retrigger -->|No| Cap
                    Cap --> End([Round complete])
                ```

                Add decisions, loops, and edge-case paths required by the approved specification.
                """.formatted(name);
    }

    private static String agentReviewTemplate() {
        return """
                # Agent Review Log

                The agent updates this file during the source-review phase, before implementation. Keep questions explicit and link them to source files and sections. Do not fill unanswered items with assumptions.

                ## Review status

                - Status: Not reviewed
                - Sources reviewed:
                - Review date:

                ## Open questions for the game owner

                | ID | Question | Why it matters | Source / location | Proposed options | Owner answer | Status |
                |---|---|---|---|---|---|---|

                ## Missing or incomplete PAR configuration

                | ID | Missing table/value | Required by rule | Suggested PAR location/shape | Owner response | Status |
                |---|---|---|---|---|---|

                ## Conflicts between source documents

                | ID | Source A | Source B | Conflict | Decision / owner | Status |
                |---|---|---|---|---|---|

                ## Proposed questions and implementation plan

                - Reusable GMF components that appear to fit:
                - Game-specific behaviors:
                - Potential framework gaps (do not implement before approval):
                - Proposed deterministic test cases:
                - Proposed validation and statistical checks:

                ## Decisions and change history

                | Date | Decision or source update | Owner | Impact |
                |---|---|---|---|
                """;
    }

    private static String gameCreationForm() throws IOException {
        Path template = Path.of("docs", "game-creation-form.md");
        if (!Files.isRegularFile(template)) {
            throw new IOException("Could not find docs/game-creation-form.md from the project root.");
        }
        return Files.readString(template, StandardCharsets.UTF_8)
                .replace("(game-creation-flow.md)", "(../../docs/game-creation-flow.md)")
                .replace("(game-math-validation-flow.md)", "(../../docs/game-math-validation-flow.md)")
                .replace("(architecture.md)", "(../../docs/architecture.md)");
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    public record CreatedWorkspace(String gameName, String gameCode, Path directory,
            String suggestedPackage) {}
}
