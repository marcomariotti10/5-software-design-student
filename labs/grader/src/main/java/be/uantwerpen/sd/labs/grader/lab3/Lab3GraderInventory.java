package be.uantwerpen.sd.labs.grader.lab3;

import be.uantwerpen.sd.labs.grader.core.LabGrader;
import be.uantwerpen.sd.labs.grader.core.SourceChecks;
import be.uantwerpen.sd.labs.grader.core.SuiteRunner;
import be.uantwerpen.sd.labs.grader.core.TestResult;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public final class Lab3GraderInventory implements LabGrader {

    private static final String ROOT_LABS = "be.uantwerpen.sd.labs";
    private static final String ROOT_SOLN = "be.uantwerpen.sd.solutions";
    private static final String SUBPKG = "lab3.inventory";
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
     * Choose one root consistently for this run (prefer solutions if present).
     */
    private static String chooseRoot() {
        if (loadOrNull(ROOT_LABS, "InventoryDB") != null) return ROOT_LABS;
        if (loadOrNull(ROOT_SOLN, "InventoryDB") != null) return ROOT_SOLN;
        return ROOT_LABS;
    }

    private static K k() {
        String root = chooseRoot();
        Class<?> db = loadOrNull(root, "Database");
        Class<?> inv = loadOrNull(root, "InventoryDB");
        Class<?> ctrl = loadOrNull(root, "Controller");
        Class<?> log = loadOrNull(root, "AuditLogger");
        Class<?> reo = loadOrNull(root, "ReorderService");
        Class<?> main = loadOrNull(root, "Main");
        return new K(root, db, inv, ctrl, log, reo, main);
    }

    private static boolean isPublicClass(Class<?> c) {
        return c != null && !c.isInterface() && Modifier.isPublic(c.getModifiers());
    }

    private static boolean isAbstractClass(Class<?> c) {
        return c != null && Modifier.isAbstract(c.getModifiers());
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

    private static Method getNoArgPublicMethod(Class<?> c, String name, Class<?> ret) {
        if (c == null) return null;
        for (Method m : c.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 0 && m.getReturnType() == ret) return m;
        }
        return null;
    }

    private static boolean isPublic(Method m) {
        return m != null && Modifier.isPublic(m.getModifiers());
    }

    private static boolean isStatic(Method m) {
        return m != null && Modifier.isStatic(m.getModifiers());
    }

    private static boolean returns(Class<?> ret, Method m) {
        return m != null && m.getReturnType() == ret;
    }

    private static Field findPrivateFieldAssignableTo(Class<?> c, Class<?> rawType) {
        if (c == null) return null;
        for (Field f : c.getDeclaredFields()) {
            f.setAccessible(true);
            if (Modifier.isPrivate(f.getModifiers()) && rawType.isAssignableFrom(f.getType())) return f;
        }
        return null;
    }

    private static Field findPrivateFieldAssignableTo(Class<?> c, Class<?> rawType, String nameHintLC) {
        if (c == null) return null;
        for (Field f : c.getDeclaredFields()) {
            f.setAccessible(true);
            boolean matchType = rawType.isAssignableFrom(f.getType());
            boolean matchName = (nameHintLC == null) || f.getName().toLowerCase(Locale.ROOT).contains(nameHintLC);
            if (matchType && matchName && Modifier.isPrivate(f.getModifiers())) return f;
        }
        return null;
    }

    private static Constructor<?> privateNoArgCtor(Class<?> c) {
        try {
            Constructor<?> cons = c.getDeclaredConstructor();
            cons.setAccessible(true);
            return Modifier.isPrivate(cons.getModifiers()) ? cons : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean hasNoPublicCtors(Class<?> c) {
        try {
            for (Constructor<?> cons : c.getDeclaredConstructors()) {
                if (Modifier.isPublic(cons.getModifiers())) return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object callStaticNoArg(Class<?> type, String method) {
        try {
            Method m = type.getDeclaredMethod(method);
            m.setAccessible(true);
            return m.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object call(Object target, String name, Class<?>[] sig, Object[] args) {
        try {
            Method m = target.getClass().getMethod(name, sig);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (InvocationTargetException ite) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String captureStdout(Runnable r) {
        PrintStream prev = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        try {
            System.setOut(ps);
            r.run();
        } finally {
            System.setOut(prev);
        }
        return baos.toString();
    }

    private static boolean hasPrivateStaticInstanceFieldIfPresent(Class<?> idg) {
        // Validate the "outer-field" singleton variant strictly:
        //  - If one or more fields of type IdGenerator exist on the OUTER class,
        //    ALL such fields must be private and static.
        //  - If NO such field exists, this helper returns false (so tests can
        //    rely on the holder-pattern check to pass/fail appropriately).
        try {
            boolean any = false, allOk = true;
            for (Field f : idg.getDeclaredFields()) {
                if (f.getType() == idg) {
                    any = true;
                    int m = f.getModifiers();
                    boolean priv = Modifier.isPrivate(m);
                    boolean stat = Modifier.isStatic(m);
                    allOk &= (priv && stat);
                }
            }
            return any && allOk;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasInitializationHolderInnerClass(Class<?> idg) {
        // Holder pattern: a private static inner class that holds a private static final instance of IdGenerator
        try {
            Class<?>[] inners = idg.getDeclaredClasses();
            for (Class<?> inner : inners) {
                int mod = inner.getModifiers();
                boolean isPrivateStatic = Modifier.isPrivate(mod) && Modifier.isStatic(mod);
                if (!isPrivateStatic) continue;
                // look for a field of type IdGenerator that is private static final
                for (Field f : inner.getDeclaredFields()) {
                    if (f.getType() != idg) continue;
                    int fm = f.getModifiers();
                    boolean isPSF = Modifier.isPrivate(fm) && Modifier.isStatic(fm) && Modifier.isFinal(fm);
                    if (isPSF) return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public Result run(SuiteRunner runner) {
        this.runner = runner;

        int total = 0, passed = 0;

        // ---------- Structure ----------
        List<Supplier<TestResult>> s1 = new ArrayList<>();
        s1.add(this::test_structure_required_types_public);
        s1.add(this::test_structure_inventorydb_extends_database);
        s1.add(this::test_structure_singleton_ctor_private_and_no_public_ctors);
        s1.add(this::test_structure_singleton_instance_field_is_private_static_volatile);
        s1.add(this::test_structure_stock_field_is_private_and_concurrent);
        s1.add(this::test_structure_database_pcs_and_notify_protected);
        passed += runner.runSuite("Structure (types, inheritance, fields, access)", s1);
        total += s1.size();

        // ---------- API ----------
        List<Supplier<TestResult>> s2 = new ArrayList<>();
        s2.add(this::test_api_getInstance_signature);
        s2.add(this::test_api_setStock_signature);
        s2.add(this::test_api_getStock_signature);
        s2.add(this::test_api_database_listener_signatures);
        s2.add(this::test_api_observer_classes_implement_PCL);
        s2.add(this::test_api_reorder_ctor_signature);
        s2.add(this::test_api_observer_propertyChange_has_Override_annotations);
        passed += runner.runSuite("API (contracts + @Override annotations)", s2);
        total += s2.size();

        // ---------- Behavior ----------
        List<Supplier<TestResult>> s3 = new ArrayList<>();
        s3.add(this::test_behavior_singleton_identity_across_calls);
        s3.add(this::test_behavior_singleton_identity_multithreaded);
        s3.add(this::test_behavior_getStock_default_zero);
        s3.add(this::test_behavior_setStock_first_event_has_null_old_then_updates);
        s3.add(this::test_behavior_event_name_is_stable_stockChanged);
        s3.add(this::test_behavior_two_listeners_both_receive);
        s3.add(this::test_behavior_remove_is_idempotent);
        s3.add(this::test_behavior_reorder_threshold_boundary_and_below);
        s3.add(this::test_behavior_audit_prints_new_value);
        s3.add(this::test_integration_controller_and_main_wiring);
        passed += runner.runSuite("Behavior (events, listeners, wiring, concurrency)", s3);
        total += s3.size();

        return new Result(passed, total);
    }

    private TestResult test_structure_required_types_public() {
        String title = "All required types exist & correct visibility (Database, InventoryDB, Controller, AuditLogger, ReorderService, Main)";
        K k = k();
        boolean ok =
                isPublicClass(k.Database) && isAbstractClass(k.Database) &&
                        isPublicClass(k.InventoryDB) &&
                        isPublicClass(k.Controller) &&
                        isPublicClass(k.AuditLogger) &&
                        isPublicClass(k.ReorderService) &&
                        isPublicClass(k.Main);
        return new TestResult(title, ok, ok ? "" : "Ensure all six classes exist in lab3.inventory and have correct visibility/abstractness.");
    }

    private TestResult test_structure_inventorydb_extends_database() {
        String title = "InventoryDB extends Database";
        K k = k();
        boolean ok = (k.InventoryDB != null && k.Database != null && k.Database.isAssignableFrom(k.InventoryDB));
        return new TestResult(title, ok, ok ? "" : "InventoryDB must extend Database to inherit listener helpers.");
    }

    /* ============================  Structure  ============================ */

    private TestResult test_structure_singleton_ctor_private_and_no_public_ctors() {
        String title = "Singleton constructor visibility";
        K k = k();
        if (k.InventoryDB == null) return new TestResult(title, false, "Missing InventoryDB.");
        boolean privNoArg = privateNoArgCtor(k.InventoryDB) != null;
        boolean noPublic = hasNoPublicCtors(k.InventoryDB);
        boolean ok = privNoArg && noPublic;
        return new TestResult(title, ok, ok ? "" : "Singleton needs non-accessible constructors.");
    }

    private TestResult test_structure_singleton_instance_field_is_private_static_volatile() {
        String title = "Initialization-holder inner class or outer instance field present (depends on Singleton implementation)";
        K k = k();
        if (k.InventoryDB == null) {
            return new TestResult(title, false, "Missing IdGenerator class.");
        }
        boolean outerOk = hasPrivateStaticInstanceFieldIfPresent(k.InventoryDB);
        boolean holderOk = hasInitializationHolderInnerClass(k.InventoryDB);
        boolean ok = outerOk || holderOk;
        String msg = ok ? "" : "Implement either: (a) an outer private static InventoryDB field, or (b) the initialization-holder pattern (private static inner class with a private static final InventoryDB).";
        return new TestResult(title, ok, msg);
    }

    private TestResult test_structure_stock_field_is_private_and_concurrent() {
        String title = "InventoryDB holds a concurrent Map for stock";
        K k = k();
        if (k.InventoryDB == null) return new TestResult(title, false, "Missing InventoryDB.");

        // Prefer the 'stock' field specifically, but fall back to any private Map field.
        Field f = findPrivateFieldAssignableTo(k.InventoryDB, Map.class, "stock");
        if (f == null) f = findPrivateFieldAssignableTo(k.InventoryDB, Map.class);
        if (f == null) return new TestResult(title, false, "Keep stock in a ConcurrentMap (e.g., ConcurrentHashMap).");

        boolean isPrivate = Modifier.isPrivate(f.getModifiers());

        // 1) Quick win: if the declared type itself is concurrent, we can pass without instance creation.
        boolean declaredConcurrent =
                ConcurrentMap.class.isAssignableFrom(f.getType()) ||
                        f.getType().getName().endsWith("ConcurrentHashMap");

        // 2) Otherwise, try to inspect the runtime value from a fresh instance.
        boolean valueConcurrent = false;
        try {
            // Try private no-arg ctor first (avoids relying on getInstance() which may be unimplemented in labs).
            Object instance = null;
            try {
                Constructor<?> c = k.InventoryDB.getDeclaredConstructor();
                c.setAccessible(true);
                instance = c.newInstance();
            } catch (Throwable ctorFail) {
                // Fallback: try static getInstance() if available
                try {
                    Method m = k.InventoryDB.getDeclaredMethod("getInstance");
                    m.setAccessible(true);
                    instance = m.invoke(null);
                } catch (Throwable ignore) { /* keep instance as null */ }
            }

            f.setAccessible(true);
            Object target = Modifier.isStatic(f.getModifiers()) ? null : instance;
            Object value = (target == null) ? null : f.get(target);

            valueConcurrent =
                    (value instanceof ConcurrentMap) ||
                            (value != null && value.getClass().getName().endsWith("ConcurrentHashMap"));
        } catch (Throwable ignore) {
            // Swallow everything to keep the test non-throwing.
        }

        boolean ok = isPrivate && (declaredConcurrent || valueConcurrent);
        return new TestResult(title, ok, ok ? "" : "Keep stock in a ConcurrentMap (e.g., ConcurrentHashMap).");
    }

    private TestResult test_structure_database_pcs_and_notify_protected() {
        String title = "Database has PropertyChangeSupport and notifyObservers(String,Object,Object)";
        K k = k();
        if (k.Database == null) return new TestResult(title, false, "Missing Database.");
        boolean pcsOk = findPrivateFieldAssignableTo(k.Database, java.beans.PropertyChangeSupport.class) != null;
        Method notify = getDeclaredMethod(k.Database, "notifyObservers", String.class, Object.class, Object.class);
        boolean notifyOk = notify != null && notify.getReturnType() == void.class && Modifier.isProtected(notify.getModifiers());
        return new TestResult(title, pcsOk && notifyOk, pcsOk && notifyOk ? "" : "Use a PropertyChangeSupport and a notify helper.");
    }

    private TestResult test_api_getInstance_signature() {
        String title = "getInstance() is correctly defined returns InventoryDB";
        K k = k();
        if (k.InventoryDB == null) return new TestResult(title, false, "Missing InventoryDB.");
        Method m = getDeclaredMethod(k.InventoryDB, "getInstance");
        boolean ok = m != null && isPublic(m) && isStatic(m) && returns(k.InventoryDB, m);
        return new TestResult(title, ok, ok ? "" : "Ensure correct visibility and that the method getInstance() can be called without the necessity to create an object.");
    }

    private TestResult test_api_setStock_signature() {
        String title = "setStock defined correctly";
        K k = k();
        if (k.InventoryDB == null) return new TestResult(title, false, "Missing InventoryDB.");
        Method m = getDeclaredMethod(k.InventoryDB, "setStock", String.class, int.class);
        boolean ok = m != null && isPublic(m) && m.getReturnType() == void.class;
        return new TestResult(title, ok, ok ? "" : "Ensure correct visibility, return type and method arguments.");
    }

    /* ============================  API  ============================ */

    private TestResult test_api_getStock_signature() {
        String title = "getStock(String) is correctly defined";
        K k = k();
        if (k.InventoryDB == null) return new TestResult(title, false, "Missing InventoryDB.");
        Method m = getDeclaredMethod(k.InventoryDB, "getStock", String.class);
        boolean ok = m != null && isPublic(m) && m.getReturnType() == int.class;
        return new TestResult(title, ok, ok ? "" : "Ensure correct visibility, return type and method arguments.");
    }

    private TestResult test_api_database_listener_signatures() {
        String title = "Database exposes addListener/removeListener(PropertyChangeListener)";
        K k = k();
        if (k.Database == null) return new TestResult(title, false, "Missing Database.");
        try {
            Method add = k.Database.getMethod("addListener", PropertyChangeListener.class);
            Method rem = k.Database.getMethod("removeListener", PropertyChangeListener.class);
            boolean ok = add.getReturnType() == void.class && rem.getReturnType() == void.class
                    && Modifier.isPublic(add.getModifiers()) && Modifier.isPublic(rem.getModifiers());
            return new TestResult(title, ok, ok ? "" : "Ensure correct visibility and return type.");
        } catch (Throwable t) {
            return new TestResult(title, false, "Missing addListener/removeListener(PropertyChangeListener).");
        }
    }

    private TestResult test_api_observer_classes_implement_PCL() {
        String title = "AuditLogger and ReorderService implement PropertyChangeListener";
        K k = k();
        boolean ok =
                (k.AuditLogger != null && PropertyChangeListener.class.isAssignableFrom(k.AuditLogger)) &&
                        (k.ReorderService != null && PropertyChangeListener.class.isAssignableFrom(k.ReorderService));
        return new TestResult(title, ok, ok ? "" : "Both observers must implement PropertyChangeListener.");
    }

    private TestResult test_api_reorder_ctor_signature() {
        String title = "ReorderService has a public int constructor";
        K k = k();
        if (k.ReorderService == null) return new TestResult(title, false, "Missing ReorderService.");
        try {
            Constructor<?> c = k.ReorderService.getDeclaredConstructor(int.class);
            boolean ok = Modifier.isPublic(c.getModifiers());
            return new TestResult(title, ok, ok ? "" : "Provide public ReorderService(int threshold).");
        } catch (Throwable t) {
            return new TestResult(title, false, "Missing public ReorderService(int) constructor.");
        }
    }

    private TestResult test_api_observer_propertyChange_has_Override_annotations() {
        return SourceChecks.requireOverrides(
                "@Override present on observers' propertyChange",
                "lab3.inventory",
                Map.of(
                        "AuditLogger", List.of("propertyChange"),
                        "ReorderService", List.of("propertyChange")
                )
        );
    }

    private TestResult test_behavior_singleton_identity_across_calls() {
        String title = "getInstance() returns the same instance (reference equality)";
        K k = k();
        if (k.InventoryDB == null) return new TestResult(title, false, "Missing InventoryDB.");
        Object a = callStaticNoArg(k.InventoryDB, "getInstance");
        Object b = callStaticNoArg(k.InventoryDB, "getInstance");
        boolean ok = (a != null) && (a == b) && k.InventoryDB.isInstance(a);
        return new TestResult(title, ok, ok ? "" : "Ensure a single shared instance is returned.");
    }

    private TestResult test_behavior_singleton_identity_multithreaded() {
        String title = "Singleton identity holds under concurrency";
        K k = k();
        if (k.InventoryDB == null) return new TestResult(title, false, "Missing InventoryDB.");
        try {
            // Parameters tuned to amplify the race window
            final int THREADS = 64;
            final int ROUNDS = 500; // many attempts to surface the race

            // Locate INSTANCE field so we can reset it between rounds
            Field instanceField = null;
            for (Field f : k.InventoryDB.getDeclaredFields()) {
                if (f.getType() == k.InventoryDB && Modifier.isStatic(f.getModifiers())) {
                    instanceField = f;
                    break;
                }
            }
            if (instanceField != null) instanceField.setAccessible(true);

            for (int round = 0; round < ROUNDS; round++) {
                // Try to reset INSTANCE to null (best effort)
                try {
                    if (instanceField != null) instanceField.set(null, null);
                } catch (Throwable ignore) { /* keep going; test must not throw */ }

                // Barrier to align threads
                java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(THREADS);
                java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(THREADS);
                List<Object> refs = Collections.synchronizedList(new ArrayList<>());

                for (int i = 0; i < THREADS; i++) {
                    new Thread(() -> {
                        try {
                            barrier.await(); // start at the same time
                            refs.add(callStaticNoArg(k.InventoryDB, "getInstance"));
                        } catch (Throwable ignored) {
                            refs.add(null);
                        } finally {
                            done.countDown();
                        }
                    }).start();
                }

                done.await();
                refs.removeIf(Objects::isNull);

                // Count distinct references
                Set<Object> uniq = Collections.newSetFromMap(new IdentityHashMap<>());
                uniq.addAll(refs);

                // If we ever observe >1 distinct instance, the singleton is not safe
                if (uniq.size() > 1) {
                    return new TestResult(title, false, "Multiple instances observed under concurrency (round " + round + ").");
                }
            }

            // If all rounds saw exactly one instance, we consider it OK
            return new TestResult(title, true, "");
        } catch (Throwable t) {
            return new TestResult(title, false, "Error during concurrency test: " + t);
        }
    }

    /* ============================  Behavior  ============================ */

    private TestResult test_behavior_getStock_default_zero() {
        String title = "getStock on unknown SKU returns 0";
        K k = k();
        try {
            Object db = callStaticNoArg(k.InventoryDB, "getInstance");
            Method get = k.InventoryDB.getMethod("getStock", String.class);
            boolean ok = ((Integer) get.invoke(db, "UNSEEN-SKU")) == 0;
            return new TestResult(title, ok, ok ? "" : "Return 0 for unknown SKUs.");
        } catch (Throwable t) {
            return new TestResult(title, false, "Reflection/runtime error.");
        }
    }

    private TestResult test_behavior_setStock_first_event_has_null_old_then_updates() {
        String title = "First event old=null; subsequent events carry previous value";
        K k = k();
        if (k.InventoryDB == null || k.Database == null) return new TestResult(title, false, "Missing types.");
        try {
            Object db = callStaticNoArg(k.InventoryDB, "getInstance");
            Method add = k.Database.getMethod("addListener", PropertyChangeListener.class);
            Method set = k.InventoryDB.getMethod("setStock", String.class, int.class);

            PCLProbe p = new PCLProbe();
            add.invoke(db, p);

            String sku = "INIT";
            set.invoke(db, sku, 5); // first: old should be null
            set.invoke(db, sku, 7); // second: old should be 5

            boolean e2 = p.events.size() >= 2;
            PropertyChangeEvent e0 = p.events.get(0);
            PropertyChangeEvent e1 = p.events.get(1);
            boolean ok = e2 &&
                    e0.getOldValue() == null && (e0.getNewValue() instanceof Integer i0 && i0 == 5) &&
                    (e1.getOldValue() instanceof Integer o1 && o1 == 5) &&
                    (e1.getNewValue() instanceof Integer n1 && n1 == 7);
            return new TestResult(title, ok, ok ? "" : "Validate old=null on first set, then correct old/new afterwards.");
        } catch (Throwable t) {
            return new TestResult(title, false, "Runtime reflection error (check signatures/visibility).");
        }
    }

    private TestResult test_behavior_event_name_is_stable_stockChanged() {
        String title = "Event name is stably 'stockChanged' across updates";
        K k = k();
        try {
            Object db = callStaticNoArg(k.InventoryDB, "getInstance");
            Method add = k.Database.getMethod("addListener", PropertyChangeListener.class);
            Method set = k.InventoryDB.getMethod("setStock", String.class, int.class);

            PCLProbe p = new PCLProbe();
            add.invoke(db, p);

            set.invoke(db, "SKU-E", 1);
            set.invoke(db, "SKU-E", 2);
            set.invoke(db, "SKU-E", 3);

            boolean ok = p.events.size() >= 3 && p.events.stream().allMatch(e -> "stockChanged".equals(e.getPropertyName()));
            return new TestResult(title, ok, ok ? "" : "Use a single stable event name: 'stockChanged'.");
        } catch (Throwable t) {
            return new TestResult(title, false, "Runtime reflection error.");
        }
    }

    private TestResult test_behavior_two_listeners_both_receive() {
        String title = "Two listeners both receive notifications";
        K k = k();
        try {
            Object db = callStaticNoArg(k.InventoryDB, "getInstance");
            Method add = k.Database.getMethod("addListener", PropertyChangeListener.class);
            Method set = k.InventoryDB.getMethod("setStock", String.class, int.class);

            PCLProbe p1 = new PCLProbe();
            PCLProbe p2 = new PCLProbe();
            add.invoke(db, p1);
            add.invoke(db, p2);

            set.invoke(db, "SKU-TWO", 10);
            boolean ok = p1.events.size() == 1 && p2.events.size() == 1;
            return new TestResult(title, ok, ok ? "" : "Both registered listeners should be notified.");
        } catch (Throwable t) {
            return new TestResult(title, false, "Runtime reflection error.");
        }
    }

    private TestResult test_behavior_remove_is_idempotent() {
        String title = "removeListener is idempotent (safe to call twice)";
        K k = k();
        try {
            Object db = callStaticNoArg(k.InventoryDB, "getInstance");
            Method add = k.Database.getMethod("addListener", PropertyChangeListener.class);
            Method rem = k.Database.getMethod("removeListener", PropertyChangeListener.class);
            Method set = k.InventoryDB.getMethod("setStock", String.class, int.class);

            PCLProbe p = new PCLProbe();
            add.invoke(db, p);
            rem.invoke(db, p);
            boolean ok;
            try {
                rem.invoke(db, p);
                ok = true;
            } catch (Throwable t) {
                ok = false;
            }

            set.invoke(db, "SKU-IDEMP", 1);
            ok = ok && p.events.size() == 0;

            return new TestResult(title, ok, ok ? "" : "Calling remove twice should not throw and should keep the listener detached.");
        } catch (Throwable t) {
            return new TestResult(title, false, "Runtime reflection error.");
        }
    }

    private TestResult test_behavior_reorder_threshold_boundary_and_below() {
        String title = "ReorderService prints only when qty < threshold (boundary covered)";
        K k = k();
        try {
            Object db = callStaticNoArg(k.InventoryDB, "getInstance");
            Object reorder = k.ReorderService.getDeclaredConstructor(int.class).newInstance(3);
            Method add = k.Database.getMethod("addListener", PropertyChangeListener.class);
            Method set = k.InventoryDB.getMethod("setStock", String.class, int.class);

            String out = captureStdout(() -> {
                try {
                    add.invoke(db, reorder);
                    // >= threshold => no reorder print
                    set.invoke(db, "SKU-RB", 5);
                    set.invoke(db, "SKU-RB", 3);
                    // < threshold => reorder print
                    set.invoke(db, "SKU-RB", 2);
                } catch (Throwable ignored) {
                }
            });
            int first = out.indexOf("REORDER:");
            int last = out.lastIndexOf("REORDER:");
            boolean exactlyOne = (first != -1) && (first == last);
            boolean ok = exactlyOne && out.contains("REORDER:") && out.matches("(?s).*REORDER:.*2.*")
                    && !out.matches("(?s).*REORDER:.*3\\)?\\s*");
            return new TestResult(title, ok, ok ? "" : "Print only when new qty < threshold; do not print at equality.");
        } catch (Throwable t) {
            return new TestResult(title, false, "Reflection/runtime error creating/adding ReorderService.");
        }
    }

    private TestResult test_behavior_audit_prints_new_value() {
        String title = "AuditLogger prints new quantity on change";
        K k = k();
        if (k.InventoryDB == null || k.AuditLogger == null || k.Database == null) {
            return new TestResult(title, false, "Missing InventoryDB/AuditLogger/Database.");
        }
        try {
            Object db = callStaticNoArg(k.InventoryDB, "getInstance");
            Object audit = k.AuditLogger.getDeclaredConstructor().newInstance();
            Method add = k.Database.getMethod("addListener", PropertyChangeListener.class);
            Method set = k.InventoryDB.getMethod("setStock", String.class, int.class);

            String out = captureStdout(() -> {
                try {
                    add.invoke(db, audit);
                    set.invoke(db, "SKU-A", 7);
                } catch (Throwable ignored) {
                }
            });
            boolean ok = out.contains("AUDIT:") && out.contains("7");
            return new TestResult(title, ok, ok ? "" : "Audit log should include the new quantity. Print at least `AUDIT: ` and the new value.");
        } catch (Throwable t) {
            return new TestResult(title, false, "Reflection/runtime error creating/adding AuditLogger.");
        }
    }

    private TestResult test_integration_controller_and_main_wiring() {
        String title = "Integration: Controller delegates to DB & Main wires components";
        K k = k();
        if (k.InventoryDB == null || k.Controller == null || k.Main == null || k.Database == null) {
            return new TestResult(title, false, "Missing InventoryDB/Controller/Main/Database.");
        }
        try {
            Object db = callStaticNoArg(k.InventoryDB, "getInstance");
            Object ctrl = k.Controller.getDeclaredConstructor(k.InventoryDB).newInstance(db);
            Method adjust = k.Controller.getMethod("adjust", String.class, int.class);
            Method get = k.InventoryDB.getMethod("getStock", String.class);

            // 1) Controller path
            adjust.invoke(ctrl, "SKU-C", 9);
            boolean ctrlOK = ((Integer) get.invoke(db, "SKU-C")) == 9;

            // 2) Main demo path — should print both AUDIT and REORDER lines
            Object main = k.Main.getDeclaredConstructor().newInstance();
            Method run = k.Main.getMethod("run");
            String out = captureStdout(() -> {
                try {
                    run.invoke(main);
                } catch (Throwable ignored) {
                }
            });

            boolean mainOK = out.contains("AUDIT:") && out.contains("REORDER:");
            boolean ok = ctrlOK && mainOK;
            String detail = "controllerOK=" + ctrlOK + ", mainOutputHasAudit&Reorder=" + mainOK;
            return new TestResult(title, ok, ok ? "" : "Verify Controller uses DB and Main registers listeners and triggers prints.", detail);
        } catch (Throwable t) {
            return new TestResult(title, false, "Reflection/runtime error invoking Controller/Main.", t.toString());
        }
    }

    private static final class K {
        final String root;
        final Class<?> Database, InventoryDB, Controller, AuditLogger, ReorderService, Main;

        K(String root, Class<?> db, Class<?> inv, Class<?> ctrl, Class<?> log, Class<?> reo, Class<?> main) {
            this.root = root;
            this.Database = db;
            this.InventoryDB = inv;
            this.Controller = ctrl;
            this.AuditLogger = log;
            this.ReorderService = reo;
            this.Main = main;
        }
    }

    private static final class PCLProbe implements PropertyChangeListener {
        final List<PropertyChangeEvent> events = new ArrayList<>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            events.add(evt);
        }
    }
}
