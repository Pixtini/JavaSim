import game.ExhaustiveReelGame;
import game.Game;
import game.GameFactory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.reporting.Print;
import simulation.result.SimulationResult;
import toolkit.reelset.FullReelsetSimulator;
import toolkit.reelset.ReelsetReportPrinter;
import toolkit.replay.ReplayerMain;
import toolkit.player.PrototypePlayerFrame;
import toolkit.player.PlayerDisplayData;
import toolkit.player.PlayerAdapterFactory;
import toolkit.player.PlayerGameAdapter;
import toolkit.par.ParWorkbookModes;
import toolkit.viewer.WinFinder;
import toolkit.viewer.WinScreenPrinter;
import toolkit.wizard.GameProjectWizard;

/** Small desktop launcher for the simulator and its command-line toolkit. */
public final class ToolkitGUI extends JFrame {
    private static final String[] GAME_IDS = {"expanding-wild", "basic-proxy", "G001-walkingwild"};

    private final JTextArea outputArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready");
    private final java.util.List<JButton> runButtons = new java.util.ArrayList<>();
    private final JTextField replayCsv = new JTextField(28);
    private final JTextField replayId = new JTextField("1", 12);
    private PlayerDisplayData.Round viewerFoundRound;
    private double viewerFoundStake;
    private PlayerGameAdapter viewerFoundAdapter;

    private ToolkitGUI() {
        super("JavaSim Toolkit");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 680));
        setLayout(new BorderLayout(8, 8));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Simulation", createSimulationTab());
        tabs.addTab("Win viewer", createViewerTab());
        tabs.addTab("Full reelset", createReelsetTab());
        tabs.addTab("Replay", createReplayTab());
        tabs.addTab("Game launcher", createPlayerTab());
        tabs.addTab("New game", createGameWizardTab());
        add(tabs, BorderLayout.NORTH);

        outputArea.setEditable(false);
        outputArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        outputArea.setLineWrap(false);
        javax.swing.JScrollPane outputScroll = new javax.swing.JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));
        add(outputScroll, BorderLayout.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        add(statusLabel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createSimulationTab() {
        JPanel panel = formPanel();
        JComboBox<String> game = gameSelector();
        JTextField parWorkbook = new JTextField(
                Path.of("game", "expandingwild", "expandingWildPAR.xlsx").toString(), 30);
        JComboBox<String> mode = new JComboBox<>(new String[] {"Normal"});
        game.addActionListener(event -> {
            String selectedGame = String.valueOf(game.getSelectedItem());
            if (selectedGame.equals("G001-walkingwild")) {
                parWorkbook.setText(Path.of("game", "G001-walkingwild", "source",
                        "G001-walkingWildsPAR.xlsx").toString());
            } else if (selectedGame.equals("expanding-wild")) {
                parWorkbook.setText(Path.of("game", "expandingwild", "expandingWildPAR.xlsx").toString());
            }
            refreshParModes(parWorkbook, mode);
        });
        JButton choosePar = new JButton("Choose PAR…");
        JPanel parRow = new JPanel(new BorderLayout(6, 0));
        parRow.add(parWorkbook, BorderLayout.CENTER);
        parRow.add(choosePar, BorderLayout.EAST);
        choosePar.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser(Path.of("game", "expandingwild").toFile());
            chooser.setFileFilter(new FileNameExtensionFilter("Excel PAR workbook", "xlsx"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                parWorkbook.setText(chooser.getSelectedFile().getAbsolutePath());
                refreshParModes(parWorkbook, mode);
            }
        });
        parWorkbook.addActionListener(event -> refreshParModes(parWorkbook, mode));
        parWorkbook.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                refreshParModes(parWorkbook, mode);
            }
        });
        refreshParModes(parWorkbook, mode);
        JSpinner rounds = spinner(1_000_000L, 1L, Long.MAX_VALUE, 100_000L);
        JTextField stake = new JTextField("1.0", 14);
        JCheckBox useSeed = new JCheckBox("Use this seed");
        JTextField seed = new JTextField("5051410756155515478", 22);
        JCheckBox exportReport = new JCheckBox("Save report files", true);
        JCheckBox showAwards = new JCheckBox("Print award counts");
        JSpinner threads = spinner(Runtime.getRuntime().availableProcessors(), 1,
                Runtime.getRuntime().availableProcessors() * 4, 1);
        JSpinner partitions = spinner(256, 1, Integer.MAX_VALUE, 64);
        JSpinner saved = spinner(1_000, 0, Integer.MAX_VALUE, 100);

        addRow(panel, 0, "Game", game);
        addRow(panel, 1, "PAR workbook", parRow);
        addRow(panel, 2, "Mode", mode);
        addRow(panel, 3, "Rounds", rounds);
        addRow(panel, 4, "Stake per round", stake);
        JPanel seedRow = new JPanel(new BorderLayout(8, 0));
        seedRow.add(useSeed, BorderLayout.WEST);
        seedRow.add(seed, BorderLayout.CENTER);
        useSeed.addActionListener(event -> seed.setEnabled(useSeed.isSelected()));
        seed.setEnabled(false);
        addRow(panel, 5, "Seed", seedRow);
        addRow(panel, 6, "Worker threads", threads);
        addRow(panel, 7, "Logical partitions", partitions);
        addRow(panel, 8, "Replay records to keep", saved);
        JPanel options = new JPanel();
        options.add(exportReport);
        options.add(showAwards);
        addRow(panel, 9, "Options", options);

        JButton run = runButton("Run simulation", panel);
        run.addActionListener(event -> {
            try {
                SimConfig config = new SimConfig();
                config.gameId = (String) game.getSelectedItem();
                config.gameMode = String.valueOf(mode.getSelectedItem());
                String selectedPar = parWorkbook.getText().trim();
                if (selectedPar.isEmpty()) {
                    throw new IllegalArgumentException("Select a PAR workbook before starting a simulation.");
                }
                config.parWorkbookPath = Path.of(selectedPar).toAbsolutePath().normalize();
                config.rounds = ((Number) rounds.getValue()).longValue();
                config.stake = parsePositive(stake.getText(), "Stake");
                config.usePreviousSeed = useSeed.isSelected();
                if (config.usePreviousSeed) {
                    config.seed = Long.parseLong(seed.getText().trim());
                }
                config.threads = ((Number) threads.getValue()).intValue();
                config.partitions = ((Number) partitions.getValue()).intValue();
                config.maxSavedGameplays = ((Number) saved.getValue()).intValue();
                config.exportReport = exportReport.isSelected();
                config.showAwards = showAwards.isSelected();
                runTask("Simulation", output -> {
                    PrintStream previousError = System.err;
                    try {
                        System.setErr(output);
                        Game selectedGame = GameFactory.create(config.gameId,
                                config.parWorkbookPath, config.gameMode);
                        SimulationResult result = new SimulationRunner(config, selectedGame).run();
                        new Print(result).printToConsole(config,
                                new PrintWriter(output, true, StandardCharsets.UTF_8));
                    } finally {
                        System.setErr(previousError);
                    }
                });
            } catch (RuntimeException exception) {
                showInputError(exception);
            }
        });
        return panel;
    }

    private void refreshParModes(JTextField workbookPath, JComboBox<String> modeSelector) {
        String selectedMode = String.valueOf(modeSelector.getSelectedItem());
        List<String> modes;
        try {
            String path = workbookPath.getText().trim();
            modes = path.isEmpty() ? List.of("Normal")
                    : ParWorkbookModes.availableModes(Path.of(path));
        } catch (Exception exception) {
            modes = List.of("Normal");
            modeSelector.setToolTipText(exception.getMessage());
        }
        modeSelector.removeAllItems();
        for (String availableMode : modes) {
            modeSelector.addItem(availableMode);
        }
        int existingSelection = modes.indexOf(selectedMode);
        int normalIndex = modes.indexOf("Normal");
        modeSelector.setSelectedIndex(existingSelection >= 0 ? existingSelection
                : normalIndex >= 0 ? normalIndex : 0);
        modeSelector.setToolTipText("The selected mode is used for every round in this simulation.");
    }

    private JPanel createViewerTab() {
        JPanel panel = formPanel();
        JComboBox<String> game = gameSelector();
        JTextField parWorkbook = new JTextField(
                Path.of("game", "expandingwild", "expandingWildPAR.xlsx").toString(), 30);
        JComboBox<String> mode = new JComboBox<>(new String[] {"Normal"});
        game.addActionListener(event -> {
            String selected = String.valueOf(game.getSelectedItem());
            if (selected.equals("G001-walkingwild")) {
                parWorkbook.setText(Path.of("game", "G001-walkingwild", "source",
                        "G001-walkingWildsPAR.xlsx").toString());
            } else if (selected.equals("expanding-wild")) {
                parWorkbook.setText(Path.of("game", "expandingwild", "expandingWildPAR.xlsx").toString());
            }
            refreshParModes(parWorkbook, mode);
        });
        JButton choosePar = new JButton("Choose PAR…");
        JPanel parRow = new JPanel(new BorderLayout(6, 0));
        parRow.add(parWorkbook, BorderLayout.CENTER);
        parRow.add(choosePar, BorderLayout.EAST);
        choosePar.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser(Path.of(parWorkbook.getText()).toFile());
            chooser.setFileFilter(new FileNameExtensionFilter("Excel PAR workbook", "xlsx"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                parWorkbook.setText(chooser.getSelectedFile().getAbsolutePath());
                refreshParModes(parWorkbook, mode);
            }
        });
        parWorkbook.addActionListener(event -> refreshParModes(parWorkbook, mode));
        refreshParModes(parWorkbook, mode);
        JSpinner spins = spinner(100L, 1L, Long.MAX_VALUE, 100L);
        JTextField stake = new JTextField("1.0", 14);
        JButton openFoundPlay = new JButton("Open found play in player");
        openFoundPlay.setEnabled(false);
        addRow(panel, 0, "Game", game);
        addRow(panel, 1, "PAR workbook", parRow);
        addRow(panel, 2, "Mode", mode);
        addRow(panel, 3, "Maximum spins", spins);
        addRow(panel, 4, "Base stake", stake);
        JButton run = new JButton("Find a winning screen");
        runButtons.add(run);
        openFoundPlay.addActionListener(event -> {
            if (viewerFoundRound != null && viewerFoundAdapter != null) {
                PrototypePlayerFrame.openFoundPlay(viewerFoundAdapter,
                        viewerFoundRound, viewerFoundStake);
            }
        });
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        actions.add(run);
        actions.add(openFoundPlay);
        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.gridx = 1;
        actionConstraints.gridy = 5;
        actionConstraints.anchor = GridBagConstraints.LINE_START;
        actionConstraints.insets = new Insets(8, 0, 4, 5);
        actionConstraints.gridwidth = 2;
        panel.add(actions, actionConstraints);
        run.addActionListener(event -> {
            try {
                String gameId = (String) game.getSelectedItem();
                String modeValue = String.valueOf(mode.getSelectedItem());
                Path par = Path.of(parWorkbook.getText().trim()).toAbsolutePath().normalize();
                long maxSpins = ((Number) spins.getValue()).longValue();
                double stakeValue = parsePositive(stake.getText(), "Stake");
                viewerFoundRound = null;
                viewerFoundAdapter = null;
                openFoundPlay.setEnabled(false);
                runTask("Win viewer", output -> {
                    Game selectedGame = gameId.equals("basic-proxy")
                            ? GameFactory.create(gameId)
                            : GameFactory.create(gameId, par, modeValue);
                    WinFinder.Result result = new WinFinder(selectedGame, new Random(),
                            stakeValue, maxSpins).find();
                    if (result.foundWin()) {
                        WinScreenPrinter.print(result, stakeValue, output);
                        if (!gameId.equals("basic-proxy")) {
                            PlayerGameAdapter adapter = PlayerAdapterFactory.create(
                                    gameId, par, modeValue, new Random());
                            PlayerDisplayData.Round displayRound = adapter.adaptRound(result.winningRound());
                            SwingUtilities.invokeLater(() -> {
                                viewerFoundRound = displayRound;
                                viewerFoundAdapter = adapter;
                                viewerFoundStake = stakeValue;
                                openFoundPlay.setEnabled(true);
                            });
                        }
                    } else {
                        output.printf("Win could not be found within %d spins.%n", maxSpins);
                    }
                });
            } catch (RuntimeException exception) {
                showInputError(exception);
            }
        });
        return panel;
    }

    private JPanel createReelsetTab() {
        JPanel panel = formPanel();
        JComboBox<String> game = gameSelector();
        JTextField stake = new JTextField("1.0", 14);
        JSpinner workers = spinner(Math.max(1, Runtime.getRuntime().availableProcessors()),
                1, Runtime.getRuntime().availableProcessors() * 4, 1);
        addRow(panel, 0, "Game", game);
        addRow(panel, 1, "Stake", stake);
        addRow(panel, 2, "Worker threads", workers);
        JButton run = runButton("Run full reelset", panel);
        run.addActionListener(event -> {
            try {
                String gameId = (String) game.getSelectedItem();
                double stakeValue = parsePositive(stake.getText(), "Stake");
                int workerCount = ((Number) workers.getValue()).intValue();
                runTask("Full reelset", output -> {
                    Game selectedGame = GameFactory.create(gameId);
                    if (!(selectedGame instanceof ExhaustiveReelGame reelGame)) {
                        throw new IllegalArgumentException(
                                "This game does not support full reelset evaluation.");
                    }
                    long combinations = FullReelsetSimulator.countCombinations(reelGame);
                    output.printf("Game: %s%nSet-stop outcomes: %d%nWorkers: %d%n",
                            gameId, combinations, workerCount);
                    PrintStream previousError = System.err;
                    try {
                        System.setErr(output);
                        var result = new FullReelsetSimulator()
                                .simulate(reelGame, stakeValue, workerCount);
                        ReelsetReportPrinter.print(result, output);
                    } finally {
                        System.setErr(previousError);
                    }
                });
            } catch (RuntimeException exception) {
                showInputError(exception);
            }
        });
        return panel;
    }

    private JPanel createReplayTab() {
        JPanel panel = formPanel();
        JButton browse = new JButton("Choose CSV…");
        JPanel csvRow = new JPanel(new BorderLayout(6, 0));
        csvRow.add(replayCsv, BorderLayout.CENTER);
        csvRow.add(browse, BorderLayout.EAST);
        browse.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser(Path.of("reports").toFile());
            chooser.setFileFilter(new FileNameExtensionFilter("Saved gameplay CSV", "csv"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                replayCsv.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        addRow(panel, 0, "Saved gameplays CSV", csvRow);
        addRow(panel, 1, "Gameplay ID", replayId);
        JButton run = runButton("Replay and validate", panel);
        run.addActionListener(event -> {
            try {
                String csv = replayCsv.getText().trim();
                if (csv.isEmpty()) {
                    throw new IllegalArgumentException("Choose a saved_gameplays.csv file.");
                }
                Path csvPath = Path.of(csv);
                long id = Long.parseLong(replayId.getText().trim());
                runTask("Replay", output -> ReplayerMain.replay(csvPath, id, output));
            } catch (RuntimeException exception) {
                showInputError(exception);
            }
        });
        return panel;
    }

    private JPanel createPlayerTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        JTextArea description = new JTextArea(
                "Choose Expanding Wild or G001 Walking Wild, select its PAR workbook and mode, "
                + "then launch the resizable player. Both players show reel outcomes, winning lines, "
                + "freegame flow, auto play, balance, and replay of the last round.");
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setOpaque(false);
        panel.add(description, BorderLayout.CENTER);
        JButton openPlayer = new JButton("Launch a game");
        openPlayer.addActionListener(event -> PrototypePlayerFrame.open());
        panel.add(openPlayer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createGameWizardTab() {
        JPanel panel = formPanel();
        JTextField gameName = new JTextField(28);
        JTextField gamesRoot = new JTextField(Path.of("game").toString(), 28);
        JButton chooseRoot = new JButton("Choose folder…");
        JPanel rootRow = new JPanel(new BorderLayout(6, 0));
        rootRow.add(gamesRoot, BorderLayout.CENTER);
        rootRow.add(chooseRoot, BorderLayout.EAST);
        chooseRoot.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser(Path.of("game").toFile());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Choose the parent folder for game workspaces");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                gamesRoot.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        addRow(panel, 0, "Game name", gameName);
        addRow(panel, 1, "Game folder root", rootRow);
        JLabel guidance = new JLabel("<html>Creates source templates, a two-sheet PAR workbook, a placeholder flowchart, "
                + "and an agent review log. It does not generate game logic.</html>");
        GridBagConstraints guidanceConstraints = new GridBagConstraints();
        guidanceConstraints.gridx = 1;
        guidanceConstraints.gridy = 2;
        guidanceConstraints.anchor = GridBagConstraints.LINE_START;
        guidanceConstraints.fill = GridBagConstraints.HORIZONTAL;
        guidanceConstraints.insets = new Insets(8, 0, 8, 5);
        panel.add(guidance, guidanceConstraints);

        JButton create = new JButton("Initialize game workspace");
        runButtons.add(create);
        GridBagConstraints createConstraints = new GridBagConstraints();
        createConstraints.gridx = 1;
        createConstraints.gridy = 3;
        createConstraints.anchor = GridBagConstraints.LINE_START;
        createConstraints.insets = new Insets(8, 0, 4, 5);
        panel.add(create, createConstraints);
        create.addActionListener(event -> {
            String name = gameName.getText().trim();
            String root = gamesRoot.getText().trim();
            if (name.isEmpty()) {
                showInputError(new IllegalArgumentException("Enter a game name."));
                return;
            }
            if (root.isEmpty()) {
                showInputError(new IllegalArgumentException("Choose the game folder root."));
                return;
            }
            runTask("New game workspace", output -> {
                GameProjectWizard.CreatedWorkspace workspace =
                        GameProjectWizard.create(Path.of(root), name);
                output.println("Game workspace created.");
                output.println("Game code: " + workspace.gameCode());
                output.println("Game folder: " + workspace.directory());
                output.println("Suggested Java package: " + workspace.suggestedPackage());
                output.println("Read README.md first, then fill the files under source/.");
            });
        });
        return panel;
    }

    private void runTask(String name, Task task) {
        setRunButtonsEnabled(false);
        outputArea.append("\n--- " + name + " started ---\n");
        statusLabel.setText(name + " running…");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Throwable failure;

            @Override
            protected Void doInBackground() {
                PrintStream output = new PrintStream(new UiOutputStream(), true,
                        StandardCharsets.UTF_8);
                try {
                    task.run(output);
                    output.flush();
                } catch (Throwable throwable) {
                    failure = throwable;
                    throwable.printStackTrace(output);
                    output.flush();
                } finally {
                    output.close();
                }
                return null;
            }

            @Override
            protected void done() {
                if (failure == null) {
                    statusLabel.setText(name + " finished");
                    outputArea.append("--- " + name + " finished ---\n");
                } else {
                    statusLabel.setText(name + " failed: " + failure.getMessage());
                    outputArea.append("--- " + name + " failed ---\n");
                }
                setRunButtonsEnabled(true);
            }
        };
        worker.execute();
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private JComboBox<String> gameSelector() {
        return new JComboBox<>(GAME_IDS);
    }

    private JSpinner spinner(long value, long minimum, long maximum, long step) {
        return new JSpinner(new SpinnerNumberModel(value, minimum, maximum, step));
    }

    private JSpinner spinner(int value, int minimum, int maximum, int step) {
        return new JSpinner(new SpinnerNumberModel(value, minimum, maximum, step));
    }

    private void addRow(JPanel panel, int row, String label, Component component) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_END;
        left.insets = new Insets(5, 5, 5, 10);
        panel.add(new JLabel(label + ":"), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(5, 0, 5, 5);
        panel.add(component, right);
    }

    private JButton runButton(String title, JPanel panel) {
        JButton button = new JButton(title);
        runButtons.add(button);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = panel.getComponentCount() / 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(8, 0, 4, 5);
        panel.add(button, constraints);
        return button;
    }

    private void setRunButtonsEnabled(boolean enabled) {
        runButtons.forEach(button -> button.setEnabled(enabled));
    }

    private void showInputError(RuntimeException exception) {
        javax.swing.JOptionPane.showMessageDialog(this, exception.getMessage(),
                "Check the settings", javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    private static double parsePositive(String text, String label) {
        double value = Double.parseDouble(text.trim());
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(label + " must be finite and greater than zero.");
        }
        return value;
    }

    @FunctionalInterface
    private interface Task {
        void run(PrintStream output) throws Exception;
    }

    private final class UiOutputStream extends OutputStream {
        private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
        private boolean skipLineFeed;

        @Override
        public synchronized void write(int value) throws IOException {
            if (value == '\r') {
                publishPending();
                skipLineFeed = true;
            } else if (value == '\n') {
                if (skipLineFeed) {
                    skipLineFeed = false;
                } else {
                    publishPending();
                }
            } else {
                skipLineFeed = false;
                pending.write(value);
            }
        }

        private void publishPending() {
            String line = pending.toString(StandardCharsets.UTF_8) + System.lineSeparator();
            pending.reset();
            SwingUtilities.invokeLater(() -> {
                outputArea.append(line);
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ToolkitGUI().setVisible(true));
    }
}
