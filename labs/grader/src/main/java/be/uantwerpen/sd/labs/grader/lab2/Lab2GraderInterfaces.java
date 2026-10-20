package be.uantwerpen.sd.labs.grader.lab2;

import be.uantwerpen.sd.labs.grader.core.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Lab2GraderInterfaces implements LabGrader {

    // Resolve against both student & teacher trees (works pre/post export)
    private static final String[] ROOTS = {"be.uantwerpen.sd.labs", "be.uantwerpen.sd.solutions"};
    private static final String SUBPKG = "lab2.classdiagrams.interfaces";
    private SuiteRunner runner;

    @Override
    public Result run(SuiteRunner runner) {
        this.runner = runner;

        int total = 0, passed = 0;

        // ---------- Structure ----------
        List<Supplier<TestResult>> s1 = new ArrayList<>();
        s1.add(this::test_structure_typesExist);
        s1.add(this::test_structure_devicesImplementInterface);
        s1.add(this::test_structure_device_volumeFieldPrivateInt);
        s1.add(this::test_structure_universalRemote_devicesFieldPrivateList);
        passed += runner.runSuite("Structure (types, interface implementation, modifiers)", s1);
        total += s1.size();

        // ---------- API ----------
        List<Supplier<TestResult>> s2 = new ArrayList<>();
        s2.add(this::test_api_interface_methodsExist);
        s2.add(this::test_api_devices_methodsSignaturesAndDeclaredHere);
        s2.add(this::test_api_universalRemote_methodsSignatures);
        s2.add(this::test_source_requiresOverrideAnnotations); // best-effort source inspection
        passed += runner.runSuite("API (method signatures & @Override presence)", s2);
        total += s2.size();

        // ---------- Behavior ----------
        List<Supplier<TestResult>> s3 = new ArrayList<>();
        s3.add(this::test_behavior_device_tv);
        s3.add(this::test_behavior_device_radio);
        s3.add(this::test_behavior_device_cdplayer);
        s3.add(this::test_behavior_universalRemote_affectsAllDevices);
        passed += runner.runSuite("Behavior (state changes & polymorphism via remote)", s3);
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

    /* ============================  Helpers  ============================ */

    private Ctx ctx() {
        return new Ctx(
                cls("VolumeDevice"),
                cls("TV"),
                cls("Radio"),
                cls("CDPlayer"),
                cls("UniversalRemote")
        );
    }

    private boolean hasFieldExactPrivateInt(Class<?> c, String name) {
        if (c == null) return false;
        try {
            Field f = c.getDeclaredField(name);
            int m = f.getModifiers();
            return f.getType() == int.class
                    && Modifier.isPrivate(m)
                    && !Modifier.isStatic(m)
                    && !Modifier.isFinal(m);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasPrivateListField(Class<?> c, String name) {
        if (c == null) return false;
        try {
            Field f = c.getDeclaredField(name);
            int m = f.getModifiers();
            return (f.getType() == java.util.List.class || java.util.List.class.isAssignableFrom(f.getType()))
                    && Modifier.isPrivate(m);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasPublicNoArgVoidMethod(Class<?> c, String name) {
        if (c == null) return false;
        try {
            Method m = c.getDeclaredMethod(name);
            return Modifier.isPublic(m.getModifiers()) && m.getReturnType() == void.class && m.getParameterCount() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean interfaceHasNoArgVoidMethod(Class<?> c, String name) {
        if (c == null) return false;
        try {
            Method m = c.getDeclaredMethod(name);
            // In an interface, methods are public abstract by default
            return m.getReturnType() == void.class && m.getParameterCount() == 0;
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

    private Integer getVolume(Object device) {
        if (device == null) return null;
        try {
            Field f = device.getClass().getDeclaredField("volume");
            f.setAccessible(true);
            return (Integer) f.get(device);
        } catch (Throwable t) {
            return null;
        }
    }

    private void setVolume(Object device, int v) {
        if (device == null) return;
        try {
            Field f = device.getClass().getDeclaredField("volume");
            f.setAccessible(true);
            f.set(device, v);
        } catch (Throwable ignored) {
        }
    }

    private TestResult test_structure_typesExist() {
        String title = "Types exist (VolumeDevice, TV, Radio, CDPlayer, UniversalRemote)";
        Ctx k = ctx();
        boolean ok = k.VolumeDevice != null && k.TV != null && k.Radio != null && k.CDPlayer != null && k.UniversalRemote != null;
        return new TestResult(title, ok,
                ok ? "" : "Could not resolve one or more types in lab2.classdiagrams.interfaces.", "");
    }

    /* ============================  Structure  ============================ */

    private TestResult test_structure_devicesImplementInterface() {
        String title = "Devices implement VolumeDevice";
        Ctx k = ctx();
        boolean ok =
                (k.VolumeDevice != null && k.TV != null && k.VolumeDevice.isAssignableFrom(k.TV)) &&
                        (k.VolumeDevice != null && k.Radio != null && k.VolumeDevice.isAssignableFrom(k.Radio)) &&
                        (k.VolumeDevice != null && k.CDPlayer != null && k.VolumeDevice.isAssignableFrom(k.CDPlayer));
        return new TestResult(title, ok,
                ok ? "" : "Ensure TV, Radio, and CDPlayer implement the VolumeDevice interface.", "");
    }

    private TestResult test_structure_device_volumeFieldPrivateInt() {
        String title = "Correct Device field volume type/visibility";
        Ctx k = ctx();
        boolean ok =
                hasFieldExactPrivateInt(k.TV, "volume")
                        && hasFieldExactPrivateInt(k.Radio, "volume")
                        && hasFieldExactPrivateInt(k.CDPlayer, "volume");
        return new TestResult(title, ok,
                ok ? "" : "Each device must have a field named 'volume' of the correct type/visibility.", "");
    }

    private TestResult test_structure_universalRemote_devicesFieldPrivateList() {
        String title = "Universal Remote correctly defines devices field.";
        Ctx k = ctx();
        boolean ok = hasPrivateListField(k.UniversalRemote, "devices");
        return new TestResult(title, ok,
                ok ? "" : "Define a field named 'devices' holding VolumeDevice items in a List. Ensure correct visibility.", "");
    }

    private TestResult test_api_interface_methodsExist() {
        String title = "Interface methods exist";
        Ctx k = ctx();
        boolean ok =
                interfaceHasNoArgVoidMethod(k.VolumeDevice, "volumeUp") &&
                        interfaceHasNoArgVoidMethod(k.VolumeDevice, "volumeDown");
        return new TestResult(title, ok,
                ok ? "" : "Declare both methods (no args, void) on VolumeDevice.", "");
    }

    /* ============================  API  ============================ */

    private TestResult test_api_devices_methodsSignaturesAndDeclaredHere() {
        String title = "All devices have volumeUp()/volumeDown() declared in class";
        Ctx k = ctx();
        boolean tv = hasPublicNoArgVoidMethod(k.TV, "volumeUp") && hasPublicNoArgVoidMethod(k.TV, "volumeDown")
                && declaresMethodHere(k.TV, "volumeUp") && declaresMethodHere(k.TV, "volumeDown");
        boolean ra = hasPublicNoArgVoidMethod(k.Radio, "volumeUp") && hasPublicNoArgVoidMethod(k.Radio, "volumeDown")
                && declaresMethodHere(k.Radio, "volumeUp") && declaresMethodHere(k.Radio, "volumeDown");
        boolean cd = hasPublicNoArgVoidMethod(k.CDPlayer, "volumeUp") && hasPublicNoArgVoidMethod(k.CDPlayer, "volumeDown")
                && declaresMethodHere(k.CDPlayer, "volumeUp") && declaresMethodHere(k.CDPlayer, "volumeDown");
        boolean ok = tv && ra && cd;
        return new TestResult(title, ok,
                ok ? "" : "Each concrete device must implement both methods of the VolumeDevice interface.", "");
    }

    private TestResult test_api_universalRemote_methodsSignatures() {
        String title = "UniversalRemote signatures: addDevice, lowerVolume, riseVolume";
        Ctx k = ctx();
        if (k.UniversalRemote == null || k.VolumeDevice == null) {
            return new TestResult("UniversalRemote API", false, "Missing UniversalRemote or VolumeDevice.", "");
        }
        boolean add = false;
        try {
            Method m = k.UniversalRemote.getDeclaredMethod("addDevice", k.VolumeDevice);
            add = Modifier.isPublic(m.getModifiers()) && m.getReturnType() == void.class;
        } catch (Throwable t) {
            add = false;
        }

        boolean low = hasPublicNoArgVoidMethod(k.UniversalRemote, "lowerVolume");
        boolean rise = hasPublicNoArgVoidMethod(k.UniversalRemote, "riseVolume");

        boolean ok = add && low && rise;
        return new TestResult(title, ok,
                ok ? "" : "Expose addDevice, lowerVolume, and riseVolume methods. Ensure correct Visibility and declaration.", "");
    }

    private TestResult test_source_requiresOverrideAnnotations() {
        return SourceChecks.requireOverrides(
                "@Override present on device methods",
                "lab2.classdiagrams.interfaces",
                Map.of(
                        "TV", List.of("volumeUp", "volumeDown"),
                        "Radio", List.of("volumeUp", "volumeDown"),
                        "CDPlayer", List.of("volumeUp", "volumeDown")
                )
        );
    }

    private TestResult test_behavior_device_tv() {
        return checkDeviceUpDown("TV");
    }

    /* ============================  Behavior  ============================ */

    private TestResult test_behavior_device_radio() {
        return checkDeviceUpDown("Radio");
    }

    private TestResult test_behavior_device_cdplayer() {
        return checkDeviceUpDown("CDPlayer");
    }

    private TestResult checkDeviceUpDown(String simpleName) {
        String title = ": volumeUp() increments, volumeDown() decrements";
        SafeRef R = new SafeRef();
        Class<?> C = R.cls("lab2.classdiagrams.interfaces", simpleName);
        Object d = R.newInstance(C, new Class<?>[]{}, new Object[]{});
        Integer before = getVolume(d);
        boolean step1 = (d != null) && R.call(d, "volumeUp", new Class<?>[]{}, new Object[]{}) != null;
        Integer afterUp = getVolume(d);
        boolean step2 = (d != null) && R.call(d, "volumeDown", new Class<?>[]{}, new Object[]{}) != null;
        Integer afterBack = getVolume(d);
        boolean step3 = (d != null) && R.call(d, "volumeDown", new Class<?>[]{}, new Object[]{}) != null;
        Integer afterDown = getVolume(d);

        boolean ok = before != null && afterUp != null && afterBack != null && afterDown != null
                && (afterUp == before + 1)
                && (afterBack == before)
                && (afterDown == before - 1);

        return new TestResult(
                simpleName + title,
                ok,
                ok ? "" : runner.withReflection("Ensure volume field updates on each call.", R),
                ok ? "" : ("before=" + before + ", afterUp=" + afterUp + ", afterBack=" + afterBack + ", afterDown=" + afterDown)
        );
    }

    private TestResult test_behavior_universalRemote_affectsAllDevices() {
        String title = "UniversalRemote affects all devices";
        Ctx k = ctx();
        SafeRef R = new SafeRef();
        Object remote = R.newInstance(k.UniversalRemote, new Class<?>[]{}, new Object[]{});
        Object tv = R.newInstance(k.TV, new Class<?>[]{}, new Object[]{});
        Object ra = R.newInstance(k.Radio, new Class<?>[]{}, new Object[]{});
        Object cd = R.newInstance(k.CDPlayer, new Class<?>[]{}, new Object[]{});

        // Set known starting volumes
        setVolume(tv, 10);
        setVolume(ra, 20);
        setVolume(cd, 30);

        // Add all devices
        if (remote != null) {
            R.call(remote, "addDevice", new Class<?>[]{k.VolumeDevice}, new Object[]{tv});
            R.call(remote, "addDevice", new Class<?>[]{k.VolumeDevice}, new Object[]{ra});
            R.call(remote, "addDevice", new Class<?>[]{k.VolumeDevice}, new Object[]{cd});
        }

        // lowerVolume() should decrement each
        if (remote != null) R.call(remote, "lowerVolume", new Class<?>[]{}, new Object[]{});
        Integer tvL = getVolume(tv), raL = getVolume(ra), cdL = getVolume(cd);

        // riseVolume() should increment each (back to original values)
        if (remote != null) R.call(remote, "riseVolume", new Class<?>[]{}, new Object[]{});
        Integer tvR = getVolume(tv), raR = getVolume(ra), cdR = getVolume(cd);

        boolean ok = tvL != null && raL != null && cdL != null && tvR != null && raR != null && cdR != null
                && tvL == 9 && raL == 19 && cdL == 29
                && tvR == 10 && raR == 20 && cdR == 30;

        return new TestResult(title, ok,
                ok ? "" : runner.withReflection("Ensure the remote iterates all devices and calls the right methods.", R),
                ok ? "" : ("after lower: tv=" + tvL + ", ra=" + raL + ", cd=" + cdL + " | after rise: tv=" + tvR + ", ra=" + raR + ", cd=" + cdR));
    }

    private static final class Ctx {
        final Class<?> VolumeDevice, TV, Radio, CDPlayer, UniversalRemote;

        Ctx(Class<?> vd, Class<?> tv, Class<?> r, Class<?> cd, Class<?> ur) {
            VolumeDevice = vd;
            TV = tv;
            Radio = r;
            CDPlayer = cd;
            UniversalRemote = ur;
        }
    }
}
