package be.uantwerpen.sd.labs.grader.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class CompositeLabGrader implements LabGrader {
    private final String title;
    private final LinkedHashMap<String, LabGrader> parts;

    public CompositeLabGrader(String title, LinkedHashMap<String, LabGrader> parts) {
        this.title = title;
        this.parts = new LinkedHashMap<>(parts);
    }

    @Override
    public Result run(SuiteRunner runner) {
        // Cleaner, single lab header (no “Composite” wording)
        System.out.println("\n=== " + title + " — Auto-Grader ===\n");

        int total = 0, passed = 0;

        for (Map.Entry<String, LabGrader> e : parts.entrySet()) {
            String label = e.getKey();
            LabGrader grader = e.getValue();

            // Capture output from the part grader and filter duplicate banners/summaries
            PrintStream prevOut = System.out;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try {
                System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
                LabGrader.Result r = grader.run(runner);
                System.setOut(prevOut);

                // Print a slim section header for the part
                System.out.println("\n-- " + label + " --");

                // Filter out noisy lines like:
                // "=== Lab 2 (Inheritance) — Auto-Grader ==="
                // "=== Overall Summary Lab 2 (Inheritance): ..."
                String out = buf.toString(StandardCharsets.UTF_8);
                String[] lines = out.split("\\R");

                Pattern partHeader = Pattern.compile("^===\\s*" + Pattern.quote(title)
                        + "\\s*\\([^)]+\\)\\s*—\\s*Auto-Grader\\s*===\\s*$");
                Pattern partSummary = Pattern.compile("^===\\s*Overall Summary\\s*" + Pattern.quote(title)
                        + "\\s*\\([^)]+\\):.*$");

                String prevLine = null;
                for (String line : lines) {
                    if (partHeader.matcher(line).matches()) continue;
                    if (partSummary.matcher(line).matches()) continue;
                    // collapse extra blank lines caused by filtering
                    if (line.isBlank() && (prevLine == null || prevLine.isBlank())) continue;
                    System.out.println(line);
                    prevLine = line;
                }

                // One-line part summary
                System.out.printf("\n\n  %s — %d/%d passed%n\n", label, r.passed, r.total);

                passed += r.passed;
                total += r.total;
            } finally {
                System.setOut(prevOut);
            }
        }

        System.out.printf("\n=== Overall Summary %s: %d/%d tests passed ===\n", title, passed, total);
        return new Result(passed, total);
    }
}
