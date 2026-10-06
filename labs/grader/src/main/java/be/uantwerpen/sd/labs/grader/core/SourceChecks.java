package be.uantwerpen.sd.labs.grader.core;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceChecks {
    // Configure JavaParser once (keep consistent across all tests)
    static {
        StaticJavaParser.setConfiguration(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
        );
    }

    private SourceChecks() {
    }

    /**
     * Robustly find the Java src root for the given subpackage (e.g. "lab2.classdiagrams.interfaces").
     * Works whether the CWD is repo root, solutions/, solutions/grader, labs/, labs/grader, etc.
     */
    public static Path findJavaSrcRoot(String subpkg) {
        // Extract "labN" from the subpackage (e.g. "lab2" from "lab2.classdiagrams.interfaces")
        String lab = extractLabFolder(subpkg); // e.g., "lab2"
        if (lab == null) return null;

        String labs = ("labs/" + lab + "/src/main/java").replace("/", File.separator);
        String solns = ("solutions/" + lab + "/src/main/java").replace("/", File.separator);

        // Walk upwards from the current dir to find either labs/<lab>/src/main/java or solutions/<lab>/src/main/java
        Path start = Paths.get("").toAbsolutePath();
        for (Path cur = start; cur != null; cur = cur.getParent()) {
            Path p1 = cur.resolve(labs);
            if (Files.isDirectory(p1)) return p1;
            Path p2 = cur.resolve(solns);
            if (Files.isDirectory(p2)) return p2;
        }
        return null;
    }

    private static String extractLabFolder(String subpkg) {
        // matches "lab" + digits at start of the subpackage or after dots: e.g. "lab3.observer"
        Matcher m = Pattern.compile("(lab\\d+)").matcher(subpkg);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Parse a class file under base package path (slashes) and return first method with the given name, if any.
     */
    private static Optional<MethodDeclaration> findFirstMethod(Path srcRoot, String basePkgSlashed, String simpleName, String method) {
        try {
            Path file = srcRoot.resolve((basePkgSlashed + "/" + simpleName + ".java").replace("/", File.separator));
            if (!Files.exists(file)) return Optional.empty();
            var cu = StaticJavaParser.parse(file);
            Optional<ClassOrInterfaceDeclaration> cls = cu.getClassByName(simpleName);
            if (cls.isEmpty()) return Optional.empty();
            return cls.get().getMethodsByName(method).stream().findFirst();
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    /**
     * True iff @Override is present on the named method in either labs/** or solutions/** trees for the given subpackage.
     */
    public static boolean hasOverride(Path srcRoot, String subpkg, String simpleName, String method) {
        String labsPkg = "be/uantwerpen/sd/labs/" + subpkg.replace('.', '/');
        String solnPkg = "be/uantwerpen/sd/solutions/" + subpkg.replace('.', '/');

        return findFirstMethod(srcRoot, labsPkg, simpleName, method)
                .map(m -> m.getAnnotationByName("Override").isPresent()).orElse(false)
                || findFirstMethod(srcRoot, solnPkg, simpleName, method)
                .map(m -> m.getAnnotationByName("Override").isPresent()).orElse(false);
    }

    /**
     * Convenience: build a TestResult that FAILS if any listed method lacks @Override.
     * - Never throws; returns a failing TestResult with detailed explanation instead.
     * - Does not "gracefully pass": if we can’t locate sources or annotations are missing, test FAILS.
     */
    public static TestResult requireOverrides(String title, String subpkg, Map<String, List<String>> methodsByClass) {
        Path root = findJavaSrcRoot(subpkg);
        if (root == null) {
            return new TestResult(title, false,
                    "Could not locate Java sources for '" + subpkg + "'. " +
                            "Run from repo root OR keep default layout labs/<lab>/src/main/java or solutions/<lab>/src/main/java.",
                    "CWD=" + Paths.get("").toAbsolutePath());
        }

        List<String> missing = new ArrayList<>();
        for (var e : methodsByClass.entrySet()) {
            String simple = e.getKey();
            for (String method : e.getValue()) {
                boolean ok = hasOverride(root, subpkg, simple, method);
                if (!ok) missing.add(simple + "." + method + "()");
            }
        }

        boolean pass = missing.isEmpty();
        String hint = pass ? "" : ("Add @Override to: " + String.join(", ", missing));
        String detail = pass ? "" :
                ("Looked under " + root + " in both be/uantwerpen/sd/labs/" + subpkg +
                        " and be/uantwerpen/sd/solutions/" + subpkg + ".");

        return new TestResult(title, pass, hint, detail);
    }
}
