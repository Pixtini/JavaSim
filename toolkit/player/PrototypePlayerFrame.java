package toolkit.player;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/** Resizable prototype slot player that presents game results without running a simulation. */
public final class PrototypePlayerFrame extends JFrame {
    private final PlayerGameAdapter game = new ExpandingWildPlayerAdapter(new Random());
    private final JTextField stakeField = new JTextField("1.00", 8);
    private final JSpinner autoRoundCount = new JSpinner(
            new SpinnerNumberModel(10, 1, 1_000_000, 1));
    private final JButton spinButton = new JButton("SPIN");
    private final JButton replayButton = new JButton("REPLAY LAST PLAY");
    private final JButton autoPlayButton = new JButton("AUTO PLAY");
    private final JButton stopAutoPlayButton = new JButton("STOP");
    private final JLabel gameLabel = new JLabel(game.gameName());
    private final JLabel modeLabel = new JLabel("Ready to play", SwingConstants.CENTER);
    private final JLabel detailsLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel awardLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel trackerLabel = new JLabel("Press Spin to play", SwingConstants.CENTER);
    private final JLabel totalLabel = new JLabel("Round win: £0.00", SwingConstants.CENTER);
    private final JLabel balanceLabel = new JLabel("Balance: £1,000.00", SwingConstants.CENTER);
    private final ReelViewport viewport = new ReelViewport();

    private double balance = 1_000.00;
    private double roundStake;
    private int autoRoundsRemaining;
    private boolean autoPlayActive;
    private boolean stopAutoAfterRound;
    private boolean previewingFoundPlay;
    private Timer nextAutoRoundTimer;
    private PlayerDisplayData.Round lastRound;
    private double lastRoundStake = 1.0;

    public PrototypePlayerFrame() {
        this(null, 1.0);
    }

    private PrototypePlayerFrame(PlayerDisplayData.Round foundRound, double foundStake) {
        super("JavaSim Prototype Player");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(700, 620));
        setSize(1000, 800);
        setLayout(new BorderLayout(10, 10));

        add(createControls(), BorderLayout.NORTH);
        add(createPlayArea(), BorderLayout.CENTER);
        add(createWinTracker(), BorderLayout.SOUTH);
        spinButton.addActionListener(event -> startRound());
        replayButton.addActionListener(event -> replayLastRound());
        autoPlayButton.addActionListener(event -> startAutoPlay());
        stopAutoPlayButton.addActionListener(event -> requestAutoPlayStop());
        stopAutoPlayButton.setEnabled(false);
        if (foundRound != null) {
            lastRound = foundRound;
            lastRoundStake = foundStake;
            stakeField.setText(String.format(java.util.Locale.ROOT, "%.2f", foundStake));
            replayButton.setEnabled(true);
        }
        pack();
        setSize(Math.max(getWidth(), 850), Math.max(getHeight(), 720));
        setLocationRelativeTo(null);
    }

    private JPanel createControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 12));
        gameLabel.setFont(gameLabel.getFont().deriveFont(Font.BOLD, 20f));
        controls.add(gameLabel);
        controls.add(new JLabel("Stake"));
        controls.add(stakeField);
        spinButton.setFont(spinButton.getFont().deriveFont(Font.BOLD, 18f));
        spinButton.setBackground(new Color(48, 139, 104));
        spinButton.setForeground(Color.WHITE);
        spinButton.setFocusPainted(false);
        spinButton.setBorder(BorderFactory.createEmptyBorder(9, 26, 9, 26));
        controls.add(spinButton);
        replayButton.setEnabled(false);
        controls.add(replayButton);
        controls.add(new JLabel("Auto rounds"));
        autoRoundCount.setPreferredSize(new Dimension(78, autoRoundCount.getPreferredSize().height));
        controls.add(autoRoundCount);
        controls.add(autoPlayButton);
        controls.add(stopAutoPlayButton);
        return controls;
    }

    private JPanel createPlayArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD, 16f));
        awardLabel.setFont(awardLabel.getFont().deriveFont(Font.BOLD, 18f));
        awardLabel.setForeground(new Color(116, 72, 16));
        awardLabel.setOpaque(true);
        awardLabel.setBackground(new Color(255, 226, 133));
        awardLabel.setVisible(false);
        JPanel headings = new JPanel(new GridLayout(2, 1));
        headings.add(modeLabel);
        detailsLabel.setForeground(new Color(88, 94, 108));
        detailsLabel.setFont(detailsLabel.getFont().deriveFont(12f));
        headings.add(detailsLabel);
        area.add(headings, BorderLayout.NORTH);
        JPanel reelAndAward = new JPanel(new BorderLayout(4, 4));
        reelAndAward.add(awardLabel, BorderLayout.NORTH);
        reelAndAward.add(viewport, BorderLayout.CENTER);
        area.add(reelAndAward, BorderLayout.CENTER);
        area.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return area;
    }

    private JPanel createWinTracker() {
        JPanel tracker = new JPanel(new GridLayout(3, 1, 2, 2));
        tracker.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Win tracker"),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        trackerLabel.setFont(trackerLabel.getFont().deriveFont(Font.BOLD, 17f));
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 15f));
        balanceLabel.setFont(balanceLabel.getFont().deriveFont(Font.BOLD, 15f));
        tracker.add(trackerLabel);
        tracker.add(totalLabel);
        tracker.add(balanceLabel);
        return tracker;
    }

    private void startRound() {
        if (autoPlayActive) {
            return;
        }
        Double stake = readStake();
        if (stake != null) {
            beginRound(stake);
        }
    }

    private void replayLastRound() {
        if (lastRound == null || !roundIsIdle() || autoPlayActive) {
            return;
        }
        if (balance < lastRoundStake) {
            showInsufficientBalance();
            return;
        }
        stakeField.setText(String.format(java.util.Locale.ROOT, "%.2f", lastRoundStake));
        beginRound(lastRoundStake, lastRound);
    }

    private void startAutoPlay() {
        if (autoPlayActive || !roundIsIdle()) {
            return;
        }
        Double stake = readStake();
        if (stake == null) {
            return;
        }
        if (balance < stake) {
            showInsufficientBalance();
            return;
        }
        autoRoundsRemaining = ((Number) autoRoundCount.getValue()).intValue();
        autoPlayActive = true;
        stopAutoAfterRound = false;
        setAutoControlsRunning(true);
        beginRound(stake);
    }

    private Double readStake() {
        try {
            double stake = Double.parseDouble(stakeField.getText().trim());
            if (!Double.isFinite(stake) || stake <= 0.0) {
                throw new NumberFormatException();
            }
            if (balance < stake) {
                showInsufficientBalance();
                return null;
            }
            return stake;
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Stake must be a positive number.",
                    "Invalid stake", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void beginRound(double stake) {
        beginRound(stake, null);
    }

    private void beginRound(double stake, PlayerDisplayData.Round replayRound) {
        roundStake = stake;
        balance = currency(balance - stake);
        updateBalanceLabel();
        spinButton.setEnabled(false);
        autoPlayButton.setEnabled(false);
        stopAutoPlayButton.setEnabled(autoPlayActive);
        stakeField.setEnabled(false);
        autoRoundCount.setEnabled(false);
        replayButton.setEnabled(false);
        awardLabel.setVisible(false);
        modeLabel.setText("Calculating round…");
        trackerLabel.setText("Waiting for spin result");
        new SwingWorker<PlayerDisplayData.Round, Void>() {
            @Override
            protected PlayerDisplayData.Round doInBackground() {
                return replayRound == null ? game.spin(stake) : replayRound;
            }

            @Override
            protected void done() {
                try {
                    PlayerDisplayData.Round round = get();
                    lastRound = round;
                    lastRoundStake = stake;
                    presentBaseGame(round);
                } catch (Exception exception) {
                    balance = currency(balance + roundStake);
                    updateBalanceLabel();
                    roundIsComplete();
                    JOptionPane.showMessageDialog(PrototypePlayerFrame.this,
                            "Could not play this round: " + rootMessage(exception),
                            "Player error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void presentBaseGame(PlayerDisplayData.Round round) {
        modeLabel.setText("Basegame");
        playSpin(round.baseGame(), () -> {
            if (round.featureTriggered()) {
                showFreeGameAward(round, 0);
            } else {
                completeRound(round);
            }
        });
    }

    private void showFreeGameAward(PlayerDisplayData.Round round, int freeGameIndex) {
        awardLabel.setText("FREE GAMES AWARDED  •  " + round.awardedFreeGames());
        awardLabel.setVisible(true);
        modeLabel.setText("Scatter feature triggered");
        Timer awardPause = new Timer(1250, event -> {
            awardLabel.setVisible(false);
            if (freeGameIndex < round.featureSpins().size()) {
                PlayerDisplayData.Spin freeSpin = round.featureSpins().get(freeGameIndex);
                modeLabel.setText("Freegame " + (freeGameIndex + 1) + " of "
                        + round.awardedFreeGames());
                playSpin(freeSpin, () -> showNextFreeGame(round, freeGameIndex + 1));
            } else {
                completeRound(round);
            }
        });
        awardPause.setRepeats(false);
        awardPause.start();
    }

    private void showNextFreeGame(PlayerDisplayData.Round round, int nextIndex) {
        if (nextIndex < round.featureSpins().size()) {
            PlayerDisplayData.Spin freeSpin = round.featureSpins().get(nextIndex);
            modeLabel.setText("Freegame " + (nextIndex + 1) + " of "
                    + round.awardedFreeGames());
            playSpin(freeSpin, () -> showNextFreeGame(round, nextIndex + 1));
        } else {
            completeRound(round);
        }
    }

    private void playSpin(PlayerDisplayData.Spin spin, Runnable finished) {
        modeLabel.setText(spin.mode());
        detailsLabel.setText(" ");
        viewport.animateSpin(spin, game.symbolPalette(), () ->
                viewport.animateExpansions(() -> {
                    detailsLabel.setText(spin.details());
                    showWins(spin, 0, finished);
                }));
    }

    private void showWins(PlayerDisplayData.Spin spin, int winIndex, Runnable finished) {
        if (winIndex >= spin.wins().size()) {
            viewport.clearActiveWin();
            if (spin.wins().isEmpty()) {
                trackerLabel.setText("No line wins on this spin");
                pauseThen(finished, 550);
            } else {
                finished.run();
            }
            return;
        }
        PlayerDisplayData.Win win = spin.wins().get(winIndex);
        viewport.setActiveWin(winIndex);
        trackerLabel.setText(win.description() + " pays " + money(win.amount()));
        pauseThen(() -> showWins(spin, winIndex + 1, finished), 850);
    }

    private void pauseThen(Runnable action, int milliseconds) {
        Timer timer = new Timer(milliseconds, event -> action.run());
        timer.setRepeats(false);
        timer.start();
    }

    private void completeRound(PlayerDisplayData.Round round) {
        if (!previewingFoundPlay) {
            balance = currency(balance + round.totalWin());
            updateBalanceLabel();
        }
        viewport.clearActiveWin();
        totalLabel.setText("Round win: " + money(round.totalWin()));
        if (round.featureTriggered() && round.featureSpins().isEmpty()) {
            detailsLabel.setText("The configured win cap prevented feature spins");
        }
        boolean anyWin = !round.baseGame().wins().isEmpty()
                || round.featureSpins().stream().anyMatch(spin -> !spin.wins().isEmpty());
        if (!anyWin) {
            trackerLabel.setText("No wins this round — spin to play again");
        }
        modeLabel.setText(previewingFoundPlay
                ? "Win viewer play loaded — press Replay Last Play to play it"
                : "Round complete");
        previewingFoundPlay = false;
        roundIsComplete();
    }

    private void roundIsComplete() {
        if (autoPlayActive) {
            autoRoundsRemaining--;
            if (stopAutoAfterRound) {
                endAutoPlay("Auto play stopped after this round");
            } else if (autoRoundsRemaining <= 0) {
                endAutoPlay("Auto play complete");
            } else if (balance < roundStake) {
                endAutoPlay("Auto play stopped: balance is below the stake");
            } else {
                modeLabel.setText("Auto play — " + autoRoundsRemaining + " rounds remaining");
                nextAutoRoundTimer = new Timer(400, event -> beginRound(roundStake));
                nextAutoRoundTimer.setRepeats(false);
                nextAutoRoundTimer.start();
                return;
            }
        }
        setControlsIdle();
    }

    private void requestAutoPlayStop() {
        if (!autoPlayActive) {
            return;
        }
        stopAutoAfterRound = true;
        stopAutoPlayButton.setEnabled(false);
        if (nextAutoRoundTimer != null && nextAutoRoundTimer.isRunning()) {
            nextAutoRoundTimer.stop();
            endAutoPlay("Auto play stopped");
            setControlsIdle();
        } else if (roundIsIdle()) {
            endAutoPlay("Auto play stopped");
            setControlsIdle();
        } else {
            modeLabel.setText("Will stop after this round, including its freegames");
        }
    }

    private boolean roundIsIdle() {
        return spinButton.isEnabled() && autoPlayButton.isEnabled();
    }

    private void setAutoControlsRunning(boolean running) {
        autoPlayButton.setEnabled(!running);
        autoRoundCount.setEnabled(!running);
        stopAutoPlayButton.setEnabled(running);
    }

    private void endAutoPlay(String message) {
        autoPlayActive = false;
        stopAutoAfterRound = false;
        if (nextAutoRoundTimer != null) {
            nextAutoRoundTimer.stop();
            nextAutoRoundTimer = null;
        }
        modeLabel.setText(message);
    }

    private void setControlsIdle() {
        spinButton.setEnabled(true);
        autoPlayButton.setEnabled(true);
        stopAutoPlayButton.setEnabled(false);
        stakeField.setEnabled(true);
        autoRoundCount.setEnabled(true);
        replayButton.setEnabled(lastRound != null);
    }

    private void showInsufficientBalance() {
        JOptionPane.showMessageDialog(this,
                "Your balance is less than the selected stake.",
                "Insufficient balance", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateBalanceLabel() {
        balanceLabel.setText("Balance: " + money(balance));
    }

    private String money(double amount) {
        return String.format(java.util.Locale.ROOT, "£%.2f", amount);
    }

    private double currency(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String rootMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.toString() : root.getMessage();
    }

    public static void open() {
        PrototypePlayerFrame frame = new PrototypePlayerFrame();
        frame.setVisible(true);
    }

    /** Opens the player with a win viewer result displayed and ready for replay. */
    public static void openFoundPlay(PlayerDisplayData.Round round, double stake) {
        PrototypePlayerFrame frame = new PrototypePlayerFrame(round, stake);
        frame.setVisible(true);
        frame.previewFoundPlay(round);
    }

    private void previewFoundPlay(PlayerDisplayData.Round round) {
        previewingFoundPlay = true;
        spinButton.setEnabled(false);
        replayButton.setEnabled(false);
        autoPlayButton.setEnabled(false);
        stakeField.setEnabled(false);
        autoRoundCount.setEnabled(false);
        presentBaseGame(round);
    }
}
