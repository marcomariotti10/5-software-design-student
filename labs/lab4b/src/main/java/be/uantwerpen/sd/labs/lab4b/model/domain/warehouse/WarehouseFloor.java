package be.uantwerpen.sd.labs.lab4b.model.domain.warehouse;

import be.uantwerpen.sd.labs.lab4b.level.Palette;
import be.uantwerpen.sd.labs.lab4b.model.domain.GroundTile;
import javafx.scene.canvas.GraphicsContext;

public final class WarehouseFloor extends GroundTile {
    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean isSlippery() {
        return false;
    }

    @Override
    public boolean isTarget() {
        return false;
    }

    @Override
    public void render(GraphicsContext g, Palette p, int x, int y, int s) {
        g.setFill(p.getFloor());
        g.fillRect(x, y, s, s);
    }
}
