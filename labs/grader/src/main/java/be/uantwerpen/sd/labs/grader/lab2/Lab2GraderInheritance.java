package be.uantwerpen.sd.labs.grader.lab2;

import be.uantwerpen.sd.labs.grader.core.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Lab2GraderInheritance implements LabGrader {

    // Resolve against both teacher & student trees
    private static final String[] ROOTS = {"be.uantwerpen.sd.labs", "be.uantwerpen.sd.solutions"};
    private static final String SUBPKG = "lab2.classdiagrams.inheritance";
    private SuiteRunner runner;

    private static String miss(String what) {
        return "Missing or incorrect " + what + ".";
    }

    @Override
    public Result run(SuiteRunner runner) {
        this.runner = runner;

        int total = 0, passed = 0;

        List<Supplier<TestResult>> s1 = new ArrayList<>();
        s1.add(this::test_structure_classesExist);
        s1.add(this::test_structure_extendsEmployee);
        s1.add(this::test_structure_employee_fieldsProtected);
        s1.add(this::test_structure_programmer_fieldsProtected);
        s1.add(this::test_structure_customerservice_fieldsProtected);
        s1.add(this::test_structure_departmentOfficer_fieldsProtected);
        passed += runner.runSuite("Structure (classes, inheritance, modifiers)", s1);
        total += s1.size();

        List<Supplier<TestResult>> s2 = new ArrayList<>();
        s2.add(this::test_ctors_signatures);
        s2.add(this::test_methods_signatures);
        s2.add(this::test_source_requiresOverride);
        passed += runner.runSuite("API (constructors & methods)", s2);
        total += s2.size();

        List<Supplier<TestResult>> s3 = new ArrayList<>();
        s3.add(this::test_behavior_employeeSalary);
        s3.add(this::test_behavior_programmerSalary);
        s3.add(this::test_behavior_customerServiceSalary);
        s3.add(this::test_behavior_departmentOfficerSalary);
        s3.add(this::test_behavior_polymorphismSmoke);
        passed += runner.runSuite("Behavior (salary calculations & polymorphism)", s3);
        total += s3.size();

        return new Result(passed, total);
    }

    /* ============================  Helpers  ============================ */

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

    private boolean almost(double a, double b) {
        return !Double.isNaN(a) && Math.abs(a - b) < 1e-9;
    }

    private Ctx ctx() {
        return new Ctx(cls("Employee"), cls("Programmer"), cls("CustomerService"), cls("DepartmentOfficer"));
    }

    private boolean hasField(Class<?> c, String name, Class<?> type, int requiredMods) {
        if (c == null) return false;
        try {
            Field f = c.getDeclaredField(name);
            return f.getType() == type && (f.getModifiers() & requiredMods) == requiredMods
                    && !Modifier.isPrivate(f.getModifiers()) && !Modifier.isPublic(f.getModifiers()); // ensure protected, not private/public
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasCtor(Class<?> c, Class<?>[] sig) {
        if (c == null) return false;
        try {
            var cons = c.getDeclaredConstructor(sig);
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

    private TestResult test_structure_classesExist() {
        String title = "Classes exist (Employee, Programmer, CustomerService, DepartmentOfficer)";
        Ctx k = ctx();
        boolean ok = k.Employee != null && k.Programmer != null && k.CustomerService != null && k.DepartmentOfficer != null;
        return new TestResult(title, ok,
                ok ? "" : "Could not resolve one or more classes in lab2.classdiagrams.inheritance.", "");
    }

    /* ============================  Structure  ============================ */

    private TestResult test_structure_extendsEmployee() {
        String title = "Subclasses extend Employee";
        Ctx k = ctx();
        boolean ok = k.Employee != null
                && k.Programmer != null && k.Programmer.getSuperclass() == k.Employee
                && k.CustomerService != null && k.CustomerService.getSuperclass() == k.Employee
                && k.DepartmentOfficer != null && k.DepartmentOfficer.getSuperclass() == k.Employee;
        return new TestResult(title, ok,
                ok ? "" : "Ensure the three roles inherit from Employee.", "");
    }

    private TestResult test_structure_employee_fieldsProtected() {
        String title = "Employee fields of correct type and visibility";
        Ctx k = ctx();
        boolean ok = hasField(k.Employee, "hourlySalary", double.class, Modifier.PROTECTED)
                && hasField(k.Employee, "hoursWorked", double.class, Modifier.PROTECTED);
        return new TestResult(title, ok,
                ok ? "" : miss("Verify Employee fields have the correct name/type"), "");
    }

    private TestResult test_structure_programmer_fieldsProtected() {
        String title = "Programmer fields of correct type and visibility";
        Ctx k = ctx();
        boolean ok = hasField(k.Programmer, "bonusPerBug", double.class, Modifier.PROTECTED)
                && hasField(k.Programmer, "numberOfBugs", double.class, Modifier.PROTECTED);
        return new TestResult(title, ok,
                ok ? "" : miss("Verify Programmer fields have the correct name/type"), "");
    }

    private TestResult test_structure_customerservice_fieldsProtected() {
        String title = "CustomerService fields of correct type and visibility";
        Ctx k = ctx();
        // Note: field names in the starter use 'Costumer' spelling; we honor that contract.
        boolean ok = hasField(k.CustomerService, "bonusPerCostumer", double.class, Modifier.PROTECTED)
                && hasField(k.CustomerService, "numberOfCostumers", double.class, Modifier.PROTECTED);
        return new TestResult(title, ok,
                ok ? "" : miss("Verify CustomerService fields have the correct name/type"), "");
    }

    private TestResult test_structure_departmentOfficer_fieldsProtected() {
        String title = "DepartmentOfficer fields of correct type and visibility";
        Ctx k = ctx();
        boolean ok = hasField(k.DepartmentOfficer, "companyBonus", double.class, Modifier.PROTECTED);
        return new TestResult(title, ok,
                ok ? "" : miss("Verify DepartmentOfficer fields have the correct name/type"), "");
    }

    private TestResult test_ctors_signatures() {
        String title = "Constructors have the expected Signatures";
        Ctx k = ctx();
        boolean ok =
                hasCtor(k.Employee, new Class<?>[]{double.class, double.class}) &&
                        hasCtor(k.Programmer, new Class<?>[]{double.class, double.class, double.class, double.class}) &&
                        hasCtor(k.CustomerService, new Class<?>[]{double.class, double.class, double.class, double.class}) &&
                        hasCtor(k.DepartmentOfficer, new Class<?>[]{double.class, double.class, double.class});
        return new TestResult(title, ok,
                ok ? "" : "Check parameter counts/types and visibility.", "");
    }

    /* ============================  API  ============================ */

    private TestResult test_methods_signatures() {
        String title = "Methods have the expected Signatures";
        Ctx k = ctx();
        boolean ok =
                hasPublicNoArgMethod(k.Employee, "calculateDailySalary", double.class) &&
                        hasPublicNoArgMethod(k.Programmer, "calculateDailySalary", double.class) &&
                        hasPublicNoArgMethod(k.CustomerService, "calculateDailySalary", double.class) &&
                        hasPublicNoArgMethod(k.DepartmentOfficer, "calculateDailySalary", double.class);

        return new TestResult(title, ok,
                ok ? "" : "Ensure the method has correct visibility and return type, and that it has no parameters.", "");
    }

    private TestResult test_source_requiresOverride() {
        return SourceChecks.requireOverrides(
                "Methods are Overridden when necessary",
                "lab2.classdiagrams.inheritance",
                Map.of(
                        "Programmer", List.of("calculateDailySalary"),
                        "CustomerService", List.of("calculateDailySalary"),
                        "DepartmentOfficer", List.of("calculateDailySalary")
                )
        );
    }

    private TestResult test_behavior_employeeSalary() {
        String title = "Employee salary calculation";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object e = R.newInstance(k.Employee, new Class<?>[]{double.class, double.class}, new Object[]{10.0, 8.0});
        double got = (e == null) ? Double.NaN : R.callDouble(e, "calculateDailySalary", new Class<?>[]{}, new Object[]{}, Double.NaN);
        boolean ok = almost(got, 80.0);
        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Check base calculation.", R),
                ok ? "" : ("expected≈80.0, actual=" + got));
    }

    /* ============================  Behavior  ============================ */

    private TestResult test_behavior_programmerSalary() {
        String title = "Programmer salary calculation";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object p = R.newInstance(k.Programmer, new Class<?>[]{double.class, double.class, double.class, double.class}, new Object[]{10.0, 8.0, 0.5, 12.0});
        double got = (p == null) ? Double.NaN : R.callDouble(p, "calculateDailySalary", new Class<?>[]{}, new Object[]{}, Double.NaN);
        boolean ok = almost(got, 86.0); // 10*8 + 0.5*12
        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Include the bug bonus on top of base salary.", R),
                ok ? "" : ("expected≈86.0, actual=" + got));
    }

    private TestResult test_behavior_customerServiceSalary() {
        String title = "CustomerService salary calculation";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object c = R.newInstance(k.CustomerService, new Class<?>[]{double.class, double.class, double.class, double.class}, new Object[]{9.0, 8.0, 1.0, 10.0});
        double got = (c == null) ? Double.NaN : R.callDouble(c, "calculateDailySalary", new Class<?>[]{}, new Object[]{}, Double.NaN);
        boolean ok = almost(got, 82.0); // 9*8 + 1*10
        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Include per-customer bonus on top of base salary.", R),
                ok ? "" : ("expected≈82.0, actual=" + got));
    }

    private TestResult test_behavior_departmentOfficerSalary() {
        String title = "DepartmentOfficer salary calculation";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object d = R.newInstance(k.DepartmentOfficer, new Class<?>[]{double.class, double.class, double.class}, new Object[]{11.0, 9.0, 20.0});
        double got = (d == null) ? Double.NaN : R.callDouble(d, "calculateDailySalary", new Class<?>[]{}, new Object[]{}, Double.NaN);
        boolean ok = almost(got, 119.0); // 11*9 + 20
        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Include company bonus on top of base salary.", R),
                ok ? "" : ("expected≈119.0, actual=" + got));
    }

    private TestResult test_behavior_polymorphismSmoke() {
        String title = "Polymorphism smoke test";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object e = R.newInstance(k.Employee, new Class<?>[]{double.class, double.class}, new Object[]{10.0, 8.0});
        Object p = R.newInstance(k.Programmer, new Class<?>[]{double.class, double.class, double.class, double.class}, new Object[]{10.0, 8.0, 0.5, 12.0});
        Object cs = R.newInstance(k.CustomerService, new Class<?>[]{double.class, double.class, double.class, double.class}, new Object[]{9.0, 8.0, 1.0, 10.0});
        Object dof = R.newInstance(k.DepartmentOfficer, new Class<?>[]{double.class, double.class, double.class}, new Object[]{11.0, 9.0, 20.0});
        double s0 = (e == null) ? Double.NaN : R.callDouble(e, "calculateDailySalary", new Class<?>[]{}, new Object[]{}, Double.NaN);
        double s1 = (p == null) ? Double.NaN : R.callDouble(p, "calculateDailySalary", new Class<?>[]{}, new Object[]{}, Double.NaN);
        double s2 = (cs == null) ? Double.NaN : R.callDouble(cs, "calculateDailySalary", new Class<?>[]{}, new Object[]{}, Double.NaN);
        double s3 = (dof == null) ? Double.NaN : R.callDouble(dof, "calculateDailySalary", new Class<?>[]{}, new Object[]{}, Double.NaN);

        boolean ok = almost(s0, 80.0) && almost(s1, 86.0) && almost(s2, 82.0) && almost(s3, 119.0);
        return new TestResult(title, ok,
                ok ? "" : "Upcasting should still dispatch to subclass implementations.", "");
    }

    private static final class Ctx {
        final Class<?> Employee, Programmer, CustomerService, DepartmentOfficer;

        Ctx(Class<?> e, Class<?> p, Class<?> cs, Class<?> d) {
            Employee = e;
            Programmer = p;
            CustomerService = cs;
            DepartmentOfficer = d;
        }
    }
}