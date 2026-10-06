package be.uantwerpen.sd.labs.lab4b.logic.warehouse;

import be.uantwerpen.sd.labs.lab4b.logic.CoveragePolicy;
import be.uantwerpen.sd.labs.lab4b.model.domain.Entity;

public final class WarehouseCoveragePolicy implements CoveragePolicy {
    @Override
    public boolean countsForCoverage(Entity e) {
        return e != null && e.isBox();
    }
}
