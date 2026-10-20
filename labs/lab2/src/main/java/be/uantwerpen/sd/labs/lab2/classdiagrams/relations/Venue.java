package be.uantwerpen.sd.labs.lab2.classdiagrams.relations;

/**
 * Physical room or hall where screenings happen.
 * Tracks the venue’s seating capacity and which screenings take place there.
 */
public class Venue {
    private final String name;
    private final int capacity;

    public Venue(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }
}

