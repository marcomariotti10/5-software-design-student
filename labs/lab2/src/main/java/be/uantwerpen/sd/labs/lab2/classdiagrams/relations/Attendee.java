package be.uantwerpen.sd.labs.lab2.classdiagrams.relations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a visitor who can reserve seats for screenings.
 * Keeps a history of bookings made by this person.
 */
public class Attendee extends Person {
    private final List<Booking> bookings = new ArrayList<>();

    public Attendee(String name) {
        super(name);
    }

    void addBooking(Booking b) {
        if (b != null && !bookings.contains(b)) bookings.add(b);
    }

    public List<Booking> getBookings() {
        return Collections.unmodifiableList(bookings);
    }
}
