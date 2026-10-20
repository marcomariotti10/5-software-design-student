package be.uantwerpen.sd.labs.lab2.classdiagrams.relations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Records that an attendee reserved a specific seat for a screening,
 * together with the price and the timestamp of the reservation.
 */
public class Booking {
    private final Attendee attendee;
    private final Screening screening;
    private final String seatLabel;
    private final BigDecimal price;
    private final LocalDateTime bookedAt;

    Booking(Attendee attendee, Screening screening, String seatLabel, BigDecimal price, LocalDateTime bookedAt) {
        this.attendee = Objects.requireNonNull(attendee);
        this.screening = Objects.requireNonNull(screening);
        this.seatLabel = Objects.requireNonNull(seatLabel);
        this.price = Objects.requireNonNull(price);
        this.bookedAt = Objects.requireNonNull(bookedAt);
    }

    public Attendee getAttendee() {
        return attendee;
    }

    public Screening getScreening() {
        return screening;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }
}
