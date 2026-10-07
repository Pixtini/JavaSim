package toolkit.player;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Size-aware reel viewport that animates reel drops and highlights arbitrary win positions. */
public final class ReelViewport extends JPanel {
    private static final int REEL_GAP = 8;
    private static final int CELL_GAP = 5;
    private static final int ANIMATION_TICK_MILLIS = 95;
    private static final int REEL_STOP_STAGGER_TICKS = 5;
    private static final int REEL_ROLL_TICKS = 11;
    private static final int EXPANSION_TICK_MILLIS = 125;
    private static final Map<String, Color> STYLE_COLORS = Map.ofEntries(
            Map.entry("T1", new Color(177, 72, 74)),
            Map.entry("T2", new Color(198, 105, 48)),
            Map.entry("T3", new Color(166, 133, 38)),
            Map.entry("T4", new Color(45, 139, 139)),
            Map.entry("T5", new Color(74, 112, 181)),
            Map.entry("L1", new Color(76, 139, 83)),
            Map.entry("L2", new Color(58, 129, 111)),
            Map.entry("L3", new Color(117, 91, 159)),
            Map.entry("L4", new Color(166, 86, 132)),
            Map.entry("L5", new Color(102, 112, 129)),
            Map.entry("W", new Color(201, 133, 37)),
            Map.entry("S", new Color(148, 79, 168)),
            Map.entry("B", new Color(190, 93, 60)));

    private final Random animationRandom = new Random();
    private PlayerDisplayData.Spin spin;
    private List<PlayerDisplayData.SymbolCell> palette = List.of();
    private int activeWin = -1;
    private int animationTick;
    private int expandedReelCount;
    private double activeExpansionProgress;
    private boolean expansionsStarted;
    private Timer dropTimer;
    private Timer expansionTimer;

    public ReelViewport() {
        setOpaque(true);
        setBackground(new Color(25, 29, 39));
        setPreferredSize(new Dimension(760, 500));
        setMinimumSize(new Dimension(360, 300));
    }

    public void animateSpin(PlayerDisplayData.Spin nextSpin,
            List<PlayerDisplayData.SymbolCell> availableSymbols, Runnable finished) {
        if (dropTimer != null) {
            dropTimer.stop();
        }
        if (expansionTimer != null) {
            expansionTimer.stop();
            expansionTimer = null;
        }
        spin = nextSpin;
        palette = List.copyOf(availableSymbols);
        activeWin = -1;
        animationTick = 0;
        expandedReelCount = 0;
        activeExpansionProgress = 0.0;
        expansionsStarted = false;
        repaint();
        int finalStopTick = (spin.reelCount() - 1) * REEL_STOP_STAGGER_TICKS
                + REEL_ROLL_TICKS;
        dropTimer = new Timer(ANIMATION_TICK_MILLIS, event -> {
            animationTick++;
            repaint();
            if (animationTick >= finalStopTick) {
                dropTimer.stop();
                dropTimer = null;
                finished.run();
            }
        });
        dropTimer.start();
    }

    /** Reveals post-stop reel transformations only after every reel has settled. */
    public void animateExpansions(Runnable finished) {
        if (spin.expandingReels().isEmpty()) {
            finished.run();
            return;
        }
        expandedReelCount = 0;
        activeExpansionProgress = 0.0;
        Timer pause = new Timer(420, event -> {
            expansionsStarted = true;
            expansionTimer = new Timer(EXPANSION_TICK_MILLIS, tick -> {
                activeExpansionProgress = Math.min(1.0, activeExpansionProgress + 0.2);
                if (activeExpansionProgress >= 1.0) {
                    expandedReelCount++;
                    activeExpansionProgress = 0.0;
                }
                repaint();
                if (expandedReelCount >= spin.expandingReels().size()) {
                    expansionTimer.stop();
                    expansionTimer = null;
                    finished.run();
                }
            });
            expansionTimer.start();
        });
        pause.setRepeats(false);
        pause.start();
    }

    public void setActiveWin(int index) {
        activeWin = index;
        repaint();
    }

    public void clearActiveWin() {
        activeWin = -1;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (spin == null) {
            g.setColor(new Color(210, 215, 228));
            g.setFont(getFont().deriveFont(Font.BOLD, 22f));
            String prompt = "Press Spin to play";
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(prompt, (getWidth() - metrics.stringWidth(prompt)) / 2,
                    getHeight() / 2);
        } else {
            paintReels(g);
        }
        g.dispose();
    }

    private void paintReels(Graphics2D g) {
        int reelCount = spin.reelCount();
        int rowCount = spin.rowCount();
        int maxCellWidth = Math.max(42,
                (getWidth() - 48 - REEL_GAP * (reelCount - 1)) / reelCount);
        int maxCellHeight = Math.max(42,
                (getHeight() - 48 - CELL_GAP * (rowCount - 1)) / rowCount);
        int cellSize = Math.max(38, Math.min(maxCellWidth, maxCellHeight));
        int cellWidth = Math.min(cellSize, maxCellWidth);
        int cellHeight = Math.min((int) (cellSize * 0.86), maxCellHeight);
        int totalWidth = reelCount * cellWidth + (reelCount - 1) * REEL_GAP;
        int totalHeight = rowCount * cellHeight + (rowCount - 1) * CELL_GAP;
        int startX = (getWidth() - totalWidth) / 2;
        int startY = (getHeight() - totalHeight) / 2;

        PlayerDisplayData.Win highlighted = activeWin >= 0 && activeWin < spin.wins().size()
                ? spin.wins().get(activeWin) : null;
        for (int reel = 0; reel < reelCount; reel++) {
            int reelProgress = animationTick - reel * REEL_STOP_STAGGER_TICKS;
            if (reelProgress >= REEL_ROLL_TICKS) {
                paintStoppedReel(g, reel, highlighted, startX, startY, cellWidth, cellHeight);
            } else {
                paintRollingReel(g, reel, animationTick, startX, startY,
                        cellWidth, cellHeight, rowCount);
            }
        }
        if (highlighted != null) {
            paintWinLine(g, highlighted, startX, startY, cellWidth, cellHeight);
        }
    }

    private void paintStoppedReel(Graphics2D g, int reel, PlayerDisplayData.Win highlighted,
            int startX, int startY, int cellWidth, int cellHeight) {
        int sourceBannerRow = bannerRow(reel);
        int expansionIndex = spin.expandingReels().indexOf(reel);
        boolean fullyExpanded = expansionIndex >= 0 && expansionIndex < expandedReelCount;
        boolean animatingExpansion = expansionsStarted && expansionIndex == expandedReelCount
                && expansionIndex >= 0;
        int maxDistance = sourceBannerRow < 0 ? 0
                : Math.max(sourceBannerRow, spin.rowCount() - 1 - sourceBannerRow);
        for (int row = 0; row < spin.rowCount(); row++) {
            boolean revealWild = fullyExpanded || (animatingExpansion
                    && (sourceBannerRow == row || Math.abs(row - sourceBannerRow)
                            <= Math.ceil(activeExpansionProgress * maxDistance)));
            PlayerDisplayData.SymbolCell symbol = revealWild
                    ? spin.reels().get(reel).get(row)
                    : spin.stoppedReels().get(reel).get(row);
            int x = startX + reel * (cellWidth + REEL_GAP);
            int y = startY + row * (cellHeight + CELL_GAP);
            paintCell(g, symbol, x, y, cellWidth, cellHeight,
                    highlighted != null && contains(highlighted.positions(), reel, row));
        }
    }

    private int bannerRow(int reel) {
        for (int row = 0; row < spin.rowCount(); row++) {
            if (spin.stoppedReels().get(reel).get(row).label().equals("B")) {
                return row;
            }
        }
        return -1;
    }

    private void paintRollingReel(Graphics2D g, int reel, int reelProgress,
            int startX, int startY, int cellWidth, int cellHeight, int rowCount) {
        int x = startX + reel * (cellWidth + REEL_GAP);
        int pitch = cellHeight + CELL_GAP;
        int offset = reelProgress * 22 % pitch;
        var originalClip = g.getClip();
        g.clipRect(x, startY, cellWidth, rowCount * cellHeight + (rowCount - 1) * CELL_GAP);
        for (int row = -1; row <= rowCount; row++) {
            int y = startY + row * pitch + offset;
            paintCell(g, randomSymbol(reel, row), x, y, cellWidth, cellHeight, false);
        }
        g.setClip(originalClip);
    }

    private void paintCell(Graphics2D g, PlayerDisplayData.SymbolCell symbol,
            int x, int y, int width, int height, boolean winning) {
        Color base = STYLE_COLORS.getOrDefault(symbol.style(), new Color(87, 91, 111));
        g.setColor(base.darker());
        g.fillRoundRect(x, y + 3, width, height, 14, 14);
        g.setColor(base);
        g.fillRoundRect(x, y, width, height - 3, 14, 14);
        g.setColor(winning ? new Color(255, 232, 114) : new Color(255, 255, 255, 95));
        g.setStroke(new BasicStroke(winning ? 4f : 1.5f));
        g.drawRoundRect(x + 1, y + 1, width - 3, height - 5, 14, 14);
        g.setColor(Color.WHITE);
        float fontSize = Math.max(14f, Math.min(width / 3.2f, height / 2.7f));
        g.setFont(getFont().deriveFont(Font.BOLD, fontSize));
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(symbol.label())) / 2;
        int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(symbol.label(), textX, textY);
    }

    private void paintWinLine(Graphics2D g, PlayerDisplayData.Win win, int startX,
            int startY, int cellWidth, int cellHeight) {
        if (!win.connectPositions() || win.positions().size() < 2) {
            return;
        }
        g.setColor(new Color(28, 24, 15, 190));
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        paintWinSegments(g, win, startX, startY, cellWidth, cellHeight);
        g.setColor(new Color(255, 226, 88, 235));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        paintWinSegments(g, win, startX, startY, cellWidth, cellHeight);
    }

    private void paintWinSegments(Graphics2D g, PlayerDisplayData.Win win, int startX,
            int startY, int cellWidth, int cellHeight) {
        PlayerDisplayData.Position previous = win.positions().get(0);
        for (int i = 1; i < win.positions().size(); i++) {
            PlayerDisplayData.Position next = win.positions().get(i);
            g.draw(new Line2D.Double(centerX(previous.reel(), startX, cellWidth),
                    centerY(previous.row(), startY, cellHeight),
                    centerX(next.reel(), startX, cellWidth),
                    centerY(next.row(), startY, cellHeight)));
            previous = next;
        }
    }

    private int centerX(int reel, int startX, int cellWidth) {
        return startX + reel * (cellWidth + REEL_GAP) + cellWidth / 2;
    }

    private int centerY(int row, int startY, int cellHeight) {
        return startY + row * (cellHeight + CELL_GAP) + cellHeight / 2;
    }

    private PlayerDisplayData.SymbolCell randomSymbol(int reel, int row) {
        if (palette.isEmpty()) {
            return new PlayerDisplayData.SymbolCell("?", "L5");
        }
        int offset = Math.floorMod(reel * spin.rowCount() + row
                + animationRandom.nextInt(palette.size()), palette.size());
        return palette.get(offset);
    }

    private boolean contains(List<PlayerDisplayData.Position> positions, int reel, int row) {
        return positions.stream().anyMatch(position -> position.reel() == reel
                && position.row() == row);
    }
}
