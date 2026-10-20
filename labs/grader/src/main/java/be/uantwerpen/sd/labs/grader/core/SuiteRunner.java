package be.uantwerpen.sd.labs.grader.core;

public final class SuiteRunner {

    // --- New config flags (with sane defaults) ---
    private boolean color = !"false".equalsIgnoreCase(System.getProperty("grader.color", "true"))
            && System.getenv("NO_COLOR") == null;
    private boolean compact = false;

    // NEW: mute student stdout by default inside tests
    private boolean silenceStudentStdout = true;

    private static String rpt(char ch, int n) {
        return (n <= 0) ? "" : String.valueOf(ch).repeat(n);
    }

    private static String bar(int passed, int total, int width) {
        if (total <= 0) return "[" + rpt('░', width) + "]";
        int fill = (int) Math.round((passed * 1.0 / total) * width);
        return "[" + rpt('█', fill) + rpt('░', Math.max(0, width - fill)) + "]";
    }

    public SuiteRunner withColor(boolean enabled) {
        this.color = enabled;
        return this;
    }

    public SuiteRunner withCompact(boolean enabled) {
        this.compact = enabled;
        return this;
    }

    public SuiteRunner withSilencedStudentOutput(boolean enabled) {
        this.silenceStudentStdout = enabled;
        return this;
    }

    // --- Tiny helpers (no extra deps) ---
    private String c(String code, String s) {
        return color ? code + s + "\u001B[0m" : s;
    }

    private String green(String s) {
        return c("\u001B[32m", s);
    }

    private String red(String s) {
        return c("\u001B[31m", s);
    }

    private String yellow(String s) {
        return c("\u001B[33m", s);
    }

    private String dim(String s) {
        return c("\u001B[2m", s);
    }

    private String bold(String s) {
        return c("\u001B[1m", s);
    }

    private void boxTitle(String title) {
        String line = rpt('─', Math.max(8, title.length() + 2));
        System.out.println("\n" + dim("┌" + line + "┐"));
        System.out.println(dim("│ ") + bold(title) + dim(" │"));
        System.out.println(dim("└" + line + "┘"));
    }

    /**
     * Prints one grouped test suite with its own header and summary; returns number passed.
     */
    public int runSuite(String title, java.util.List<java.util.function.Supplier<TestResult>> tests) {
        boxTitle(title);
        int passed = 0, idx = 1;
        java.util.List<String> failed = new java.util.ArrayList<>();

        for (java.util.function.Supplier<TestResult> s : tests) {
            java.io.PrintStream prevOut = System.out;
            try {
                if (silenceStudentStdout) {
                    System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
                }
                long t0 = System.nanoTime();
                TestResult r = s.get();
                long ms = (System.nanoTime() - t0) / 1_000_000;
                System.setOut(prevOut); // restore before we print our own line

                if (!compact || !r.pass) printOne(r, idx, ms);
                if (r.pass) passed++;
                else failed.add(String.format("%d) %s", idx, r.name));
            } finally {
                // always restore even if a test throws
                System.setOut(prevOut);
            }
            idx++;
        }

        String badge = (passed == tests.size()) ? green("✅") : red("❌");
        String summary = String.format("%s %d/%d passed %s",
                badge, passed, tests.size(), dim(bar(passed, tests.size(), 20)));

        System.out.println("\n" + summary);
        if (failed.isEmpty()) {
            System.out.println("\t" + green("✓ All checks for this suite passed. You can proceed to the next suite."));
        } else {
            System.out.println("\t" + yellow("• Some checks failed. Fix these before moving on."));
            System.out.println(dim("\t  Failed: " + String.join(" | ", failed)));
        }
        return passed;
    }

    /**
     * Prints a single test result line (with timing).
     */
    private void printOne(TestResult r, int idx, long ms) {
        String icon = r.pass ? green("✅") : red("❌");
        System.out.printf("%2d) %s %s %s%n", idx, icon, r.name, dim(String.format("(+%d ms)", ms)));
        if (!r.pass && r.hint != null && !r.hint.isEmpty()) {
            System.out.println("    " + yellow("» ") + r.hint);
        }
        if (!r.pass && r.detail != null && !r.detail.isEmpty()) {
            System.out.println("    " + dim("· Detail: " + r.detail));
        }
    }

    /**
     * Prints a single test result line.
     */
    public void printOne(TestResult r, int idx) {
        String icon = r.pass ? "✅" : "❌";
        System.out.printf("%2d) %s %s\n", idx, icon, r.name);
        if (!r.pass && r.hint != null && !r.hint.isEmpty()) {
            System.out.println("    » " + r.hint);
        }
        if (!r.pass && r.detail != null && !r.detail.isEmpty()) {
            System.out.println("    · Detail: " + r.detail);
        }
    }

    public TestResult assertEquals(String name, int expected, int actual, String hint) {
        boolean pass = expected == actual;
        String detail = pass ? "" : ("expected=" + expected + ", actual=" + actual);
        return new TestResult(name, pass, pass ? "" : shortHint(hint), detail);
    }

    /**
     * Keep hints short & not too revealing.
     */
    public String shortHint(String s) {
        return s;
    }

    /**
     * Combine a conceptual hint with SafeRef.lastError, if present.
     */
    public String withReflection(String baseHint, SafeRef R) {
        String refl = (R != null && R.lastError != null && !R.lastError.isEmpty())
                ? (" Reflection: " + R.lastError) : "";
        if (baseHint == null) baseHint = "";
        if (baseHint.isEmpty()) return refl.isEmpty() ? "" : refl.substring(1); // drop leading space if reflection only
        return baseHint + refl;
    }
}
