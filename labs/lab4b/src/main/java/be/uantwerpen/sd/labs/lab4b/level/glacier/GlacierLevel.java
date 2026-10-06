package be.uantwerpen.sd.labs.lab4b.level.glacier;

import be.uantwerpen.sd.labs.lab4b.config.AppConfig;
import be.uantwerpen.sd.labs.lab4b.level.Level;
import be.uantwerpen.sd.labs.lab4b.level.Palette;

public final class GlacierLevel extends Level {
    private static javafx.scene.paint.Color color(String hex) {
        return javafx.scene.paint.Color.web(hex);
    }

    @Override
    public Palette palette() {
        var C = AppConfig.get();
        var t = C.themeGlacier;
        return new Palette.Builder()
                .background(color(t.get("background")))
                .wall(color(t.get("wall")))
                .floor(color(t.get("floor")))
                .target(color(t.get("target")))
                .player(color(t.get("player")))
                .box(color(t.get("box")))
                .build();
    }

    @Override
    public int extraPullBias() {
        return AppConfig.get().glacierPullBonus;
    }
}