package game.ember_forge.tests;

import game.ember_forge.config.EmberForgeConfig;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import toolkit.player.*;

/** Optional desktop integration check: actual animated windows, mode charging and settlement. */
public final class EmberForgePrototypeCheck {
    private static Object field(Object target, String name) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("Desktop display required for this optional check");
        Path preview = Path.of("game/G002-ember-forge/validation/prototype.png");
        Files.deleteIfExists(preview);
        var complete = new CountDownLatch(1); var failure = new AtomicReference<Throwable>();
        List<PrototypePlayerFrame> frames = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                var constructor = PrototypePlayerFrame.class.getDeclaredConstructor(PlayerGameAdapter.class,
                        PlayerDisplayData.Round.class, double.class);
                constructor.setAccessible(true);
                for (String mode : List.of("Normal", "EM1", "BB1")) {
                    var adapter = PlayerAdapterFactory.create(EmberForgeConfig.ID, EmberForgeConfig.DEFAULT_PAR, mode, new Random(86));
                    var frame = constructor.newInstance(adapter, null, 1.0);
                    frames.add(frame); frame.setVisible(true);
                    ((JButton)field(frame,"spinButton")).doClick();
                    double charged = (double)field(frame,"balance");
                    if (Math.abs(charged - (1000 - adapter.costMultiplier())) > 1e-8)
                        throw new AssertionError(mode + " initial charge");
                    System.out.println(mode + " initial charge verified: " + adapter.costMultiplier());
                }
                var timer = new javax.swing.Timer(100, null);
                timer.addActionListener(event -> {
                    try {
                        var buy = frames.get(2);
                        String detail = ((JLabel)field(buy,"detailsLabel")).getText();
                        if (!Files.exists(preview) && detail.startsWith("Heat")) {
                            BufferedImage picture = new BufferedImage(buy.getWidth(), buy.getHeight(), BufferedImage.TYPE_INT_RGB);
                            var graphics = picture.createGraphics(); buy.paint(graphics); graphics.dispose();
                            ImageIO.write(picture, "png", preview.toFile());
                            System.out.println("Captured actual animated Freegame window with Heat: " + preview);
                        }
                        boolean idle = frames.stream().allMatch(f -> ((JButton)field(f,"spinButton")).isEnabled());
                        if (!idle) return;
                        for (var frame : frames) {
                            var round = (PlayerDisplayData.Round)field(frame,"lastRound");
                            double cost = (double)field(frame,"costMultiplier");
                            double expected = Math.round((1000 - cost + round.totalWin()) * 100) / 100.0;
                            if (Math.abs((double)field(frame,"balance") - expected) > 1e-8)
                                throw new AssertionError("Settlement wallet mismatch");
                            if (!((JButton)field(frame,"replayButton")).isEnabled())
                                throw new AssertionError("Replay unavailable after settlement");
                            System.out.println(frame.getTitle() + ": settled " + round.featureSpins().size()
                                    + " feature spins, win " + round.totalWin() + ", wallet " + expected);
                        }
                        var buyRound = (PlayerDisplayData.Round)field(buy,"lastRound");
                        if (buyRound.featureSpins().size() != 8) throw new AssertionError("Buy must present eight Freegames");
                        // Replay Last Play is deliberately a charged repeat in the shared prototype.
                        var normal = frames.get(0);
                        double before = (double)field(normal,"balance");
                        ((JButton)field(normal,"replayButton")).doClick();
                        if (Math.abs((double)field(normal,"balance") - (before - 1)) > 1e-8)
                            throw new AssertionError("Replay cost mismatch");
                        System.out.println("Replay button starts a charged repeat of the same stored display outcome.");
                        timer.stop(); complete.countDown();
                    } catch (Throwable t) { failure.set(t); timer.stop(); complete.countDown(); }
                });
                timer.start();
            } catch (Throwable t) { failure.set(t); complete.countDown(); }
        });
        if (!complete.await(180, TimeUnit.SECONDS)) failure.set(new AssertionError("UI check timed out"));
        SwingUtilities.invokeAndWait(() -> frames.forEach(PrototypePlayerFrame::dispose));
        if (failure.get() != null) { failure.get().printStackTrace(); System.exit(1); }
        System.out.println("Passed desktop prototype integration check."); System.exit(0);
    }
}
