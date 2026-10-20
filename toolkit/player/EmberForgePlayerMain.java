package toolkit.player;

import game.ember_forge.config.EmberForgeConfig;
import game.ember_forge.config.EmberForgeParParser;
import java.util.Random;
import javax.swing.SwingUtilities;

/** Direct launch of G002: optional purchase mode and deterministic seed. */
public final class EmberForgePlayerMain {
    private EmberForgePlayerMain() {}
    public static void main(String[] args) throws Exception {
        if (args.length > 2) throw new IllegalArgumentException("Expected [Normal|EM1|BB1] [seed]");
        var config = new EmberForgeParParser().parse(EmberForgeConfig.DEFAULT_PAR,
                args.length > 0 ? args[0] : "Normal");
        var adapter = new EmberForgePlayerAdapter(config,
                args.length > 1 ? new Random(Long.parseLong(args[1])) : new Random());
        SwingUtilities.invokeLater(() -> PrototypePlayerFrame.open(adapter));
    }
}
