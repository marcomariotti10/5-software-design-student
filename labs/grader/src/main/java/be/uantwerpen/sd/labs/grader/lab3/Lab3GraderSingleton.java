package be.uantwerpen.sd.labs.grader.lab3;

import be.uantwerpen.sd.labs.grader.core.LabGrader;
import be.uantwerpen.sd.labs.grader.core.SafeRef;
import be.uantwerpen.sd.labs.grader.core.SuiteRunner;
import be.uantwerpen.sd.labs.grader.core.TestResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lab 3 — Singleton (IdGenerator) Auto‑Grader
 * <p>
 * This grader checks structure (constructors/fields/modifiers), API (method signatures),
 * and behavior (singleton identity & ID monotonicity). It is resilient to both
 * eager and lazy singleton implementations and to the initialization‑holder pattern.
 */
public final class Lab3GraderSingleton implements LabGrader {

    // Resolve against both student & teacher trees (works pre/post export)
    private static final String[] ROOTS = {"be.uantwerpen.sd.labs", "be.uantwerpen.sd.solutions"};
    private static final String SUBPKG = "lab3.singleton";
    private static final String SIMPLE = "IdGenerator";
    private SuiteRunner runner;

    private static boolean hasPrivateNoArgCtor(Class<?> c) {
        try {
            Constructor<?> cons = c.getDeclaredConstructor();
            return Modifier.isPrivate(cons.getModifiers());
        } catch (Throwable t) {
            return false;
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

    // ---------------- Helpers ----------------

    private static Method getDeclaredNoArgMethod(Class<?> c, String name) {
        try {
            return c.getDeclaredMethod(name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean returns(Class<?> ret, Method m) {
        return m != null && m.getReturnType() == ret;
    }

    private static boolean isPublic(Method m) {
        return m != null && Modifier.isPublic(m.getModifiers());
    }

    private static boolean isStatic(Method m) {
        return m != null && Modifier.isStatic(m.getModifiers());
    }

    private static boolean notStatic(Method m) {
        return m != null && !Modifier.isStatic(m.getModifiers());
    }

    private static Object callStaticNoArg(Method m) {
        try {
            m.setAccessible(true);
            return m.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static long callNextId(Object target) {
        try {
            Method m = target.getClass().getMethod("nextId");
            m.setAccessible(true);
            Object o = m.invoke(target);
            return (o instanceof Number) ? ((Number) o).longValue() : Long.MIN_VALUE;
        } catch (Throwable t) {
            return Long.MIN_VALUE;
        }
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

    private static boolean outerClassHasSelfTypedInstanceField(Class<?> idg) {
        try {
            for (Field f : idg.getDeclaredFields()) {
                if (f.getType() == idg) return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasPrivateNonStaticNumericCounterField(Class<?> idg) {
        // Accept either a primitive long/Long or an AtomicLong as the counter.
        // Must be private and non-static. We ignore any fields of the same type as the class (holders).
        try {
            for (Field f : idg.getDeclaredFields()) {
                if (f.getType() == idg) continue; // skip self-typed fields (e.g., eager instance)
                int m = f.getModifiers();
                boolean priv = Modifier.isPrivate(m);
                boolean stat = Modifier.isStatic(m);
                Class<?> t = f.getType();
                boolean numeric = (t == long.class) || (t == Long.class) ||
                        "java.util.concurrent.atomic.AtomicLong".equals(t.getName());
                if (priv && !stat && numeric) return true;
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
        s1.add(this::test_structure_classExists);
        s1.add(this::test_structure_ctor_private_noArgs);
        s1.add(this::test_structure_noPublicCtors);
        s1.add(this::test_structure_ifNoOuterInstanceField_thenHolderPatternPresent);
        s1.add(this::test_structure_counterField_privateNonStatic_numeric);
        passed += this.runner.runSuite("Structure (classes, constructors, modifiers)", s1);
        total += s1.size();

        // ---------- API ----------
        List<Supplier<TestResult>> s2 = new ArrayList<>();
        s2.add(this::test_api_getInstance_signature_publicStatic_returnsSelf);
        s2.add(this::test_api_nextId_signature_publicNonStatic_returnsLong);
        passed += this.runner.runSuite("API (method signatures)", s2);
        total += s2.size();

        // ---------- Behavior ----------
        List<Supplier<TestResult>> s3 = new ArrayList<>();
        s3.add(this::test_behavior_getInstance_identitySingleton);
        s3.add(this::test_behavior_nextId_monotonicByOne);
        s3.add(this::test_behavior_stateSharedAcrossReferences);
        s3.add(this::test_integration_ticketService_usesSingletonIdGenerator);
        passed += this.runner.runSuite("Behavior (singleton identity & ID sequence)", s3);
        total += s3.size();

        return new Result(passed, total);
    }

    private Class<?> cls(String simpleName) {
        for (String root : ROOTS) {
            String fqn = root + "." + SUBPKG + "." + simpleName;
            try {
                return Class.forName(fqn);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private Ctx ctx() {
        return new Ctx(cls(SIMPLE));
    }

    private TestResult test_structure_classExists() {
        String title = "Class exists (lab3.singleton.IdGenerator)";
        Ctx k = ctx();
        boolean ok = k.IdGenerator != null;
        return new TestResult(title, ok,
                ok ? "" : "Could not resolve class in either student or solutions tree.");
    }


    // ---------------- Tests: Structure ----------------

    private TestResult test_structure_ctor_private_noArgs() {
        String title = "Constructor has correct visibility";
        Ctx k = ctx();
        boolean ok = k.IdGenerator != null && hasPrivateNoArgCtor(k.IdGenerator);
        return new TestResult(title, ok,
                ok ? "" : "Ensure only IdGenerator can access the constructor.");
    }

    private TestResult test_structure_noPublicCtors() {
        String title = "No one can access constructors";
        Ctx k = ctx();
        boolean ok = k.IdGenerator != null && hasNoPublicCtors(k.IdGenerator);
        return new TestResult(title, ok,
                ok ? "" : "Singleton classes must not expose constructors to everyone.");
    }

    private TestResult test_structure_ifNoOuterInstanceField_thenHolderPatternPresent() {
        String title = "Initialization-holder inner class or outer instance field present (depends on Singleton implementation)";
        Ctx k = ctx();
        if (k.IdGenerator == null) {
            return new TestResult(title, false, "Missing IdGenerator class.");
        }
        boolean outerOk = hasPrivateStaticInstanceFieldIfPresent(k.IdGenerator);
        boolean holderOk = hasInitializationHolderInnerClass(k.IdGenerator);
        boolean ok = outerOk || holderOk;
        String msg = ok ? "" : "Implement either: (a) an outer private static IdGenerator field, or (b) the initialization-holder pattern (private static inner class with a private static final IdGenerator).";
        return new TestResult(title, ok, msg);
    }

    private TestResult test_structure_counterField_privateNonStatic_numeric() {
        String title = "Counter field correctly defined";
        Ctx k = ctx();
        if (k.IdGenerator == null) {
            return new TestResult(title, false, "Missing IdGenerator class.");
        }
        boolean ok = hasPrivateNonStaticNumericCounterField(k.IdGenerator);
        return new TestResult(title, ok,
                ok ? "" : "Verify whether it has the correct type and visibility.");
    }

    private TestResult test_api_getInstance_signature_publicStatic_returnsSelf() {
        String title = "getInstance() signature";
        Ctx k = ctx();
        if (k.IdGenerator == null) return new TestResult(title, false, "Missing IdGenerator class.");
        Method m = getDeclaredNoArgMethod(k.IdGenerator, "getInstance");
        boolean ok = m != null && isPublic(m) && isStatic(m) && returns(k.IdGenerator, m);
        return new TestResult(title, ok,
                ok ? "" : "Expose getInstance() correctly which returns the IdGenerator.");
    }


    // ---------------- Tests: API ----------------

    private TestResult test_api_nextId_signature_publicNonStatic_returnsLong() {
        String title = "nextId() signature";
        Ctx k = ctx();
        if (k.IdGenerator == null) return new TestResult(title, false, "Missing IdGenerator class.");
        Method m = getDeclaredNoArgMethod(k.IdGenerator, "nextId");
        boolean ok = m != null && isPublic(m) && notStatic(m) && m.getReturnType() == long.class;
        return new TestResult(title, ok,
                ok ? "" : "Provide a nextId() method returning long (correct visibility).");
    }

    private TestResult test_behavior_getInstance_identitySingleton() {
        String title = "getInstance() returns the same instance (reference equality)";
        Ctx k = ctx();
        if (k.IdGenerator == null) return new TestResult(title, false, "Missing IdGenerator class.");
        Method gi = getDeclaredNoArgMethod(k.IdGenerator, "getInstance");
        Object a = callStaticNoArg(gi);
        Object b = callStaticNoArg(gi);
        boolean ok = (a != null) && (a == b) && k.IdGenerator.isInstance(a);
        return new TestResult(title, ok,
                ok ? "" : "Ensure getInstance() retains and returns a single, shared instance.");
    }

    // ---------------- Tests: Behavior ----------------

    private TestResult test_behavior_nextId_monotonicByOne() {
        String title = "nextId() increases by exactly 1 on each call";
        Ctx k = ctx();
        if (k.IdGenerator == null) return new TestResult(title, false, "Missing IdGenerator class.");
        Method gi = getDeclaredNoArgMethod(k.IdGenerator, "getInstance");
        Object inst = callStaticNoArg(gi);
        if (inst == null) return new TestResult(title, false, "getInstance() failed or returned null.");
        long a = callNextId(inst);
        long b = callNextId(inst);
        long c = callNextId(inst);
        boolean ok = (a != Long.MIN_VALUE) && (b == a + 1) && (c == b + 1);
        String detail = ok ? "" : ("got sequence: " + a + ", " + b + ", " + c);
        return new TestResult(title, ok,
                ok ? "" : "Increase the internal counter then return it.", detail);
    }

    private TestResult test_behavior_stateSharedAcrossReferences() {
        String title = "Multiple references share the same counter/state";
        Ctx k = ctx();
        if (k.IdGenerator == null) return new TestResult(title, false, "Missing IdGenerator class.");
        Method gi = getDeclaredNoArgMethod(k.IdGenerator, "getInstance");
        Object r1 = callStaticNoArg(gi);
        Object r2 = callStaticNoArg(gi);
        if (r1 == null || r2 == null) {
            return new TestResult(title, false, "getInstance() failed or returned null.");
        }
        long before = callNextId(r1);
        long after = callNextId(r2);
        boolean ok = (after == before + 1) && (r1 == r2);
        String detail = ok ? "" : ("r1==r2? " + (r1 == r2) + ", seq: " + before + " -> " + after);
        return new TestResult(title, ok,
                ok ? "" : "Both references must be the same instance and share state.", detail);
    }

    private TestResult test_integration_ticketService_usesSingletonIdGenerator() {
        String title = "Integration: TicketService uses IdGenerator singleton";
        // Resolve classes from either student or solutions tree
        Class<?> TicketService = cls("TicketService");
        Class<?> Ticket = cls("Ticket");
        Class<?> IdGenerator = cls("IdGenerator");
        if (TicketService == null || Ticket == null || IdGenerator == null) {
            return new TestResult(
                    title,
                    false,
                    "Missing TicketService/Ticket/IdGenerator in lab3.singleton package."
            );
        }

        SafeRef R = new SafeRef();

        // 1) Create two tickets via TicketService
        Object svc = R.newInstance(TicketService, new Class<?>[]{}, new Object[]{});
        Object t1 = (svc == null) ? null : R.call(svc, "create", new Class<?>[]{String.class}, new Object[]{"A"});
        Object t2 = (svc == null) ? null : R.call(svc, "create", new Class<?>[]{String.class}, new Object[]{"B"});

        // Extract ids and titles (be defensive wrt reflection failures)
        long id1 = (t1 == null) ? Long.MIN_VALUE
                : R.callLong(t1, "getId", new Class<?>[]{}, new Object[]{}, Long.MIN_VALUE);
        long id2 = (t2 == null) ? Long.MIN_VALUE
                : R.callLong(t2, "getId", new Class<?>[]{}, new Object[]{}, Long.MIN_VALUE);
        Object title1 = (t1 == null) ? null : R.call(t1, "getTitle", new Class<?>[]{}, new Object[]{});
        Object title2 = (t2 == null) ? null : R.call(t2, "getTitle", new Class<?>[]{}, new Object[]{});

        // 2) Grab the singleton and ask for the next ID
        Method gi = getDeclaredNoArgMethod(IdGenerator, "getInstance");
        Object gen = callStaticNoArg(gi);
        long next = (gen == null) ? Long.MIN_VALUE : callNextId(gen);

        boolean ok =
                svc != null && t1 != null && t2 != null &&
                        id1 != Long.MIN_VALUE && id2 == id1 + 1 &&
                        next == id2 + 1 &&
                        "A".equals(title1) && "B".equals(title2);

        String detail = ok ? "" :
                ("svc=" + (svc != null) +
                        ", ids=[" + id1 + "," + id2 + "], next=" + next +
                        ", titles=[" + title1 + "," + title2 + "]");

        return new TestResult(
                "Integration: TicketService.create() obtains IDs from the singleton and preserves titles",
                ok,
                ok ? "" : runner.withReflection("The next ID is incorrectly acquired inside TicketService.create(..).", R),
                detail
        );
    }

    private static final class Ctx {
        final Class<?> IdGenerator;

        Ctx(Class<?> idg) {
            IdGenerator = idg;
        }
    }
}

