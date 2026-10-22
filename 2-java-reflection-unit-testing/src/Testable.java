import java.lang.annotation.*;

/** Marks a method to be tested by {@code RunTests} (definition given by the assignment). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Testable {}
