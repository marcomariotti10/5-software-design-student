package be.uantwerpen.sd.labs.lab1;


import java.util.HashMap;
import java.util.Map;

public class Booking {
    private HashMap<Integer, Hotel> hotels;
    private int hotelCount = 0;

    public Booking() {
        this.hotels = new HashMap<>();
    }

    /**
     * Adds a given hotel to the booking system.
     * Function needs to give ID to the hotel in its HashMap.
     * It's OK to use hotel count as ID
     * -> first hotel has ID 0
     * -> second hotel has ID 1
     * -> third hotel has ID 2
     * ...
     */
    public void addHotel(Hotel h) {
        this.hotels.put(hotelCount, h);
        hotelCount++;
    }

    /**
     * Removes a given hotel in the booking system.
     *
     * @param h the hotel to be removed
     */
    public void removeHotel(Hotel h) {
        if (h == null || this.hotels.isEmpty()) {
            return;
        }
        Integer idToRemove = null;
        for (Map.Entry<Integer, Hotel> e : this.hotels.entrySet()) {
            if (java.util.Objects.equals(e.getValue(), h)) {
                idToRemove = e.getKey();
                break;
            }
        }
        if (idToRemove != null) {
            this.hotels.remove(idToRemove);
        }
    }

    /**
     * Returns the internal hotel map. Intended for read-only inspection in tests.
     */
    public Map<Integer, Hotel> getHotels() {
        return this.hotels;
    }


    /**
     * Iterates over all hotels and searches for the cheapest hotel
     * that has an available room on the given date.
     *
     * @param date date to search for room
     * @return ID of cheapest hotel, -1 for nothing available
     */
    public int findCheapestHotel(long date) {
        double cheapestPrice = Double.MAX_VALUE;
        int cheapestHotelID = -1;

        for (Map.Entry<Integer, Hotel> entry : hotels.entrySet()) {
            int e_hotelID = entry.getKey();
            Hotel e_hotel = entry.getValue();

            if (e_hotel.checkAvailability(date) != -1 &&
                    e_hotel.getPricePerRoom() < cheapestPrice) {
                cheapestHotelID = e_hotelID;
                cheapestPrice = e_hotel.getPricePerRoom();

            }
        }

        return cheapestHotelID;
    }

    /**
     * For given hotelID and date, book a room in that hotel.
     *
     * @param date    date on which room needs to be booked
     * @param hotelID hotel to book a room in
     * @return 0 for OK, -1 for error
     */
    public int bookRoomInHotel(long date, int hotelID) {
        /*
            TODO: Check availability, then book the returned room; propagate hotel.bookRoom result
            TIP: Read Hotel.java.
        */
        return -2;
    }

    /**
     * For given hotelID and date range, book a room in that hotel for every day in the inclusive range.
     *
     * @param startDate start of the date range (inclusive)
     * @param endDate   end of the date range (inclusive)
     * @param hotelID   hotel to book a room in
     * @return 0 for OK, -1 for error
     */
    public int bookRoomInHotel(long startDate, long endDate, int hotelID) {
        /*
            TODO: Check availability across [startDate, endDate], then book the returned room for the full range; propagate hotel.bookRoom result
            TIP: Read Hotel.java. Implement 'checkAvailability(long startDate, long endDate)'
        */
        return -2;
    }


    /**
     * Iterates over all hotels and books a room in the cheapest hotel that has an available room
     * for every day in the inclusive range [startDate, endDate].
     *
     * @param startDate start of the date range (inclusive)
     * @param endDate   end of the date range (inclusive)
     * @return ID of the booked hotel on success; -1 if no availability or on error
     */
    public int bookRoomInCheapestHotel(long startDate, long endDate) {
        /*
            TODO: Find the cheapest hotel with availability across [startDate, endDate]; then book the entire range in that hotel and return its ID
            TIP: Read Hotel.java. Implement 'checkAvailability(long startDate, long endDate)'
        */
        return -2;
    }
}