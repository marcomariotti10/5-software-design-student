package be.uantwerpen.sd.labs.lab3.observer;


import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
TODO: Implement Subject and notify observers when a new bid arrives.
Fields you will need:
  - Set<Observer> observers
Methods you will implement:
  - void addObserver(Observer o)
  - void removeObserver(Observer o)
  - void notifyObservers(String event, Object payload)
  - void place(Bid bid)
TIP: Pick an event name (e.g., 'bidPlaced') and send the Bid as payload.
*/
public class Auction {
    private Bid highest;

    public void place(Bid bid) {
        /*
            TODO: Accept a bid, update the highest bid when appropriate, and notify observers.
            TIP: Compare with current highest; notify with your chosen event name.
        */
        return;
    }

    public Bid highest() {
        return highest;
    }
}
