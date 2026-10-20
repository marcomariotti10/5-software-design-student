package be.uantwerpen.sd.labs.grader.core;

public final class TestResult {
    public final String name;
    public final boolean pass;
    public final String hint;
    public final String detail;

    public TestResult(String name, boolean pass, String hint) {
        this(name, pass, hint, "");
    }

    public TestResult(String name, boolean pass, String hint, String detail) {
        this.name = name;
        this.pass = pass;
        this.hint = hint;
        this.detail = detail;
    }
}
