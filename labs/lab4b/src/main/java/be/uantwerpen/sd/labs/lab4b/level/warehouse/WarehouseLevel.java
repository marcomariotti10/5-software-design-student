package be.uantwerpen.sd.labs.lab4b.level.warehouse;

import be.uantwerpen.sd.labs.lab4b.config.AppConfig;
import be.uantwerpen.sd.labs.lab4b.level.Level;
import be.uantwerpen.sd.labs.lab4b.level.Palette;

public final class WarehouseLevel extends Level {
    private static javafx.scene.paint.Color color(String hex) {
        return javafx.scene.paint.Color.web(hex);
    }

    @Override
    public Palette palette() {
        var C = AppConfig.get();
        var t = C.themeWarehouse;
        return new Palette.Builder()
                .background(color(t.get("background")))
                .floor(color(t.get("floor")))
                .wall(color(t.get("wall")))
                .target(color(t.get("target")))
                .player(color(t.get("player")))
                .box(color(t.get("box")))
                .build();
    }

}