package be.uantwerpen.sd.labs.grader.lab2;

import be.uantwerpen.sd.labs.grader.core.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Lab2GraderAbstractions implements LabGrader {

    // Resolve against both student & teacher trees (works pre/post export)
    private static final String[] ROOTS = {"be.uantwerpen.sd.labs", "be.uantwerpen.sd.solutions"};
    private static final String SUBPKG = "lab2.classdiagrams.abstractions";
    private SuiteRunner runner;

    @Override
    public Result run(SuiteRunner runner) {
        this.runner = runner;
        int total = 0, passed = 0;

        // ---------- Structure ----------
        List<Supplier<TestResult>> s1 = new ArrayList<>();
        s1.add(this::test_structure_classesExist);
        s1.add(this::test_structure_shapeIsAbstract);
        s1.add(this::test_structure_extendsShape);
        s1.add(this::test_structure_shape_fieldsProtectedAndTyped);
        passed += runner.runSuite("Structure (classes, inheritance, modifiers)", s1);
        total += s1.size();

        // ---------- API ----------
        List<Supplier<TestResult>> s2 = new ArrayList<>();
        s2.add(this::test_api_ctors_signatures);
        s2.add(this::test_api_methods_signatures);
        s2.add(this::test_api_methods_overrides);
        s2.add(this::test_api_getName_signature);
        passed += runner.runSuite("API (constructors & methods)", s2);
        total += s2.size();

        // ---------- Behavior ----------
        List<Supplier<TestResult>> s3 = new ArrayList<>();
        s3.add(this::test_behavior_circle);
        s3.add(this::test_behavior_square);
        s3.add(this::test_behavior_triangle);
        s3.add(this::test_behavior_polymorphismSmoke);
        s3.add(this::test_behavior_constructor_throwsOnNonPositive);
        s3.add(this::test_behavior_getName_returnsGivenName);
        passed += runner.runSuite("Behavior (formulas, validation, polymorphism)", s3);
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

    // ---------------- Helpers ----------------

    private Ctx ctx() {
        return new Ctx(cls("Shape"), cls("Circle"), cls("Square"), cls("EquilateralTriangle"));
    }

    private boolean hasField(Class<?> c, String name, Class<?> type, int requiredMods) {
        if (c == null) return false;
        try {
            var f = c.getDeclaredField(name);
            int m = f.getModifiers();
            // Must be protected (not private or public)
            boolean prot = Modifier.isProtected(m) && !Modifier.isPrivate(m) && !Modifier.isPublic(m);
            return f.getType() == type && prot && (m & requiredMods) == requiredMods;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasCtor(Class<?> c, Class<?>[] sig) {
        if (c == null) return false;
        try {
            Constructor<?> cons = c.getDeclaredConstructor(sig);
            return Modifier.isPublic(cons.getModifiers());
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasPublicNoArgMethod(Class<?> c, String name, Class<?> ret) {
        if (c == null) return false;
        try {
            Method m = c.getDeclaredMethod(name);
            return Modifier.isPublic(m.getModifiers()) && m.getReturnType() == ret && m.getParameterCount() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean declaresMethodHere(Class<?> c, String name) {
        if (c == null) return false;
        try {
            c.getDeclaredMethod(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean almost(double a, double b) {
        return !Double.isNaN(a) && Math.abs(a - b) < 1e-9;
    }

    private TestResult test_structure_classesExist() {
        String title = "Classes exist (Shape, Circle, Square, EquilateralTriangle)";
        Ctx k = ctx();
        boolean ok = k.Shape != null && k.Circle != null && k.Square != null && k.EquilateralTriangle != null;
        return new TestResult(title, ok,
                ok ? "" : "Could not resolve one or more classes in lab2.classdiagrams.abstractions.", "");
    }

    // ---------------- Tests: Structure ----------------

    private TestResult test_structure_shapeIsAbstract() {
        String title = "Shape is abstract";
        Ctx k = ctx();
        boolean ok = k.Shape != null && Modifier.isAbstract(k.Shape.getModifiers());
        return new TestResult(title, ok, ok ? "" : "Mark Shape as abstract.", "");
    }

    private TestResult test_structure_extendsShape() {
        String title = "Subclasses extend Shape";
        Ctx k = ctx();
        boolean ok = k.Shape != null
                && k.Circle != null && k.Circle.getSuperclass() == k.Shape
                && k.Square != null && k.Square.getSuperclass() == k.Shape
                && k.EquilateralTriangle != null && k.EquilateralTriangle.getSuperclass() == k.Shape;
        return new TestResult(title, ok, ok ? "" : "Ensure each class extends Shape.", "");
    }

    private TestResult test_structure_shape_fieldsProtectedAndTyped() {
        String title = "Shape fields protected/typed and correctly typed";
        Ctx k = ctx();
        boolean ok = hasField(k.Shape, "size", double.class, Modifier.PROTECTED)
                && hasField(k.Shape, "name", String.class, Modifier.PROTECTED);
        return new TestResult(title,
                ok, ok ? "" : "Verify field names, types, and protected visibility.", "");
    }

    private TestResult test_api_ctors_signatures() {
        String title = "Constructors have expected signatures";
        Ctx k = ctx();
        boolean ok = hasCtor(k.Shape, new Class<?>[]{double.class, String.class})
                && hasCtor(k.Circle, new Class<?>[]{double.class, String.class})
                && hasCtor(k.Square, new Class<?>[]{double.class, String.class})
                && hasCtor(k.EquilateralTriangle, new Class<?>[]{double.class, String.class});
        return new TestResult(title, ok,
                ok ? "" : "Check parameter counts/types and public visibility.", "");
    }

    // ---------------- Tests: API ----------------

    private TestResult test_api_methods_signatures() {
        String title = "Methods have expected signatures";
        Ctx k = ctx();
        // methods present and public
        boolean sigs = hasPublicNoArgMethod(k.Shape, "calcCircumference", double.class)
                && hasPublicNoArgMethod(k.Shape, "calcArea", double.class)
                && hasPublicNoArgMethod(k.Circle, "calcCircumference", double.class)
                && hasPublicNoArgMethod(k.Circle, "calcArea", double.class)
                && hasPublicNoArgMethod(k.Square, "calcCircumference", double.class)
                && hasPublicNoArgMethod(k.Square, "calcArea", double.class)
                && hasPublicNoArgMethod(k.EquilateralTriangle, "calcCircumference", double.class)
                && hasPublicNoArgMethod(k.EquilateralTriangle, "calcArea", double.class);

        // subclasses declare overrides in their own class
        boolean decl = declaresMethodHere(k.Circle, "calcCircumference")
                && declaresMethodHere(k.Circle, "calcArea")
                && declaresMethodHere(k.Square, "calcCircumference")
                && declaresMethodHere(k.Square, "calcArea")
                && declaresMethodHere(k.EquilateralTriangle, "calcCircumference")
                && declaresMethodHere(k.EquilateralTriangle, "calcArea");

        boolean ok = sigs && decl;
        return new TestResult(title, ok,
                ok ? "" : "Ensure signatures are correct and methods are implemented in each subclass.", "");
    }

    private TestResult test_api_methods_overrides() {
        // keep your existing signature checks, then:
        TestResult r = SourceChecks.requireOverrides(
                "@Override present on calcCircumference",
                "lab2.classdiagrams.abstractions",
                Map.of(
                        "Circle", List.of("calcCircumference"),
                        "Square", List.of("calcCircumference"),
                        "EquilateralTriangle", List.of("calcCircumference")
                )
        );
        return r;
    }

    private TestResult test_api_getName_signature() {
        String title = "Method: public String getName()";
        Ctx k = ctx();
        boolean ok = hasPublicNoArgMethod(k.Shape, "getName", String.class);
        return new TestResult(title, ok,
                ok ? "" : "Expose the name via a public getter.", "");
    }

    private TestResult test_behavior_circle() {
        String title = "Circle formulas";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object c = R.newInstance(k.Circle, new Class<?>[]{double.class, String.class}, new Object[]{3.0, "c"});
        double C = (c == null) ? Double.NaN : R.callDouble(c, "calcCircumference", new Class<?>[]{}, new Object[]{}, Double.NaN);
        double A = (c == null) ? Double.NaN : R.callDouble(c, "calcArea", new Class<?>[]{}, new Object[]{}, Double.NaN);
        boolean ok = almost(C, 2 * Math.PI * 3) && almost(A, Math.PI * 9);
        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Check circumference/area for Circle.", R),
                ok ? "" : ("got C=" + C + ", A=" + A));
    }

    // ---------------- Tests: Behavior ----------------

    private TestResult test_behavior_square() {
        String title = "Square formulas";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object q = R.newInstance(k.Square, new Class<?>[]{double.class, String.class}, new Object[]{4.0, "q"});
        double C = (q == null) ? Double.NaN : R.callDouble(q, "calcCircumference", new Class<?>[]{}, new Object[]{}, Double.NaN);
        double A = (q == null) ? Double.NaN : R.callDouble(q, "calcArea", new Class<?>[]{}, new Object[]{}, Double.NaN);
        boolean ok = almost(C, 16.0) && almost(A, 16.0);
        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Check circumference/area for Square.", R),
                ok ? "" : ("got C=" + C + ", A=" + A));
    }

    private TestResult test_behavior_triangle() {
        String title = "Triangle formulas";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object t = R.newInstance(k.EquilateralTriangle, new Class<?>[]{double.class, String.class}, new Object[]{3.0, "t"});
        double C = (t == null) ? Double.NaN : R.callDouble(t, "calcCircumference", new Class<?>[]{}, new Object[]{}, Double.NaN);
        double A = (t == null) ? Double.NaN : R.callDouble(t, "calcArea", new Class<?>[]{}, new Object[]{}, Double.NaN);
        boolean ok = almost(C, 9.0) && almost(A, Math.sqrt(3) / 4 * 9.0);
        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Check circumference/area for EquilateralTriangle.", R),
                ok ? "" : ("got C=" + C + ", A=" + A));
    }

    private TestResult test_behavior_polymorphismSmoke() {
        String title = "Polymorphism Smoke Test";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object[] arr = new Object[]{
                R.newInstance(k.Circle, new Class<?>[]{double.class, String.class}, new Object[]{2.0, "c"}),
                R.newInstance(k.Square, new Class<?>[]{double.class, String.class}, new Object[]{3.0, "s"}),
                R.newInstance(k.EquilateralTriangle, new Class<?>[]{double.class, String.class}, new Object[]{4.0, "t"})
        };
        double[] areas = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            areas[i] = (arr[i] == null) ? Double.NaN : R.callDouble(arr[i], "calcArea", new Class<?>[]{}, new Object[]{}, Double.NaN);
        }
        boolean ok = almost(areas[0], Math.PI * 4.0) && almost(areas[1], 9.0) && almost(areas[2], Math.sqrt(3) / 4 * 16.0);
        return new TestResult(title, ok,
                ok ? "" : "Upcasting should still dispatch to subclass implementations.", "");
    }

    private TestResult test_behavior_constructor_throwsOnNonPositive() {
        String title = "Constructor validates non-positive size (Circle)";
        Ctx k = ctx();
        try {
            k.Circle.getDeclaredConstructor(double.class, String.class).newInstance(0.0, "x");
            return new TestResult(title, false,
                    "Expected IllegalArgumentException for size <= 0.", "");
        } catch (Throwable t) {
            boolean ok = t.getCause() instanceof IllegalArgumentException || t instanceof IllegalArgumentException;
            return new TestResult(title, ok,
                    ok ? "" : "Throw IllegalArgumentException when size <= 0.", "");
        }
    }

    private TestResult test_behavior_getName_returnsGivenName() {
        String title = "getName returns the provided name";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object s = R.newInstance(k.Square, new Class<?>[]{double.class, String.class}, new Object[]{5.0, "hello"});
        Object got = (s == null) ? null : R.call(s, "getName", new Class<?>[]{}, new Object[]{});
        boolean ok = (got instanceof String) && "hello".equals(got);
        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Check the getter implementation.", R), "");
    }

    private static final class Ctx {
        final Class<?> Shape, Circle, Square, EquilateralTriangle;

        Ctx(Class<?> s, Class<?> c, Class<?> q, Class<?> t) {
            Shape = s;
            Circle = c;
            Square = q;
            EquilateralTriangle = t;
        }
    }
}
