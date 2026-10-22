/**
 * Unit testing with annotations - RunTests
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small unit-testing tool built on the Java Reflection API.
 * <p>
 * {@code java RunTests MyClass} loads {@code MyClass}, creates an instance {@code t} with the
 * public no-argument constructor, and invokes on {@code t} every non-private instance method
 * annotated with {@link Testable}, using the arguments and the expected result described by its
 * {@link Specification}. For each of these methods {@link Report#report} is called exactly once,
 * with one of four outcomes:
 * <ol>
 *   <li>{@code WrongArgs}: the method cannot be invoked with the given arguments (lengths that do
 *       not match the parameter list, types that do not correspond, values that cannot be converted);</li>
 *   <li>{@code WrongResultType}: the arguments are fine but the expected result is not (a type
 *       different from the return type, or a value that cannot be converted);</li>
 *   <li>{@code TestFailed}: the method was invoked but did not return the expected result
 *       (or threw an exception);</li>
 *   <li>{@code TestSucceeded}: the method returned the expected result.</li>
 * </ol>
 * In the first two cases the method is not invoked at all. Methods are tested in the order in
 * which they are declared in the source file.
 */
public class RunTests {

    /** The four types allowed in a {@link Specification}, with the Java types they correspond to. */
    private enum SpecType {
        INT("int", int.class, Integer.class),
        DOUBLE("double", double.class, Double.class),
        BOOL("bool", boolean.class, Boolean.class),
        STRING("string", String.class, String.class);

        final String name;
        final Class<?> primitive;
        final Class<?> boxed;

        SpecType(String name, Class<?> primitive, Class<?> boxed) {
            this.name = name;
            this.primitive = primitive;
            this.boxed = boxed;
        }

        /** Returns the type with the given name, or null if the name is not one of the four. */
        static SpecType named(String name) {
            for (SpecType t : values()) {
                if (t.name.equals(name)) {
                    return t;
                }
            }
            return null;
        }

        /** Tells whether a parameter or return type of a method corresponds to this type. */
        boolean matches(Class<?> javaType) {
            return javaType == primitive || javaType == boxed;
        }

        /**
         * Converts the string representation of a value, e.g. "42" or "true".
         * @throws IllegalArgumentException if the string does not represent a value of this type.
         */
        Object parse(String text) {
            switch (this) {
                case INT:
                    return Integer.valueOf(text.trim());
                case DOUBLE:
                    return Double.valueOf(text.trim());
                case BOOL:
                    if (text.equals("true") || text.equals("false")) {
                        return Boolean.valueOf(text);
                    }
                    throw new IllegalArgumentException("not a boolean: " + text);
                default:
                    return text;
            }
        }
    }

    /** Signals that a specification cannot be used: the method will not be invoked. */
    private static final class NotTestable extends Exception {
        private static final long serialVersionUID = 1L;

        NotTestable(String message) {
            super(message);
        }
    }

    /**
     * The specification used for a method annotated with {@link Testable} but not with
     * {@link Specification}: all the attributes take their default values (no arguments, no result).
     */
    private static final Specification DEFAULT_SPECIFICATION = new Specification() {
        public String[] argTypes() { return new String[0]; }
        public String[] argValues() { return new String[0]; }
        public String resType() { return ""; }
        public String resVal() { return ""; }
        public Class<? extends Annotation> annotationType() { return Specification.class; }
    };

    /**
     * Entry point: {@code java RunTests <ClassName>}.
     * @param args the fully qualified name of the class to test.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java RunTests <ClassName>");
            System.exit(2);
        }
        Class<?> testClass;
        Object instance;
        try {
            testClass = Class.forName(args[0]);
            Constructor<?> constructor = testClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + args[0]);
            System.exit(1);
            return;
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.err.println("Cannot create an instance of " + args[0] + " with a no-argument constructor: " + e);
            System.exit(1);
            return;
        }
        for (Method method : testableMethods(testClass)) {
            runTest(instance, method);
        }
    }

    /**
     * Returns the non-private instance methods of the class annotated with {@link Testable},
     * in declaration order.
     */
    static List<Method> testableMethods(Class<?> testClass) {
        List<Method> methods = new ArrayList<>();
        for (Method m : testClass.getDeclaredMethods()) {
            int modifiers = m.getModifiers();
            if (m.isAnnotationPresent(Testable.class) && !Modifier.isPrivate(modifiers)
                    && !Modifier.isStatic(modifiers) && !m.isSynthetic() && !m.isBridge()) {
                methods.add(m);
            }
        }
        Map<String, Integer> order = declarationOrder(testClass);
        methods.sort(Comparator
                .comparingInt((Method m) -> order.getOrDefault(m.getName() + descriptor(m), Integer.MAX_VALUE))
                .thenComparing(Method::getName));
        return methods;
    }

    /**
     * Tests one method and reports the outcome exactly once.
     */
    private static void runTest(Object instance, Method method) {
        Specification spec = method.getAnnotation(Specification.class);
        if (spec == null) {
            spec = DEFAULT_SPECIFICATION;
        }

        Object[] arguments;
        try {
            arguments = convertArguments(method, spec);
        } catch (NotTestable e) {
            Report.report(Report.TEST_RESULT.WrongArgs, method.getName(), spec);
            return;
        }

        Object expected;
        try {
            expected = convertExpectedResult(method, spec);
        } catch (NotTestable e) {
            Report.report(Report.TEST_RESULT.WrongResultType, method.getName(), spec);
            return;
        }

        Object actual;
        try {
            method.setAccessible(true); // protected and package-private methods are tested too
            actual = method.invoke(instance, arguments);
        } catch (InvocationTargetException e) {
            // The method was invoked but threw an exception instead of returning the expected result.
            Report.report(Report.TEST_RESULT.TestFailed, method.getName(), spec);
            return;
        } catch (IllegalAccessException | RuntimeException e) {
            // The invocation itself failed, so the method could not be tested with these arguments.
            Report.report(Report.TEST_RESULT.WrongArgs, method.getName(), spec);
            return;
        }

        boolean success = (expected == null) ? actual == null : expected.equals(actual);
        Report.report(success ? Report.TEST_RESULT.TestSucceeded : Report.TEST_RESULT.TestFailed,
                method.getName(), spec);
    }

    /**
     * Builds the actual arguments from {@code argTypes} and {@code argValues}.
     * @throws NotTestable if the lengths differ from each other or from the number of parameters,
     *         if a type is unknown or does not correspond to the parameter, or if a value cannot be converted.
     */
    private static Object[] convertArguments(Method method, Specification spec) throws NotTestable {
        String[] types = spec.argTypes();
        String[] values = spec.argValues();
        Class<?>[] parameters = method.getParameterTypes();
        if (types.length != values.length || types.length != parameters.length) {
            throw new NotTestable("wrong number of arguments");
        }
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            SpecType type = SpecType.named(types[i]);
            if (type == null || !type.matches(parameters[i])) {
                throw new NotTestable("argument " + i + " has the wrong type");
            }
            try {
                arguments[i] = type.parse(values[i]);
            } catch (IllegalArgumentException e) {
                throw new NotTestable("argument " + i + " cannot be converted");
            }
        }
        return arguments;
    }

    /**
     * Builds the expected result from {@code resType} and {@code resVal}; returns null for a void method.
     * @throws NotTestable if the type does not correspond to the return type of the method,
     *         or if the expected value cannot be converted to it.
     */
    private static Object convertExpectedResult(Method method, Specification spec) throws NotTestable {
        Class<?> returnType = method.getReturnType();
        if (spec.resType().isEmpty()) {
            if (returnType != void.class || !spec.resVal().isEmpty()) {
                throw new NotTestable("a result is returned but none is expected, or vice versa");
            }
            return null;
        }
        SpecType type = SpecType.named(spec.resType());
        if (type == null || !type.matches(returnType)) {
            throw new NotTestable("the result has the wrong type");
        }
        try {
            return type.parse(spec.resVal());
        } catch (IllegalArgumentException e) {
            throw new NotTestable("the expected result cannot be converted");
        }
    }

    // --- Declaration order ---------------------------------------------------------------------

    /**
     * Maps "name + descriptor" of each method to its position in the class file.
     * <p>
     * {@code Class.getDeclaredMethods()} returns the methods "in no particular order", while the
     * expected outputs list the tests in source order. {@code javac} writes the method table of a
     * class file in declaration order, so the table is read here directly from the bytecode: the
     * constant pool is scanned to resolve the names, the fields are skipped, and the methods are
     * numbered as they appear. If the class file cannot be read, an empty map is returned and the
     * methods are sorted by name.
     */
    static Map<String, Integer> declarationOrder(Class<?> c) {
        Map<String, Integer> order = new HashMap<>();
        String resource = c.getName().substring(c.getName().lastIndexOf('.') + 1) + ".class";
        try (InputStream raw = c.getResourceAsStream(resource)) {
            if (raw == null) {
                return order;
            }
            DataInputStream in = new DataInputStream(raw);
            if (in.readInt() != 0xCAFEBABE) {
                return order;
            }
            in.readUnsignedShort(); // minor version
            in.readUnsignedShort(); // major version

            int poolCount = in.readUnsignedShort();
            String[] utf8 = new String[poolCount];
            for (int i = 1; i < poolCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1:  utf8[i] = in.readUTF(); break;           // Utf8
                    case 3: case 4: in.readInt(); break;              // Integer, Float
                    case 5: case 6: in.readLong(); i++; break;        // Long, Double: two slots
                    case 7: case 8: case 16: case 19: case 20:        // Class, String, MethodType, Module, Package
                        in.readUnsignedShort(); break;
                    case 9: case 10: case 11: case 12: case 17: case 18: // refs, NameAndType, Dynamic, InvokeDynamic
                        in.readInt(); break;
                    case 15: in.readUnsignedByte(); in.readUnsignedShort(); break; // MethodHandle
                    default: return new HashMap<>(); // unknown constant: give up
                }
            }
            in.readUnsignedShort(); // access flags
            in.readUnsignedShort(); // this class
            in.readUnsignedShort(); // super class
            skip(in, 2L * in.readUnsignedShort()); // interfaces

            int fieldCount = in.readUnsignedShort();
            for (int i = 0; i < fieldCount; i++) {
                skip(in, 6); // access flags, name, descriptor
                skipAttributes(in);
            }
            int methodCount = in.readUnsignedShort();
            for (int i = 0; i < methodCount; i++) {
                in.readUnsignedShort(); // access flags
                String name = utf8[in.readUnsignedShort()];
                String descriptor = utf8[in.readUnsignedShort()];
                order.putIfAbsent(name + descriptor, i);
                skipAttributes(in);
            }
        } catch (IOException | RuntimeException e) {
            return new HashMap<>();
        }
        return order;
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            in.readUnsignedShort(); // name
            skip(in, in.readInt() & 0xFFFFFFFFL);
        }
    }

    private static void skip(DataInputStream in, long bytes) throws IOException {
        long left = bytes;
        while (left > 0) {
            int skipped = in.skipBytes((int) Math.min(left, Integer.MAX_VALUE));
            if (skipped <= 0) {
                throw new IOException("truncated class file");
            }
            left -= skipped;
        }
    }

    /** The JVM method descriptor of a method, e.g. "(II)I" for {@code int testMin(int, int)}. */
    static String descriptor(Method m) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) {
            sb.append(descriptor(p));
        }
        return sb.append(')').append(descriptor(m.getReturnType())).toString();
    }

    private static String descriptor(Class<?> t) {
        if (t.isArray()) {
            return t.getName().replace('.', '/'); // already "[I", "[Ljava/lang/String;", ...
        }
        if (t == void.class) return "V";
        if (t == int.class) return "I";
        if (t == long.class) return "J";
        if (t == double.class) return "D";
        if (t == float.class) return "F";
        if (t == boolean.class) return "Z";
        if (t == byte.class) return "B";
        if (t == char.class) return "C";
        if (t == short.class) return "S";
        return "L" + t.getName().replace('.', '/') + ";";
    }
}
