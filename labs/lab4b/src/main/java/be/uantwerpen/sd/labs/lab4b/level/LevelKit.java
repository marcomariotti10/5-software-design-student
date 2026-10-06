package be.uantwerpen.sd.labs.lab4b.level;

import be.uantwerpen.sd.labs.lab4b.gen.WorldGenerator;
import be.uantwerpen.sd.labs.lab4b.logic.CoveragePolicy;
import be.uantwerpen.sd.labs.lab4b.logic.MovementStrategy;
import be.uantwerpen.sd.labs.lab4b.model.domain.Box;
import be.uantwerpen.sd.labs.lab4b.model.domain.GroundTile;
import be.uantwerpen.sd.labs.lab4b.model.domain.Player;

public abstract class LevelKit {

    public abstract MovementStrategy movement();

    public abstract WorldGenerator generator();

    public abstract RendererHints hints();

    public abstract Level level();

    public abstract GroundTile floor();

    public abstract GroundTile wall();

    public abstract GroundTile target();

    public abstract Box box();

    public abstract Player player();

    public String name() {
        return getClass().getSimpleName().replace("Kit", "");
    }

    public abstract CoveragePolicy coverage();

    public interface RendererHints {
        boolean showSelectionOverlay();
    }
}
