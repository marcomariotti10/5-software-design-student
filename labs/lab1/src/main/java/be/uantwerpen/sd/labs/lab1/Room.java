package be.uantwerpen.sd.labs.lab1;

import java.util.HashMap;

public class Room {
    // Hashmap containing mapping between DATE and BOOKED
    private HashMap<Long, Boolean> bookSchedule;

    public Room() {
        bookSchedule = new HashMap<>();
    }

    public Boolean getBooking(long date) {
        return bookSchedule.getOrDefault(date, false);
    }

    public void setBooking(long date, boolean booking) {
        bookSchedule.put(date, booking);
    }

}
