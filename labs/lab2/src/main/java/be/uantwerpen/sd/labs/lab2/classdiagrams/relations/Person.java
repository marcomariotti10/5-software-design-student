package be.uantwerpen.sd.labs.lab2.classdiagrams.relations;

/**
 * Base type for people in the festival domain.
 * Stores a display name and provides common behaviour for roles such as attendees and staff.
 */
public abstract class Person {
    private final String name;

    protected Person(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
