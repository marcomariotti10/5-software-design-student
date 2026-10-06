package be.uantwerpen.sd.labs.grader.lab1;

import be.uantwerpen.sd.labs.grader.core.LabGrader;
import be.uantwerpen.sd.labs.grader.core.SafeRef;
import be.uantwerpen.sd.labs.grader.core.SuiteRunner;
import be.uantwerpen.sd.labs.grader.core.TestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Lab1Grader implements LabGrader {

    private SuiteRunner runner;

    /* ============================
       Grader entrypoint
       ============================ */
    @Override
    public Result run(SuiteRunner runner) {
        this.runner = runner;
        System.out.println("\n=== Lab 1 - Auto-Grader ===\n");

        int grandTotal = 0;
        int grandPassed = 0;

        // ---- Suite 1: bookRoomInHotel(date, hotelId)
        List<Supplier<TestResult>> s1 = new ArrayList<>();
        s1.add(this::testBookRoom_singleDate_success);
        s1.add(this::testBookRoom_singleDate_overbookingFails);
        s1.add(this::testBookRoom_singleDate_unknownHotel);
        s1.add(this::testBookRoom_singleDate_invalidDate);
        s1.add(this::testBookRoom_singleDate_negativeHotelId);
        s1.add(this::testBookRoom_singleDate_noRoomsInHotel);
        int p1 = runner.runSuite("bookRoomInHotel(date, hotelId)", s1);
        grandPassed += p1;
        grandTotal += s1.size();

        // ---- Suite 2: bookRoomInHotel(start, end, hotelId)
        List<java.util.function.Supplier<TestResult>> s2 = new ArrayList<>();
        s2.add(this::testBooking_bookRoomInHotel_range_success);
        s2.add(this::testBooking_bookRoomInHotel_range_success_multiDay);
        s2.add(this::testBooking_bookRoomInHotel_range_doubleBookingFails);
        s2.add(this::testBooking_bookRoomInHotel_range_middleDayConflictFailsAtomic);
        s2.add(this::testBooking_bookRoomInHotel_range_zeroLength_success);
        s2.add(this::testBooking_bookRoomInHotel_range_invalidRange);
        s2.add(this::testBooking_bookRoomInHotel_range_negativeHotelId);
        s2.add(this::testBooking_bookRoomInHotel_range_atomicConflictFails);
        s2.add(this::testRangeAPIs_negativeDates);
        s2.add(this::testBooking_bookRoomInHotel_range_badHotel);
        int p2 = runner.runSuite("bookRoomInHotel(start, end, hotelId)", s2);
        grandPassed += p2;
        grandTotal += s2.size();

        // ---- Suite 3: bookRoomInCheapestHotel(start, end)
        List<java.util.function.Supplier<TestResult>> s3 = new ArrayList<>();
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_success);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_returnsCheapestExpected);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_tiePrice);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_skipsHotelWithMiddleGap);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_picksMoreExpensiveWhenCheapUnavailable);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_ignoresEmptyHotel);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_noHotels);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_doubleBookingFails);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_noAvailability);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_invalidRange);
        s3.add(this::testBooking_bookRoomInCheapestHotel_range_negativeDates);
        int p3 = runner.runSuite("bookRoomInCheapestHotel(start, end)", s3);
        grandPassed += p3;
        grandTotal += s3.size();

        // Final summary
        System.out.printf("\n=== Overall Summary Lab 1: %d/%d tests passed ===\n", grandPassed, grandTotal);
        if (grandPassed == grandTotal) {
            System.out.println("All suites green. 🎉");
        } else {
            System.out.println("Check the suite summaries above to see which method to focus on next.");
        }

        return new Result(grandPassed, grandTotal);
    }

    /* ============================
       Test fixtures
       ============================ */
    private Fixture baselineFixture() {
        SafeRef R = new SafeRef();
        Class<?> Hotel = R.cls("lab1", "Hotel");
        Class<?> Booking = R.cls("lab1", "Booking");

        Object booking = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});
        if (booking == null) {
            return new Fixture(null, new Object[]{null, null, null}, R);
        }

        // Helper lambdas using reflection
        java.util.function.Function<Double, Object> newHotel = (price) ->
                R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{price});
        java.util.function.BiConsumer<Object, Integer> bookSingle = (h, dayRoom) -> {
            long day = (dayRoom >> 16);
            int room = (dayRoom & 0xFFFF);
            R.call(h, "bookRoom", new Class<?>[]{long.class, int.class}, new Object[]{day, room});
        };

        Object h0 = newHotel.apply(30.0);
        R.call(h0, "addRoom", new Class<?>[]{}, new Object[]{}); // id 0
        R.call(h0, "addRoom", new Class<?>[]{}, new Object[]{}); // id 1
        R.call(h0, "addRoom", new Class<?>[]{}, new Object[]{}); // id 2
        bookSingle.accept(h0, (10 << 16) | 0);
        bookSingle.accept(h0, (10 << 16) | 1);
        bookSingle.accept(h0, (10 << 16) | 2);
        bookSingle.accept(h0, (11 << 16) | 0);
        bookSingle.accept(h0, (11 << 16) | 1);
        bookSingle.accept(h0, (11 << 16) | 2);
        bookSingle.accept(h0, (12 << 16) | 2);

        Object h1 = newHotel.apply(46.99);
        R.call(h1, "addRoom", new Class<?>[]{}, new Object[]{}); // id 0
        R.call(h1, "addRoom", new Class<?>[]{}, new Object[]{}); // id 1
        bookSingle.accept(h1, (10 << 16) | 0);
        bookSingle.accept(h1, (10 << 16) | 1);
        bookSingle.accept(h1, (11 << 16) | 0);
        bookSingle.accept(h1, (12 << 16) | 1);

        Object h2 = newHotel.apply(40.0);
        R.call(h2, "addRoom", new Class<?>[]{}, new Object[]{}); // id 0
        bookSingle.accept(h2, (11 << 16) | 0);

        // booking.addHotel(...)
        R.call(booking, "addHotel", new Class<?>[]{Hotel}, new Object[]{h0});
        R.call(booking, "addHotel", new Class<?>[]{Hotel}, new Object[]{h1});
        R.call(booking, "addHotel", new Class<?>[]{Hotel}, new Object[]{h2});

        return new Fixture(booking, new Object[]{h0, h1, h2}, R);
    }

    private Fixture emptyFixture() {
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Object booking = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});
        return new Fixture(booking, new Object[]{}, R);
    }

    // New fixtures
    private Fixture fixtureWithEmptyHotel() {
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Class<?> Hotel = R.cls("lab1", "Hotel");
        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});
        Object hEmpty = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{5.0});
        // Intentionally do NOT add any rooms to hEmpty
        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{hEmpty});
        return new Fixture(b, new Object[]{hEmpty}, R);
    }

    private Fixture tiePriceFixture() {
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Class<?> Hotel = R.cls("lab1", "Hotel");

        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});
        Object hA = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{20.0});
        Object hB = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{20.0});

        R.call(hA, "addRoom", new Class<?>[]{}, new Object[]{});
        R.call(hB, "addRoom", new Class<?>[]{}, new Object[]{});

        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{hA}); // id 0
        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{hB}); // id 1

        return new Fixture(b, new Object[]{hA, hB}, R);
    }

    private Fixture fixtureRangeFreeHotel() {
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Class<?> Hotel = R.cls("lab1", "Hotel");

        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});
        Object h = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{10.0});
        R.call(h, "addRoom", new Class<?>[]{}, new Object[]{});
        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{h}); // id 0
        return new Fixture(b, new Object[]{h}, R);
    }

    // Cheap hotel exists but is unavailable for part of the range; a more expensive one is free.
    private Fixture fixtureCheapestSkipUnavailable() {
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Class<?> Hotel = R.cls("lab1", "Hotel");
        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});

        Object cheap = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{5.0});  // id 0
        R.call(cheap, "addRoom", new Class<?>[]{}, new Object[]{});
        R.call(cheap, "bookRoom", new Class<?>[]{long.class, int.class}, new Object[]{6L, 0}); // unavailable on 6

        Object expensive = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{7.0}); // id 1
        R.call(expensive, "addRoom", new Class<?>[]{}, new Object[]{});

        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{cheap});
        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{expensive});
        return new Fixture(b, new Object[]{cheap, expensive}, R);
    }

    // Cheapest price hotel has zero rooms; should be ignored in selection.
    private Fixture fixtureCheapestWithEmptyHotel() {
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Class<?> Hotel = R.cls("lab1", "Hotel");
        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});

        Object empty = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{1.0}); // id 0, no rooms
        Object real = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{2.0}); // id 1
        R.call(real, "addRoom", new Class<?>[]{}, new Object[]{});

        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{empty});
        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{real});
        return new Fixture(b, new Object[]{empty, real}, R);
    }

    // Hotel with a single middle-day conflict inside a larger free range.
    private Fixture fixtureRangeMiddleGap() {
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Class<?> Hotel = R.cls("lab1", "Hotel");
        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});

        Object h = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{10.0}); // id 0
        R.call(h, "addRoom", new Class<?>[]{}, new Object[]{});
        // Make middle day (e.g., 8) unavailable; ends 5 and 10 remain free.
        R.call(h, "bookRoom", new Class<?>[]{long.class, int.class}, new Object[]{8L, 0});

        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{h});
        return new Fixture(b, new Object[]{h}, R);
    }

    // Two hotels: cheap has a middle-day conflict; expensive is fully free for the range.
    private Fixture fixtureCheapestSkipMiddleGap() {
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Class<?> Hotel = R.cls("lab1", "Hotel");
        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});

        Object cheap = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{5.0});  // id 0
        R.call(cheap, "addRoom", new Class<?>[]{}, new Object[]{});
        R.call(cheap, "bookRoom", new Class<?>[]{long.class, int.class}, new Object[]{8L, 0}); // middle-day conflict

        Object expensive = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{7.0}); // id 1
        R.call(expensive, "addRoom", new Class<?>[]{}, new Object[]{});

        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{cheap});
        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{expensive});
        return new Fixture(b, new Object[]{cheap, expensive}, R);
    }

    private TestResult testBookRoom_singleDate_success() {
        String title = "Single-day booking succeeds when a room is free";
        Fixture f = baselineFixture();
        // In the baseline fixture, hotel with ID 2 has a free room on day 10.
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, int.class}, new Object[]{10L, 2}, -1);
        return runner.assertEquals(title, 0, rc,
                runner.withReflection("Expected success when the date is valid, the hotel exists, and at least one room is free.", f.R));
    }

    /* ============================
       Tests – Single date
       ============================ */

    private TestResult testBookRoom_singleDate_overbookingFails() {
        String title = "Prevent double-booking on the same date in the same hotel";
        Fixture f = baselineFixture();
        long date = 10L;
        int rc1 = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, int.class}, new Object[]{date, 2}, -1);
        int rc2 = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, int.class}, new Object[]{date, 2}, -1);
        boolean pass = (rc1 == 0) && (rc2 == -1);
        return new TestResult(title, pass,
                pass ? "" : runner.withReflection("A second identical booking must fail after capacity is taken.", f.R),
                pass ? "" : ("first=" + rc1 + ", second=" + rc2));
    }

    private TestResult testBookRoom_singleDate_unknownHotel() {
        String title = "Reject bookings for unknown hotel IDs";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1 : f.R.callInt(f.booking, "bookRoomInHotel", new Class<?>[]{long.class, int.class}, new Object[]{10L, 999}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Unknown hotel ID: return failure without attempting any booking.", f.R));
    }

    private TestResult testBookRoom_singleDate_invalidDate() {
        String title = "Reject negative dates for single-day bookings";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1 : f.R.callInt(f.booking, "bookRoomInHotel", new Class<?>[]{long.class, int.class}, new Object[]{-5L, 0}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Dates must be non-negative.", f.R));
    }

    // Single-date: hotel exists but has no rooms -> should fail gracefully
    private TestResult testBookRoom_singleDate_noRoomsInHotel() {
        String title = "Reject booking in a hotel with zero rooms";
        Fixture f = fixtureWithEmptyHotel();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, int.class}, new Object[]{10L, 0}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Hotels with zero rooms should be treated as unavailable.", f.R));
    }

    private TestResult testBookRoom_singleDate_negativeHotelId() {
        String title = "Reject negative hotel IDs";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, int.class}, new Object[]{10L, -1}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Hotel ID must be non-negative and valid.", f.R));
    }

    /* ============================
       Tests – Range via Booking (Student tasks)
       ============================ */
    private TestResult testBooking_bookRoomInHotel_range_success() {
        String title = "Range booking succeeds when every day is available";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{10L, 10L, 2}, -1);
        return runner.assertEquals(title, 0, rc,
                runner.withReflection("Expected success when all days in [start..end] are available in the specified hotel.", f.R));
    }

    private TestResult testBooking_bookRoomInHotel_range_success_multiDay() {
        String title = "Multi-day range booking succeeds when all days are free";
        Fixture f = fixtureRangeFreeHotel();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{5L, 7L, 0}, -1);
        return runner.assertEquals(title, 0, rc,
                runner.withReflection("Expected success for a multi-day continuous range with available capacity.", f.R));
    }

    private TestResult testBooking_bookRoomInHotel_range_doubleBookingFails() {
        String title = "Second identical range booking must fail (capacity taken)";
        Fixture f = fixtureRangeFreeHotel();
        int first = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{5L, 7L, 0}, -1);
        int second = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{5L, 7L, 0}, -1);
        boolean pass = (first == 0 && second == -1);
        return new TestResult(title, pass,
                pass ? "" : runner.shortHint(runner.withReflection("After a successful range booking, an identical request must fail because capacity is reserved.", f.R)),
                pass ? "" : ("first=" + first + ", second=" + second));
    }

    // Range: first and last dates are free, but a middle date is not -> must fail atomically
    private TestResult testBooking_bookRoomInHotel_range_middleDayConflictFailsAtomic() {
        String title = "Any unavailable day makes the whole range fail (atomicity)";
        Fixture f = fixtureRangeMiddleGap();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{5L, 10L, 0}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Validate every day in [start..end], not just the endpoints.", f.R));
    }

    private TestResult testBooking_bookRoomInHotel_range_negativeHotelId() {
        String title = "Negative hotel id not available";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{10L, 10L, -1}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Hotel ID must be non-negative and valid.", f.R));
    }

    private TestResult testBooking_bookRoomInHotel_range_badHotel() {
        String title = "Bad hotel id not available";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1 : f.R.callInt(f.booking, "bookRoomInHotel", new Class<?>[]{long.class, long.class, int.class}, new Object[]{10L, 12L, 999}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Unknown hotel ID: return failure without attempting any booking.", f.R));
    }

    // 2) Range: zero-length range should behave like single day
    private TestResult testBooking_bookRoomInHotel_range_zeroLength_success() {
        String title = "Zero length booking succeeds";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{10L, 10L, 2}, -1);
        return runner.assertEquals(title, 0, rc,
                runner.withReflection("Treat start == end as a single-day booking when capacity exists.", f.R));
    }

    // 3) Range: invalid (start > end)
    private TestResult testBooking_bookRoomInHotel_range_invalidRange() {
        String title = "Reject ranges where start > end";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{12L, 10L, 2}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Start must be <= end.", f.R));
    }

    // 4) Range: atomic conflict must fail (no partial booking)
    private TestResult testBooking_bookRoomInHotel_range_atomicConflictFails() {
        String title = "Range with conflicts must fail atomically";
        Fixture f = baselineFixture();
        // Hotel with ID 1 has conflicts across 10..12 in the baseline fixture
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInHotel",
                new Class<?>[]{long.class, long.class, int.class}, new Object[]{10L, 12L, 1}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Any conflict on any day should fail; do not partially reserve.", f.R));
    }

    private TestResult testBooking_bookRoomInCheapestHotel_range_success() {
        String title = "Returns a valid hotel that can host the range (smoke test)";
        Fixture f = baselineFixture();
        int hotelID = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{10L, 10L}, -1);

        boolean idValid = (hotelID >= 0) && (hotelID < (f.hotels == null ? 0 : f.hotels.length));

        int rcCanHost = -2;
        if (idValid) {
            Fixture fCheck = baselineFixture();
            rcCanHost = (fCheck.booking == null) ? -1
                    : fCheck.R.callInt(fCheck.booking, "bookRoomInHotel",
                    new Class<?>[]{long.class, long.class, int.class}, new Object[]{10L, 10L, hotelID}, -1);
        }

        boolean pass = idValid && (rcCanHost == 0);
        String detail = pass ? "" : ("hotelID=" + hotelID + ", idValid=" + idValid + ", canHost=" + rcCanHost);

        return new TestResult(title, pass,
                pass ? "" : runner.shortHint(runner.withReflection("Return a valid registered hotel ID such that every day in [start..end] can be booked in that hotel. Cheapest selection is tested separately.", f.R)),
                detail);
    }

    // 5) Cheapest (range): returns the expected cheapest hotel (baseline expects ID 2)
    private TestResult testBooking_bookRoomInCheapestHotel_range_returnsCheapestExpected() {
        String title = "Returns the expected cheapest suitable hotel ID";
        Fixture f = baselineFixture();
        int hotelID = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{10L, 10L}, -1);
        return runner.assertEquals(title, 2, hotelID,
                runner.withReflection("Compare prices only among hotels that can host the entire range.", f.R));
    }

    private TestResult testBooking_bookRoomInCheapestHotel_range_noAvailability() {
        String title = "No hotel can host the full range -> return failure";
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Class<?> Hotel = R.cls("lab1", "Hotel");
        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});
        Object hA = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{10.0});
        R.call(hA, "addRoom", new Class<?>[]{}, new Object[]{});
        R.call(hA, "bookRoom", new Class<?>[]{long.class, int.class}, new Object[]{1L, 0});
        Object hB = R.newInstance(Hotel, new Class<?>[]{double.class}, new Object[]{9.0});
        R.call(hB, "addRoom", new Class<?>[]{}, new Object[]{});
        R.call(hB, "bookRoom", new Class<?>[]{long.class, int.class}, new Object[]{2L, 0});
        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{hA});
        R.call(b, "addHotel", new Class<?>[]{Hotel}, new Object[]{hB});
        int hotelID = (b == null) ? -1 : R.callInt(b, "bookRoomInCheapestHotel", new Class<?>[]{long.class, long.class}, new Object[]{1L, 2L}, -1);
        return runner.assertEquals(title, -1, hotelID,
                runner.withReflection("Return failure when no hotel covers every day in the requested range.", R));
    }

    // Cheapest: cheap hotel has a middle-day conflict inside the requested range -> pick the next valid hotel
    private TestResult testBooking_bookRoomInCheapestHotel_range_skipsHotelWithMiddleGap() {
        String title = "Skip a cheap hotel if it has a middle-day gap";
        Fixture f = fixtureCheapestSkipMiddleGap();
        int hotelID = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{5L, 10L}, -1);
        return runner.assertEquals(title, 1, hotelID,
                runner.withReflection("If the cheapest is unavailable for any day, select the next cheapest that can host the entire range.", f.R));
    }

    // 6) Cheapest (range): tie on price -> any valid candidate (id 0 or 1)
    private TestResult testBooking_bookRoomInCheapestHotel_range_tiePrice() {
        String title = "Price tie: returns any valid cheapest candidate";
        Fixture f = tiePriceFixture();
        int hotelID = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{5L, 6L}, -1);
        boolean pass = (hotelID == 0 || hotelID == 1);
        return new TestResult(title, pass,
                pass ? "" : runner.withReflection("When multiple hotels share the minimal price and are available, returning any one of them is acceptable.", f.R),
                pass ? "" : ("expected one of {0,1}, actual=" + hotelID));
    }

    private TestResult testBooking_bookRoomInCheapestHotel_range_picksMoreExpensiveWhenCheapUnavailable() {
        String title = "Pick more expensive only when cheaper cannot host";
        Fixture f = fixtureCheapestSkipUnavailable();
        int hotelID = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{5L, 6L}, -1);
        return runner.assertEquals(title, 1, hotelID,
                runner.withReflection("Select the next cheapest hotel that can host the full range when the cheapest cannot.", f.R));
    }

    private TestResult testBooking_bookRoomInCheapestHotel_range_ignoresEmptyHotel() {
        String title = "Ignore zero-room hotels when choosing the cheapest";
        Fixture f = fixtureCheapestWithEmptyHotel();
        int hotelID = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{5L, 5L}, -1);
        return runner.assertEquals(title, 1, hotelID,
                runner.withReflection("Hotels with zero rooms are ineligible candidates.", f.R));
    }

    private TestResult testBooking_bookRoomInCheapestHotel_range_noHotels() {
        String title = "No hotels registered -> return failure";
        Fixture f = emptyFixture();
        int hotelID = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{5L, 5L}, -1);
        return runner.assertEquals(title, -1, hotelID,
                runner.withReflection("When there are no hotels, return failure.", f.R));
    }

    // 7) Cheapest (range): booking actually reserves capacity (second attempt fails)
    private TestResult testBooking_bookRoomInCheapestHotel_range_doubleBookingFails() {
        String title = "Booking reserves capacity: identical repeat should fail";
        Fixture f = baselineFixture();
        int first = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{10L, 10L}, -1);
        int second = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{10L, 10L}, -1);
        boolean pass = (first != -1) && (second == -1);
        return new TestResult(title, pass,
                pass ? "" : runner.shortHint(runner.withReflection("After a successful booking, an identical request must fail because capacity is already reserved.", f.R)),
                pass ? "" : ("first=" + first + ", second=" + second));
    }

    // 8) Cheapest (range): invalid (start > end)
    private TestResult testBooking_bookRoomInCheapestHotel_range_invalidRange() {
        String title = "Reject ranges where start > end (cheapest)";
        Fixture f = baselineFixture();
        int rc = (f.booking == null) ? -1
                : f.R.callInt(f.booking, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{12L, 10L}, -1);
        return runner.assertEquals(title, -1, rc,
                runner.withReflection("Start must be <= end.", f.R));
    }

    // 9) Cheapest (range): negative dates rejected
    private TestResult testBooking_bookRoomInCheapestHotel_range_negativeDates() {
        String title = "Reject negative dates (cheapest)";
        SafeRef R = new SafeRef();
        Class<?> Booking = R.cls("lab1", "Booking");
        Object b = R.newInstance(Booking, new Class<?>[]{}, new Object[]{});
        int rc1 = (b == null) ? -1 : R.callInt(b, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{-1L, 1L}, -1);
        int rc2 = (b == null) ? -1 : R.callInt(b, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{1L, -1L}, -1);
        int rc3 = (b == null) ? -1 : R.callInt(b, "bookRoomInCheapestHotel",
                new Class<?>[]{long.class, long.class}, new Object[]{-2L, -1L}, -1);
        boolean pass = (rc1 == -1 && rc2 == -1 && rc3 == -1);
        return new TestResult(title, pass,
                pass ? "" : runner.shortHint(runner.withReflection("Start and end must be non-negative.", R)),
                pass ? "" : ("expected: rc1=-1, rc2= -1, rc3= -1, actual: " + "rc1=" + rc1 + ", rc2=" + rc2 + ", rc3=" + rc3));
    }

    /* ============================
       Tests – Misc/edge
       ============================ */
    private TestResult testRangeAPIs_negativeDates() {
        String title = "Reject negative dates in range APIs";
        Fixture f = baselineFixture();
        int rc1 = (f.booking == null) ? -1 : f.R.callInt(f.booking, "bookRoomInHotel", new Class<?>[]{long.class, long.class, int.class}, new Object[]{-1L, 1L, 0}, -1);
        int rc2 = (f.booking == null) ? -1 : f.R.callInt(f.booking, "bookRoomInHotel", new Class<?>[]{long.class, long.class, int.class}, new Object[]{1L, -1L, 0}, -1);
        int rc3 = (f.booking == null) ? -1 : f.R.callInt(f.booking, "bookRoomInHotel", new Class<?>[]{long.class, long.class, int.class}, new Object[]{-2L, -1L, 0}, -1);
        boolean pass = (rc1 == -1 && rc2 == -1 && rc3 == -1);
        return new TestResult(title, pass,
                pass ? "" : runner.shortHint(runner.withReflection("Ranges with negative dates should not succeed.", f.R)),
                pass ? "" : ("expected: rc1=-1, rc2= -1, rc3= -1, actual: " + "rc1=" + rc1 + ", rc2=" + rc2 + ", rc3=" + rc3));
    }

    /**
     * Simple holder for a prepared booking fixture.
     */
    private static final class Fixture {
        final Object booking;            // instance of students' Booking (or null)
        final Object[] hotels;           // instances of students' Hotel (or nulls)
        final SafeRef R;

        Fixture(Object booking, Object[] hotels, SafeRef R) {
            this.booking = booking;
            this.hotels = hotels;
            this.R = R;
        }
    }

}