package be.uantwerpen.sd.labs.lab2.classdiagrams.relations;

/**
 * A piece of cinema content that can be shown (e.g., feature or short).
 * Holds immutable descriptive data like title and runtime.
 */
public abstract class Work {
    private final String title;
    private final int runtimeMinutes;

    protected Work(String title, int runtimeMinutes) {
        this.title = title;
        this.runtimeMinutes = runtimeMinutes;
    }

    public String getTitle() {
        return title;
    }

    public int getRuntimeMinutes() {
        return runtimeMinutes;
    }
}
