# 2 · Java Reflection and Annotations: unit testing with arguments

Exercise 2 of the first assignment. The **solution format** it requires: *"A well-commented RunTests.java file."* The assignment also gives the text of the two annotations `@Testable` and `@Specification`, to be copied into suitable files.

The tool is described in the [main README](../README.md#2--java-reflection-and-annotations-unit-testing-with-arguments) and in Section 3 of the [report](../REPORT.pdf).

## Files

| File | Role | What it is |
|:---|:---|:---|
| `src/RunTests.java` | **solution** | the unit-testing tool |
| `src/Testable.java`, `src/Specification.java` | **solution** | the two annotations, with the text given by the assignment |
| `src/Report.java` | provided with the assignment | prints the outcome of each test; the assignment requires `RunTests` to use it unchanged, so `RunTests.java` does not compile without it |
| `examples/MathOpsTests_simple.java`, `examples/MathOpsTests.java` | provided with the assignment | the classes to test, with their methods annotated |
| `expected/RunTests_MathOpsTests_simple.output`, `expected/RunTests_MathOpsTests.output` | provided with the assignment | the expected output of `RunTests` on each of the two classes |

The provided files are included unchanged (their comments are in Italian, as distributed), so that anyone can run the tool and compare its output with the expected one.

## Run and verify

```bash
javac -d out src/*.java examples/*.java
java -cp out RunTests MathOpsTests
```

The first command compiles the tool, the annotations, `Report` and the two test classes into the folder `out`; the second one runs `RunTests` on the class `MathOpsTests`. To check that the output is identical to the expected one (`diff` prints nothing when two outputs coincide):

```bash
java -cp out RunTests MathOpsTests | diff - expected/RunTests_MathOpsTests.output && echo identical
java -cp out RunTests MathOpsTests_simple | diff - expected/RunTests_MathOpsTests_simple.output && echo identical
```
