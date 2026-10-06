package be.uantwerpen.sd.labs.grader.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class SafeRef {
    // Try both roots so the same grader works before and after export.
    private final String[] roots = new String[]{
            "be.uantwerpen.sd.labs",        // student tree (after export)
            "be.uantwerpen.sd.solutions"    // teacher tree (this repo)
    };

    public String lastError = "";

    /**
     * Resolve a class by lab key and simple name, e.g. ("lab1","Booking").
     */
    public Class<?> cls(String lab, String simpleName) {
        lastError = "";
        for (String root : roots) {
            String fqn = root + "." + lab + "." + simpleName;
            try {
                return Class.forName(fqn);
            } catch (Throwable ignore) {
                // try next root
            }
        }
        lastError = simpleName + " class not found in lab '" + lab + "'";
        return null;
    }

    public Object newInstance(Class<?> c, Class<?>[] sig, Object[] args) {
        lastError = "";
        if (c == null) return null;
        try {
            Constructor<?> cons = c.getDeclaredConstructor(sig);
            cons.setAccessible(true);
            return cons.newInstance(args);
        } catch (Throwable t) {
            lastError = "Failed to construct " + c.getSimpleName() + ": " +
                    t.getClass().getSimpleName() + ": " + t.getMessage();
            return null;
        }
    }

    public Object call(Object target, String method, Class<?>[] sig, Object[] args) {
        lastError = "";
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(method, sig);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (NoSuchMethodException e) {
            lastError = "Missing method '" + method + "' on " + target.getClass().getSimpleName();
            return null;
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            lastError = target.getClass().getSimpleName() + "." + method + " threw " +
                    t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "(no message)" : t.getMessage());
            return null;
        } catch (Throwable t) {
            lastError = "Failed calling " + method + " on " + target.getClass().getSimpleName() + ": " +
                    t.getClass().getSimpleName() + ": " + t.getMessage();
            return null;
        }
    }

    public int callInt(Object target, String method, Class<?>[] sig, Object[] args, int fallback) {
        lastError = "";
        Object o = call(target, method, sig, args);
        if (o == null) return fallback;
        try {
            return ((Number) o).intValue();
        } catch (Throwable t) {
            lastError = "Expected int result from '" + method + "'";
            return fallback;
        }
    }

    public double callDouble(Object target, String method, Class<?>[] sig, Object[] args, double fallback) {
        lastError = "";
        Object o = call(target, method, sig, args);
        if (o == null) return fallback;
        try {
            return ((Number) o).doubleValue();
        } catch (Throwable t) {
            lastError = "Expected double result from '" + method + "'";
            return fallback;
        }
    }

    public long callLong(Object target, String method, Class<?>[] sig, Object[] args, long fallback) {
        lastError = "";
        Object o = call(target, method, sig, args);
        if (o == null) return fallback;
        try {
            return ((Number) o).longValue();
        } catch (Throwable t) {
            lastError = "Expected long result from '" + method + "'";
            return fallback;
        }
    }

}
