package be.uantwerpen.sd.labs.lab4b.logic;

import be.uantwerpen.sd.labs.lab4b.model.domain.Entity;

public interface CoveragePolicy {
    boolean countsForCoverage(Entity e);
}