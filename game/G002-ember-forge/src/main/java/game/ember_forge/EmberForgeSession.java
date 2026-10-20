package game.ember_forge;

import game.GameSession;
import game.ember_forge.basegame.EmberForgeBaseGame;
import game.ember_forge.features.EmberForgeFreeGame;
import game.ember_forge.config.EmberForgeConfig;
import game.ember_forge.model.EmberForgeSpin;
import game.model.*;
import java.util.*;

/** Orchestrates and replays G002 rounds using only the session's supplied random stream. */
public final class EmberForgeSession implements GameSession {
    private final EmberForgeConfig config;
    private final Random random;
    private final EmberForgeBaseGame base;
    private final EmberForgeFreeGame free;
    public EmberForgeSession(EmberForgeConfig config, Random random) {
        this.config = config; this.random = Objects.requireNonNull(random);
        base = new EmberForgeBaseGame(config); free = new EmberForgeFreeGame(config);
    }
    @Override public GameRoundResult playRound() { return playRound(1); }
    @Override public GameRoundResult playRound(double stake) {
        return playRound(stake, stake * config.maxWinMultiplier());
    }
    @Override public GameRoundResult playRound(double stake, double cap) { return playRound(stake, cap, false); }
    @Override public GameRoundResult playRound(double stake, double cap, boolean capture) {
        check(stake, cap);
        cap = Math.min(cap, stake * config.maxWinMultiplier());
        EmberForgeSpin bg = base.spin(random, stake, capture).cappedAt(cap);
        List<SpinResult> spins = new ArrayList<>();
        double total = bg.getWin();
        int heat = 1;
        for (int i = 0; bg.hasFreeSpin() && i < config.freeSpins() && total < cap; i++) {
            var spin = free.spin(random, stake, heat, capture).cappedAt(Math.max(0, cap - total));
            spins.add(spin); total += spin.getWin(); heat = spin.nextHeat();
        }
        return new GameRoundResult(bg, spins, total >= cap).cappedAt(cap);
    }
    @Override public GameRoundResult replayRound(double stake, double cap, List<ReplayEvent> events) {
        check(stake, cap);
        cap = Math.min(cap, stake * config.maxWinMultiplier());
        if (events == null || events.isEmpty() || !events.get(0).type().equals("emberforge-base"))
            throw new IllegalArgumentException("G002 replay must start with Basegame");
        int[] bgData = decode(events.get(0));
        if (config.mode().equals("BB1") && bgData[0] != 1)
            throw new IllegalArgumentException("BB1 replay must enter the Freegame");
        var bg = base.evaluate(bgData[0], Arrays.copyOfRange(bgData, 1, 6), stake, true).cappedAt(cap);
        int expected = bg.hasFreeSpin() ? config.freeSpins() : 0;
        if (events.size() > expected + 1) throw new IllegalArgumentException("Too many G002 replay spins");
        List<SpinResult> spins = new ArrayList<>();
        double total = bg.getWin(); int heat = 1;
        for (int i = 1; i < events.size(); i++) {
            var event = events.get(i);
            if (total >= cap || !event.type().equals("emberforge-free"))
                throw new IllegalArgumentException("Invalid G002 feature event sequence");
            int[] data = decode(event);
            if (data[0] != 0) throw new IllegalArgumentException("G002 has only Freegame set zero");
            var spin = free.evaluate(Arrays.copyOfRange(data, 1, 6), stake, heat, true)
                    .cappedAt(Math.max(0, cap - total));
            spins.add(spin); total += spin.getWin(); heat = spin.nextHeat();
        }
        if (spins.size() != expected && total < cap)
            throw new IllegalArgumentException("Incomplete G002 Freegame replay");
        return new GameRoundResult(bg, spins, total >= cap).cappedAt(cap);
    }
    private static int[] decode(ReplayEvent event) {
        String[] fields = event.payload().split(";", -1);
        if (fields.length != 3 || !fields[0].equals("v1"))
            throw new IllegalArgumentException("Unsupported G002 replay payload");
        String[] stops = fields[2].split(",", -1);
        if (stops.length != 5) throw new IllegalArgumentException("G002 replay needs five stops");
        int[] data = new int[6]; data[0] = Integer.parseInt(fields[1]);
        for (int i = 0; i < 5; i++) data[i + 1] = Integer.parseInt(stops[i]);
        return data;
    }
    private static void check(double stake, double cap) {
        if (!Double.isFinite(stake) || stake <= 0 || !Double.isFinite(cap) || cap <= 0)
            throw new IllegalArgumentException("G002 stake and cap must be positive and finite");
    }
}
