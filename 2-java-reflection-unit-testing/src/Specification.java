import java.lang.annotation.*;

/**
 * Describes how {@code RunTests} must invoke a {@link Testable} method and which result to expect
 * (definition given by the assignment). Types are "int", "double", "bool" or "string"; values are
 * given as strings; an empty {@code resType} means that the method returns nothing ({@code void}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Specification {
    String[] argTypes() default {};
    String[] argValues() default {};
    String resType() default "";
    String resVal() default "";
}
