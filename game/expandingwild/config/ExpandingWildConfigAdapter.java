package game.expandingwild.config;

import GameModuleFramework.reels.ReelStrip;
import java.util.List;

/** Converts one game's spin-set symbol-ID strips into GMF runtime reel strips. */
public final class ExpandingWildConfigAdapter {
    private ExpandingWildConfigAdapter() {}

    public static List<ReelStrip> toReelStrips(ExpandingWildConfig.SpinSetConfig setConfig) {
        return setConfig.reelStrips.stream()
                .map(ids -> new ReelStrip(ids.stream()
                        .map(ExpandingWildConfig::fromId).toList()))
                .toList();
    }
}
