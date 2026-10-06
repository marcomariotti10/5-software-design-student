package be.uantwerpen.sd.labs.lab4b.level.glacier;

import be.uantwerpen.sd.labs.lab4b.gen.GlacierWorldGenerator;
import be.uantwerpen.sd.labs.lab4b.gen.WorldGenerator;
import be.uantwerpen.sd.labs.lab4b.level.Level;
import be.uantwerpen.sd.labs.lab4b.level.LevelKit;
import be.uantwerpen.sd.labs.lab4b.logic.CoveragePolicy;
import be.uantwerpen.sd.labs.lab4b.logic.MovementStrategy;
import be.uantwerpen.sd.labs.lab4b.logic.glacier.GlacierCoveragePolicy;
import be.uantwerpen.sd.labs.lab4b.logic.glacier.GlacierMovementStrategy;
import be.uantwerpen.sd.labs.lab4b.model.domain.Box;
import be.uantwerpen.sd.labs.lab4b.model.domain.GroundTile;
import be.uantwerpen.sd.labs.lab4b.model.domain.Player;
import be.uantwerpen.sd.labs.lab4b.model.domain.glacier.*;
public final class GlacierKit extends LevelKit {

    private final RendererHints h = () -> true;
    /*
		TODO: Provide the correct MovementStrategy.
	*/
    private MovementStrategy m = null;
    
    /*
		TODO: Provide the correct world generator.
	*/
    private WorldGenerator g = null;
    
    /*
		TODO: Provide the correct level instance.
	*/
    private Level level = null;
    
    private GlacierKit() {
    }

    public static LevelKit createLevelKit() {
        return Holder.INSTANCE;
    }

    public MovementStrategy movement() {
        /*
            TODO: Return the correct MovementStrategy.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    public WorldGenerator generator() {
        /*
            TODO: Return the correct WorldGenerator.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    @Override
    public RendererHints hints() {
        return h;
    }

    public Level level() {
        /*
            TODO: Return the correct Level.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    public GroundTile floor() {
        /*
            TODO: Return the correct Floor.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    public GroundTile wall() {
        /*
            TODO: Return the correct Wall.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    public GroundTile target() {
        /*
            TODO: Return the correct Target.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    public Box box() {
        /*
            TODO: Return the correct Box.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    public Player player() {
        /*
            TODO: Return the correct Player.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    public CoveragePolicy coverage() {
        /*
            TODO: Return the correct CoveragePolicy.
            TIP: Ensure that this abstract factory creates Glacier Objects.
        */
        return null;
    }

    private static final class Holder {
        static final LevelKit INSTANCE = new GlacierKit();
    }

}
