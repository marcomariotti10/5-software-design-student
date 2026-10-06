package be.uantwerpen.sd.labs.lab2.classdiagrams.relations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Top-level event with a name, a schedule (program), and a set of venues.
 * Serves as the entry point to set up and run the sample.
 */
public class Festival {
    private final String name;
    private final Program program;
    private final List<Venue> venues = new ArrayList<>();

    public Festival(String name) {
        this.name = name;
        this.program = new Program();
    }

    public String getName() {
        return name;
    }

    public Program getProgram() {
        return program;
    }

    public void registerVenue(Venue v) {
        if (v != null && !venues.contains(v)) venues.add(v);
    }

    public List<Venue> getVenues() {
        return Collections.unmodifiableList(venues);
    }
}
