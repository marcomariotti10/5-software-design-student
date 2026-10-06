package be.uantwerpen.sd.labs.lab3.observer;

/*
TODO: Track and print the current maximum bid.
Methods you will implement:
  - void update(String event, Object payload)
TIP: Keep an int field; update it when a higher bid arrives.
*/
public class MaxBidTracker {
    private int max = 0;

    public int getMax() {
        return max;
    }
}
