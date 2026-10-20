package be.uantwerpen.sd.labs.grader.core;

public interface LabGrader {
    Result run(SuiteRunner runner);

    final class Result {
        public final int passed, total;

        public Result(int passed, int total) {
            this.passed = passed;
            this.total = total;
        }
    }
}
