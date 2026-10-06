package be.uantwerpen.sd.labs.grader.lab3;

import be.uantwerpen.sd.labs.grader.core.LabGrader;
import be.uantwerpen.sd.labs.grader.core.SourceChecks;
import be.uantwerpen.sd.labs.grader.core.SuiteRunner;
import be.uantwerpen.sd.labs.grader.core.TestResult;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

public final class Lab3GraderObserver implements LabGrader {

    // We will choose ONE root and resolve all types consistently.
    private static final String ROOT_LABS = "be.uantwerpen.sd.labs";
    private static final String ROOT_SOLN = "be.uantwerpen.sd.solutions";
    private static final String SUBPKG = "lab3.observer";
    private SuiteRunner runner;

    private static Class<?> tryLoad(String fqn) {
        try {
            return Class.forName(fqn);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static Class<?> loadOrNull(String root, String simple) {
        return tryLoad(root + "." + SUBPKG + "." + simple);
    }

    /* ============================  Root Resolution  ============================ */

    /**
     * Choose one root: prefer solutions if Auction exists there; otherwise labs.
     */
    private static String chooseRoot() {
        if (loadOrNull(ROOT_LABS, "Auction") != null) return ROOT_LABS;
        if (loadOrNull(ROOT_SOLN, "Auction") != null) return ROOT_SOLN;
        // fallback to labs for error messaging
        return ROOT_LABS;
    }

    /**
     * Build a consistent type context from a single root; also try to load the alternate Observer.
     */
    private static K k() {
        String root = chooseRoot();
        Class<?> subj = loadOrNull(root, "Subject");
        Class<?> obs = loadOrNull(root, "Observer");
        Class<?> auc = loadOrNull(root, "Auction");
        Class<?> bid = loadOrNull(root, "Bid");
        Class<?> console = loadOrNull(root, "ConsoleAnnouncer");
        Class<?> tracker = loadOrNull(root, "MaxBidTracker");
        Class<?> main = loadOrNull(root, "Main");
        String alt = root.equals(ROOT_SOLN) ? ROOT_LABS : ROOT_SOLN;
        Class<?> altObs = loadOrNull(alt, "Observer");
        return new K(root, subj, obs, auc, bid, altObs, console, tracker, main);
    }

    private static boolean isPublicInterface(Class<?> c) {
        return c != null && c.isInterface() && Modifier.isPublic(c.getModifiers());
    }

    private static boolean isPublicClass(Class<?> c) {
        return c != null && !c.isInterface() && Modifier.isPublic(c.getModifiers());
    }

    /* ============================  Helpers  ============================ */

    private static Method getDeclaredMethod(Class<?> c, String name, Class<?>... sig) {
        if (c == null) return null;
        try {
            Method m = c.getDeclaredMethod(name, sig);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean pubVoid(Method m) {
        return m != null && Modifier.isPublic(m.getModifiers()) && m.getReturnType() == void.class;
    }

    private static boolean pubNoArg(Method m, Class<?> ret) {
        return m != null && Modifier.isPublic(m.getModifiers()) && m.getReturnType() == ret && m.getParameterCount() == 0;
    }

    /**
     * Find a public single-arg method by name; return it even if the parameter is the "other" Observer type.
     */
    private static Method findOneArgMethod(Class<?> type, String name) {
        if (type == null) return null;
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 1) {
                return m;
            }
        }
        // also scan declared (non-public) to give better error messages
        for (Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    /**
     * Find a public single-arg method by name; return it even if the parameter is the "other" Observer type.
     */
    private static Method findTwoArgMethod(Class<?> type, String name) {
        if (type == null) return null;
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 2) {
                return m;
            }
        }
        // also scan declared (non-public) to give better error messages
        for (Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 2) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static Field getDeclaredFieldOrNull(Class<?> c, String name) {
        try {
            Field f = c.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isPrivate(Field f) {
        return f != null && Modifier.isPrivate(f.getModifiers());
    }

    private static Field findPrivateFieldAssignableTo(Class<?> c, Class<?> rawType, String nameHintLC) {
        for (Field f : c.getDeclaredFields()) {
            f.setAccessible(true);
            boolean matchType = rawType.isAssignableFrom(f.getType());
            boolean matchName = (nameHintLC == null) || f.getName().toLowerCase(Locale.ROOT).contains(nameHintLC);
            if (matchType && matchName && isPrivate(f)) return f;
        }
        return null;
    }

    private static boolean classImplements(Class<?> clazz, Class<?> iface) {
        if (clazz == null || iface == null) return false;
        for (Class<?> i : clazz.getInterfaces()) if (i == iface) return true;
        return false;
    }

    @Override
    public Result run(SuiteRunner runner) {
        this.runner = runner;

        int total = 0, passed = 0;

        // ---------- Structure ----------
        List<Supplier<TestResult>> s1 = new ArrayList<>();
        s1.add(this::test_structure_all_required_types_exist);
        s1.add(this::test_structure_auction_implements_subject_AND_exposes_methods);
        s1.add(this::test_structure_observer_update_exact_signature);
        s1.add(this::test_structure_concrete_observers_implement_Observer);
        passed += runner.runSuite("Structure (types & contracts)", s1);
        total += s1.size();

        // ---------- API ----------
        List<Supplier<TestResult>> s2 = new ArrayList<>();
        s2.add(this::test_api_auction_methods_signatures);
        s2.add(this::test_api_subject_contract_signatures);
        s2.add(this::test_api_auction_required_fields_modifiers);
        s2.add(this::test_api_consoleAnnouncer_signatures);
        s2.add(this::test_api_maxBidTracker_signatures_and_override);
        s2.add(this::test_api_auction_override_annotations);
        s2.add(this::test_api_observers_override_annotations);
        passed += runner.runSuite("API (method signatures)", s2);
        total += s2.size();

        // ---------- Behavior ----------
        List<Supplier<TestResult>> s3 = new ArrayList<>();
        s3.add(this::test_behavior_initial_highest_is_null);
        s3.add(this::test_behavior_two_observers_receive_notifications);
        s3.add(this::test_behavior_removeObserver_stops_notifications);
        s3.add(this::test_behavior_duplicate_registration_not_duplicated);
        s3.add(this::test_behavior_place_updates_highest_and_notifies_every_time_with_stable_event);
        s3.add(this::test_behavior_detach_idempotent);
        passed += runner.runSuite("Behavior (notification flow & state changes)", s3);
        total += s3.size();

        return new Result(passed, total);
    }

    private Object newAuction(K k) {
        if (k.Auction == null) return null;
        try {
            Constructor<?> c0 = k.Auction.getDeclaredConstructor();
            c0.setAccessible(true);
            return c0.newInstance();
        } catch (Throwable ignore) {
        }
        // common fallbacks, in case students add arguments
        for (Class<?> t : new Class<?>[]{int.class, double.class, String.class}) {
            try {
                Constructor<?> c1 = k.Auction.getDeclaredConstructor(t);
                c1.setAccessible(true);
                return c1.newInstance(t == int.class ? 0 : t == double.class ? 0.0 : "");
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private Object newBid(K k, String who, int amount) {
        try {
            Constructor<?> c = k.Bid.getDeclaredConstructor(String.class, int.class);
            c.setAccessible(true);
            return c.newInstance(who, amount);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Build a proxy that implements the primary Observer and (if present) the alternate Observer interface.
     */
    private Object newObserverProxy(K k, Probe p) {
        List<Class<?>> ifaces = new ArrayList<>();
        if (k.Observer != null) ifaces.add(k.Observer);
        if (k.AltObserver != null) ifaces.add(k.AltObserver);
        return Proxy.newProxyInstance(
                (k.Observer != null ? k.Observer.getClassLoader() : k.Auction.getClassLoader()),
                ifaces.toArray(new Class<?>[0]),
                p);
    }

    private Method findOneArgPublicMethodByName(Class<?> type, String name, Object arg) {
        if (type == null) return null;
        // Search public API first (includes inherited methods)
        for (Method m : type.getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() != 1) continue;
            Class<?> p = m.getParameterTypes()[0];
            if (arg == null || p.isInstance(arg)) return m;
        }
        // Then declared (non-public) for better error messages / odd student code
        for (Method m : type.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() != 1) continue;
            Class<?> p = m.getParameterTypes()[0];
            if (arg == null || p.isInstance(arg)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private boolean addObserver(K k, Object auction, Object observer) {
        try {
            Method add = findOneArgPublicMethodByName(k.Auction, "addObserver", observer);
            if (add == null) return false;
            add.setAccessible(true);
            add.invoke(auction, observer);
            return true;
        } catch (InvocationTargetException ite) {
            // bubble cause into the failure details in the test (optional)
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean removeObserver(K k, Object auction, Object observer) {
        try {
            Method rem = findOneArgPublicMethodByName(k.Auction, "removeObserver", observer);
            if (rem == null) return false;
            rem.setAccessible(true);
            rem.invoke(auction, observer);
            return true;
        } catch (InvocationTargetException ite) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean place(K k, Object auction, Object bid, StringBuilder err) {
        try {
            getDeclaredMethod(k.Auction, "place", k.Bid).invoke(auction, bid);
            return true;
        } catch (InvocationTargetException ite) {
            if (err != null) err.append("Cause: ").append(ite.getCause());
            return false;
        } catch (Throwable t) {
            if (err != null) err.append(t);
            return false;
        }
    }

    private Object highest(K k, Object auction) {
        try {
            return getDeclaredMethod(k.Auction, "highest").invoke(auction);
        } catch (Throwable t) {
            return null;
        }
    }

    private Integer bidAmountOrNull(Class<?> Bid, Object b) {
        if (b == null) return null;
        try { // common getter
            Method m = Bid.getMethod("getAmount");
            return (Integer) m.invoke(b);
        } catch (Throwable ignored) {
        }
        try { // field fallback
            Field f = Bid.getDeclaredField("amount");
            f.setAccessible(true);
            Object v = f.get(b);
            return (v instanceof Integer) ? (Integer) v : null;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private boolean sameBidByIdentityOrAmount(Class<?> Bid, Object a, Object b) {
        if (a == b) return true;
        Integer aa = bidAmountOrNull(Bid, a);
        Integer bb = bidAmountOrNull(Bid, b);
        return aa != null && bb != null && aa.equals(bb);
    }

    private TestResult test_structure_auction_implements_subject_AND_exposes_methods() {
        String title = "Auction implements Subject AND exposes public methods";
        K k = k();
        boolean implementsIface = (k.Auction != null && k.Subject != null && k.Subject.isAssignableFrom(k.Auction));
        Method add = getDeclaredMethod(k.Auction, "addObserver", k.Observer);
        Method rem = getDeclaredMethod(k.Auction, "removeObserver", k.Observer);
        Method notify = getDeclaredMethod(k.Auction, "notifyObservers", String.class, Object.class);
        // even if methods are declared against the alternate Observer, findOneArgMethod will catch it
        if (add == null) add = findOneArgMethod(k.Auction, "addObserver");
        if (rem == null) rem = findOneArgMethod(k.Auction, "removeObserver");
        if (notify == null) notify = findTwoArgMethod(k.Auction, "removeObserver");
        boolean methods = (add != null && Modifier.isPublic(add.getModifiers()) && add.getReturnType() == void.class)
                && (rem != null && Modifier.isPublic(rem.getModifiers()) && rem.getReturnType() == void.class)
                && (notify != null && Modifier.isPublic(notify.getModifiers()) && notify.getReturnType() == void.class);
        boolean ok = implementsIface && methods;
        return new TestResult(title, ok,
                ok ? "" : "Auction must implement Subject and declare its methods.");
    }

    private TestResult test_structure_observer_update_exact_signature() {
        String title = "Observer has correct methods implemented";
        K k = k();
        Method m = getDeclaredMethod(k.Observer, "update", String.class, Object.class);
        boolean ok = (k.Observer != null) && (m != null) && (m.getReturnType() == void.class);
        return new TestResult(title, ok,
                ok ? "" : "Define an update method on Observer.");
    }

    /* ============================  Structure  ============================ */

    private TestResult test_structure_all_required_types_exist() {
        String title = "All required types exist & are public (Subject, Observer, Auction, Bid, ConsoleAnnouncer, MaxBidTracker, Main)";
        K k = k();
        boolean ok =
                isPublicInterface(k.Subject) &&
                        isPublicInterface(k.Observer) &&
                        isPublicClass(k.Auction) &&
                        isPublicClass(k.Bid) &&
                        isPublicClass(k.Console) &&
                        isPublicClass(k.Tracker) &&
                        isPublicClass(k.Main);
        return new TestResult(
                title,
                ok,
                ok ? "" : "Ensure the six classes plus two interfaces exist in lab3.observer and are public."
        );
    }

    private TestResult test_structure_concrete_observers_implement_Observer() {
        String title = "Concrete observers implement Observer";
        K k = k();
        Class<?> Console = loadOrNull(k.root, "ConsoleAnnouncer");
        Class<?> Tracker = loadOrNull(k.root, "MaxBidTracker");
        boolean cOK = classImplements(Console, k.Observer);
        boolean tOK = classImplements(Tracker, k.Observer);
        boolean ok = cOK && tOK;
        return new TestResult(
                title,
                ok,
                ok ? "" : "The two concrete observer classes must implement the Observer interface.",
                "ConsoleAnnouncer implements? " + cOK + ", MaxBidTracker implements? " + tOK
        );
    }

    private TestResult test_api_auction_methods_signatures() {
        String title = "Auction methods signatures";
        K k = k();
        Method place = getDeclaredMethod(k.Auction, "place", k.Bid);
        Method highest = getDeclaredMethod(k.Auction, "highest");
        boolean ok = pubVoid(place) && pubNoArg(highest, k.Bid);
        return new TestResult(title, ok,
                ok ? "" : "place() shouldn't return anything, highest should return a Bid. Both should have the correct visibility.");
    }

    private TestResult test_api_subject_contract_signatures() {
        String title = "Subject declares its methods correctly";
        K k = k();
        try {
            Method add = k.Subject.getMethod("addObserver", k.Observer);
            Method rem = k.Subject.getMethod("removeObserver", k.Observer);
            Method notify = k.Subject.getMethod("notifyObservers", String.class, Object.class);
            boolean add_ok = Modifier.isAbstract(add.getModifiers()) && add.getReturnType() == void.class;
            boolean rem_ok = Modifier.isAbstract(rem.getModifiers()) && rem.getReturnType() == void.class;
            boolean notify_ok = (notify != null) && Modifier.isAbstract(notify.getModifiers()) && notify.getReturnType() == void.class;

            boolean ok = add_ok && rem_ok && notify_ok;
            return new TestResult(title, ok,
                    ok ? "" : "Ensure Subject's methods are abstract and don't return anything.");
        } catch (Throwable t) {
            return new TestResult(title, false,
                    "Missing methods on Subject: addObserver/removeObserver/notifyObservers.");
        }
    }


    /* ============================  API  ============================ */

    private TestResult test_api_auction_required_fields_modifiers() {
        String title = "Auction required fields modifiers";
        K k = k();
        Field obs = findPrivateFieldAssignableTo(k.Auction, java.util.Collection.class, null);
        Field hi = findPrivateFieldAssignableTo(k.Auction, k.Bid, null);
        boolean ok = (obs != null) && (hi != null);
        return new TestResult(title, ok,
                ok ? "" : "Provide a Set for observers and a Bid field for the current highest bid. Check correct visibility of fields.");
    }

    private TestResult test_api_consoleAnnouncer_signatures() {
        String title = "ConsoleAnnouncer signatures and overrides";
        K k = k();
        Class<?> Console = loadOrNull(k.root, "ConsoleAnnouncer");
        try {
            // public no-arg constructor
            boolean ctorOk;
            try {
                Constructor<?> c0 = Console.getDeclaredConstructor();
                ctorOk = Modifier.isPublic(c0.getModifiers());
            } catch (Throwable t) {
                ctorOk = false;
            }

            // public void update(String,Object)
            Method up = Console.getMethod("update", String.class, Object.class);
            boolean updOk = Modifier.isPublic(up.getModifiers()) && up.getReturnType() == void.class;

            boolean ok = ctorOk && updOk;
            return new TestResult(title, ok,
                    ok ? "" : "Verify correct visibility.");
        } catch (Throwable t) {
            return new TestResult(title, false,
                    "Missing public constructor and/or update(String,Object) method.");
        }
    }

    private TestResult test_api_maxBidTracker_signatures_and_override() {
        String title = "MaxBidTracker signatures";
        K k = k();
        Class<?> Tracker = loadOrNull(k.root, "MaxBidTracker");
        try {
            // update signature
            Method up = Tracker.getMethod("update", String.class, Object.class);
            boolean updOk = Modifier.isPublic(up.getModifiers()) && up.getReturnType() == void.class;

            // getter variations
            Method getMax = null, getHighest = null, highest = null;
            try {
                getMax = Tracker.getMethod("getMax");
            } catch (Throwable ignore) {
            }
            try {
                getHighest = Tracker.getMethod("getHighest");
            } catch (Throwable ignore) {
            }
            try {
                highest = Tracker.getMethod("highest");
            } catch (Throwable ignore) {
            }
            boolean getterOk =
                    (getMax != null && getMax.getReturnType() == int.class && Modifier.isPublic(getMax.getModifiers()))
                            || ((getHighest != null || highest != null) && ((getHighest != null ? getHighest : highest).getReturnType() == k.Bid)
                            && Modifier.isPublic((getHighest != null ? getHighest : highest).getModifiers()));

            boolean ok = updOk && getterOk;
            return new TestResult(title, ok,
                    ok ? "" : "Verify methods are of correct visibility and overriden.");
        } catch (Throwable t) {
            return new TestResult(title, false,
                    "Missing update(String,Object) and/or a public getter for the max.");
        }
    }

    private TestResult test_api_auction_override_annotations() { // or your current test name
        return SourceChecks.requireOverrides(
                "@Override present on observers' propertyChange",
                "lab3.observer",
                Map.of(
                        "Auction", List.of(
                                "addObserver",
                                "removeObserver",
                                "notifyObservers"
                        )
                )
        );
    }

    private TestResult test_api_observers_override_annotations() { // or your current test name
        return SourceChecks.requireOverrides(
                "@Override present on observers' update",
                "lab3.observer",
                Map.of(
                        "ConsoleAnnouncer", List.of("update"),
                        "MaxBidTracker", List.of("update")
                )
        );
    }

    private TestResult test_behavior_initial_highest_is_null() {
        String title = "Behavior initial highest value";
        K k = k();
        Object auction = newAuction(k);
        if (auction == null) return new TestResult(title, false, "Could not instantiate Auction.");
        Object h0 = highest(k, auction);
        boolean ok = (h0 == null);
        return new TestResult(title, ok,
                ok ? "" : "Before any bids, highest() should be null (no winner yet).");
    }

    private TestResult test_behavior_two_observers_receive_notifications() {
        String title = "Behavior two observers receive notifications";
        K k = k();
        Object auction = newAuction(k);
        if (auction == null) return new TestResult(title, false,
                "Could not instantiate Auction.");

        Probe p1 = new Probe(), p2 = new Probe();
        Object o1 = newObserverProxy(k, p1), o2 = newObserverProxy(k, p2);
        if (!addObserver(k, auction, o1) || !addObserver(k, auction, o2)) {
            return new TestResult(
                    title,
                    false,
                    "Failed to add observers (check method signatures/visibility).",
                    "Available addObserver overloads: " +
                            Arrays.toString(Arrays.stream(k.Auction.getMethods())
                                    .filter(m -> m.getName().equals("addObserver")).toArray())
            );
        }

        Object b = newBid(k, "Alice", 10);
        StringBuilder err = new StringBuilder();
        boolean okCall = place(k, auction, b, err);
        boolean ok = okCall && p1.calls == 1 && p2.calls == 1
                && p1.events.get(0) != null && p2.events.get(0) != null
                && Objects.equals(p1.events.get(0), p2.events.get(0))
                && k.Bid.isInstance(p1.payloads.get(0)) && k.Bid.isInstance(p2.payloads.get(0));
        return new TestResult(title, ok,
                ok ? "" : "Runtime error while invoking methods. Verify signatures and visibility.",
                ok ? "" : err.toString());
    }

    /* ============================  Behavior  ============================ */

    private TestResult test_behavior_removeObserver_stops_notifications() {
        String title = "Behavior remove observer stops notifications";
        K k = k();
        Object auction = newAuction(k);
        if (auction == null) return new TestResult(title, false, "Could not instantiate Auction.");
        Probe p1 = new Probe();
        Probe p2 = new Probe();
        Object o1 = newObserverProxy(k, p1), o2 = newObserverProxy(k, p2);
        addObserver(k, auction, o1);
        addObserver(k, auction, o2);

        place(k, auction, newBid(k, "A", 10), null);
        removeObserver(k, auction, o2);
        place(k, auction, newBid(k, "B", 20), null);

        boolean ok = p1.calls == 2 && p2.calls == 1;
        return new TestResult(title, ok,
                ok ? "" : "After removal, the detached observer must not receive later notifications.",
                "Expected: observer 1 calls= 2, observer 2 calls= 1, Actual: observer 1 calls=" + p1.calls + ", observer 2 calls=" + p2.calls);
    }

    private TestResult test_behavior_duplicate_registration_not_duplicated() {
        String title = "Behavior duplicate registration not-duplicated";
        K k = k();
        Object auction = newAuction(k);
        if (auction == null) return new TestResult(title, false, "Could not instantiate Auction.");
        Probe p = new Probe();
        Object o = newObserverProxy(k, p);
        addObserver(k, auction, o);
        addObserver(k, auction, o); // add twice
        place(k, auction, newBid(k, "A", 5), null);
        boolean ok = p.calls == 1;
        return new TestResult(title, ok,
                ok ? "" : "Use a Set or equivalent to avoid duplicates.");
    }

    private TestResult test_behavior_place_updates_highest_and_notifies_every_time_with_stable_event() {
        String title = "place(...) updates highest() and pushes the Bid";
        K k = k();
        Object auction = newAuction(k);
        if (auction == null) return new TestResult(title, false, "Could not instantiate Auction.");
        Probe p = new Probe();
        Object o = newObserverProxy(k, p);
        addObserver(k, auction, o);

        Object b10 = newBid(k, "A", 10);
        Object b15 = newBid(k, "B", 15);
        Object b07 = newBid(k, "C", 7);
        Object b20 = newBid(k, "D", 20);

        place(k, auction, b10, null);
        Object h1 = highest(k, auction);
        place(k, auction, b15, null);
        Object h2 = highest(k, auction);
        place(k, auction, b07, null);
        Object h3 = highest(k, auction);
        place(k, auction, b20, null);
        Object h4 = highest(k, auction);

        boolean updates =
                sameBidByIdentityOrAmount(k.Bid, h1, b10) &&
                        sameBidByIdentityOrAmount(k.Bid, h2, b15) &&
                        sameBidByIdentityOrAmount(k.Bid, h3, b15) &&
                        sameBidByIdentityOrAmount(k.Bid, h4, b20);

        boolean notifiedOnEveryBid = (p.calls == 4);
        boolean payloadsAreBids = p.payloads.stream().allMatch(x -> x != null && k.Bid.isInstance(x));
        // event name stays consistent across notifications (teacher uses "bidPlaced")
        boolean stableEvent = new HashSet<>(p.events).size() == 1 && p.events.get(0) != null;

        boolean ok = updates && notifiedOnEveryBid && payloadsAreBids && stableEvent;
        return new TestResult(title, ok,
                ok ? "" : "highest() must track the max; notify on every place(...); use one stable event name; payload is the Bid.",
                "notified=" + p.calls + ", updatesOK=" + updates + ", stableEvent=" + stableEvent + ", events=" + p.events);
    }

    private TestResult test_behavior_detach_idempotent() {
        String title = "Detach idempotent behavior";
        K k = k();
        Object auction = newAuction(k);
        if (auction == null) return new TestResult(title, false, "Could not instantiate Auction.");
        Probe p = new Probe();
        Object o = newObserverProxy(k, p);
        addObserver(k, auction, o);
        removeObserver(k, auction, o);
        // remove again (should be safe)
        boolean ok;
        try {
            ok = removeObserver(k, auction, o);
        } catch (Throwable t) {
            ok = false;
        }
        return new TestResult(title, ok,
                ok ? "" : "Calling removeObserver twice should not throw an error.");
    }

    private static final class K {
        final String root;
        final Class<?> Subject, Observer, Auction, Bid, Console, Tracker, Main;
        // Optionally, the other Observer (from the alternate root) to multi-implement in the proxy
        final Class<?> AltObserver;

        K(String root, Class<?> subj, Class<?> obs, Class<?> auc, Class<?> bid, Class<?> altObs,
          Class<?> console, Class<?> tracker, Class<?> main) {
            this.root = root;
            this.Subject = subj;
            this.Observer = obs;
            this.Auction = auc;
            this.Bid = bid;
            this.AltObserver = altObs;
            this.Console = console;
            this.Tracker = tracker;
            this.Main = main;
        }
    }

    /**
     * Proxy probe that records update(event,payload) calls.
     */
    private static final class Probe implements InvocationHandler {
        final List<String> events = new ArrayList<>();
        final List<Object> payloads = new ArrayList<>();
        int calls = 0;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            switch (name) {
                case "update" -> {
                    calls++;
                    if (args != null && args.length >= 1) {
                        events.add(args[0] == null ? null : args[0].toString());
                        payloads.add(args.length >= 2 ? args[1] : null);
                    } else {
                        events.add(null);
                        payloads.add(null);
                    }
                    return null;
                }
                case "equals" -> {  // identity equality is fine for tests
                    return (args != null && args.length == 1 && proxy == args[0]);
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "toString" -> {
                    return "ObserverProbe@" + Integer.toHexString(System.identityHashCode(proxy));
                }
                default -> {
                    // default no-op return for other methods
                    return method.getReturnType().isPrimitive() ? 0 : null;
                }
            }
        }
    }
}
