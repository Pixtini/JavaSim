package game.expandingwild.config;

import GameModuleFramework.reels.ReelStrip;
import java.util.List;

/** Converts spreadsheet-friendly game configuration data into GMF runtime types. */
public final class ExpandingWildConfigAdapter {
    private ExpandingWildConfigAdapter() {}

    public static List<ReelStrip> toBaseReelStrips(ExpandingWildConfig config) {
        return config.baseReelStrips.stream()
                .map(ids -> new ReelStrip(ids.stream()
                        .map(ExpandingWildConfig::fromId).toList()))
                .toList();
    }
}
