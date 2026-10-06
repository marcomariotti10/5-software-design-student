package be.uantwerpen.sd.labs.grader;

import be.uantwerpen.sd.labs.grader.core.CompositeLabGrader;
import be.uantwerpen.sd.labs.grader.core.LabGrader;
import be.uantwerpen.sd.labs.grader.core.SuiteRunner;
import be.uantwerpen.sd.labs.grader.lab1.Lab1Grader;
import be.uantwerpen.sd.labs.grader.lab2.Lab2GraderAbstractions;
import be.uantwerpen.sd.labs.grader.lab2.Lab2GraderInheritance;
import be.uantwerpen.sd.labs.grader.lab2.Lab2GraderInterfaces;
import be.uantwerpen.sd.labs.grader.lab3.Lab3GraderInventory;
import be.uantwerpen.sd.labs.grader.lab3.Lab3GraderObserver;
import be.uantwerpen.sd.labs.grader.lab3.Lab3GraderSingleton;

import java.util.LinkedHashMap;
import java.util.Map;

public class GraderMain {
    // Register all lab graders with simple keys: "lab1", "lab2", ...
    private static final Map<String, LabGrader> LABS = new LinkedHashMap<>();

    static {
        LABS.put("lab1", new Lab1Grader());

        LinkedHashMap<String, LabGrader> parts2 = new LinkedHashMap<>();
        parts2.put("Inheritance", new Lab2GraderInheritance());
        parts2.put("Abstractions", new Lab2GraderAbstractions());
        parts2.put("Interfaces", new Lab2GraderInterfaces());
        LABS.put("lab2", new CompositeLabGrader("Lab 2", parts2));

        LinkedHashMap<String, LabGrader> parts3 = new LinkedHashMap<>();
        parts3.put("Singleton", new Lab3GraderSingleton());
        parts3.put("Observer", new Lab3GraderObserver());
        parts3.put("Inventory", new Lab3GraderInventory());
        LABS.put("lab3", new CompositeLabGrader("Lab 3", parts3));
    }

    public static void main(String[] args) {
        boolean noColor = false;
        boolean compact = false;
        boolean showStudentOut = false; // NEW flag
        String labArg = null;

        // Parse args: keep your --lab handling intact, add soft flags
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--lab=")) {
                labArg = a.substring("--lab=".length()).trim();
                continue;
            }
            if (a.equals("--lab") && i + 1 < args.length) {
                labArg = args[++i].trim();
                continue;
            }
            if (a.equals("--no-color")) noColor = true;
            if (a.equals("--compact") || a.equals("-q")) compact = true;
            if (a.equals("--show-student-output")) showStudentOut = true; // NEW
        }

        SuiteRunner runner = new SuiteRunner()
                .withColor(!noColor)
                .withCompact(compact)
                .withSilencedStudentOutput(!showStudentOut);  // NEW

        if (labArg == null || labArg.isEmpty()) {
            int grandPassed = 0, grandTotal = 0;
            java.util.LinkedHashMap<String, LabGrader.Result> rows = new java.util.LinkedHashMap<>();

            for (var e : LABS.entrySet()) {
                System.out.println("\n============================");
                System.out.println("Grading " + e.getKey());
                System.out.println("============================");
                var res = e.getValue().run(runner);
                rows.put(e.getKey(), res);
                grandPassed += res.passed;
                grandTotal += res.total;
            }

            // Pretty final summary table
            System.out.println("\n>>> Summary");
            System.out.println("Lab    | Passed | Total | Percent | Progress");
            System.out.println("-------+--------+-------+---------+----------------------");
            for (var e : rows.entrySet()) {
                var r = e.getValue();
                int pct = (r.total == 0) ? 0 : (int) Math.round(100.0 * r.passed / r.total);
                String bar = "[" + "#".repeat((int) Math.round(20.0 * r.passed / Math.max(1, r.total)))
                        + "-".repeat(20 - (int) Math.round(20.0 * r.passed / Math.max(1, r.total))) + "]";
                System.out.printf("%-6s | %6d | %5d | %7d%% | %s%n", e.getKey(), r.passed, r.total, pct, bar);
            }
            System.out.printf("%n>>> Total: %d/%d tests passed%n", grandPassed, grandTotal);
        } else {
            LabGrader grader = LABS.get(labArg);
            if (grader == null) {
                System.err.println("Unknown lab '" + labArg + "'. Available: " + String.join(", ", LABS.keySet()));
                System.exit(2);
            }
            grader.run(runner);
        }
    }
}
