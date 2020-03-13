package ru.ifmo.rain.maslov.implementor;

import info.kgeorgiy.java.advanced.implementor.JarImpler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class Implementor implements JarImpler {

    private static final String lineSeparator = System.lineSeparator();

    /**
     * Check both argument are not null
     *
     * @param token the given class token
     * @param path  the given path
     * @throws ImplerException if one of argument is {@code null}
     */
    private void argumentChecker(Class<?> token, Path path) throws ImplerException {
        if (path == null || token == null)
            throw new ImplerException(new IllegalArgumentException("Null arguments"));
    }

    /**
     * Creates all directories of {@code path} if them do not exists
     *
     * @param file the given path
     * @throws ImplerException if creation of directories failed
     */
    private void createPath(Path file) throws ImplerException {
        Path parent = file.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new ImplerException("Cannot create result file directories", e);
            }
        }
    }

    /**
     * Path where the token implementation should be
     *
     * @param path   the folder path
     * @param token  the given class token
     * @param suffix suffix to resolved file in folder
     * @return the result path
     */
    private static Path getFilePath(Path path, Class<?> token, String suffix) {
        return path
                .resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(getClassName(token) + suffix);
    }

    /**
     * Makes jar file of class or interface witch implements token class
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException in any exceptional situation during generation
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        argumentChecker(token, jarFile);
        createPath(jarFile);
        Path tmpDirPath;
        try {
            tmpDirPath = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Cannot create temp directory");
        }
        try {
            implement(token, tmpDirPath);
            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Ivan Maslov");
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null || compiler.run(null, null, null, "-cp",
                    tmpDirPath.toString() + File.pathSeparator + System.getProperty("java.class.path"),
                    getFilePath(tmpDirPath, token, ".java").toString()) != 0) {
                throw new ImplerException("Cannot compile generated files");
            }
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                String className = token.getName().replace('.', '/') + "Impl.class";
                jar.putNextEntry(new ZipEntry(className));
                Files.copy(getFilePath(tmpDirPath, token, ".class"), jar);
            } catch (IOException e) {
                throw new ImplerException("Unable to write to JAR file", e);
            }
        } finally {
            try {
                Files.walk(tmpDirPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new ImplerException("Cannot remove temp directory");
            }
        }
    }

    /**
     * Class to wrap {@link Method} and push it to {@link HashSet} {@link Collection}
     */
    static class MethodWrap {
        private final Method method;

        /**
         * @param method the given {@link Method} to wrap
         */
        private MethodWrap(Method method) {
            this.method = method;
        }

        /**
         * @return the inner {@code method}
         */
        Method getMethod() {
            return method;
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param o the reference object with which to compare.
         * @return {@code true} if this object is the same as the obj
         * argument; {@code false} otherwise.
         * @see #hashCode()
         * @see java.util.HashMap
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodWrap that = (MethodWrap) o;
            return Objects.equals(method.getName(), that.method.getName())
                    && Objects.equals(method.getReturnType(), that.method.getReturnType())
                    && Arrays.equals(method.getParameterTypes(), that.method.getParameterTypes());
        }

        /**
         * Returns a hash code value for the object. This method is
         * supported for the benefit of hash tables such as those provided by
         * {@link java.util.HashMap}.
         *
         * @return a hash code value for this object.
         * @see java.lang.Object#equals(java.lang.Object)
         * @see java.lang.System#identityHashCode
         */
        @Override
        public int hashCode() {
            return Objects.hash(
                    method.getName().hashCode(),
                    method.getReturnType().hashCode(),
                    Arrays.hashCode(method.getParameterTypes()));
        }
    }

    /**
     * Get {@link Class} token by {@link String}
     *
     * @param token the given string
     * @return the class token
     * @throws ImplerException if there is no such class of the string or {@code token == null}
     */
    private static Class<?> getTokenToMain(String token) throws ImplerException {
        try {
            return Class.forName(Objects.requireNonNull(token));
        } catch (ClassNotFoundException | NullPointerException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * return {@link Path} by {@link String}
     *
     * @param path the given string
     * @return the path
     * @throws ImplerException if the string is not valid path or {@code string == null}
     */
    private static Path getPathToMain(String path) throws ImplerException {
        try {
            return Paths.get(Objects.requireNonNull(path));
        } catch (InvalidPathException | NullPointerException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Executable method of class {@link Implementor}
     *
     * @param argv the given arguments from console
     */
    public static void main(String[] argv) {
        if (argv == null || argv.length < 2 || argv.length > 3) {
            System.out.println("Incorrect arguments");
            return;
        }
        try {
            JarImpler impl = new Implementor();
            if (argv.length == 2) {
                impl.implement(getTokenToMain(argv[0]), getPathToMain(argv[1]));
            }
            if (argv.length == 3) {
                impl.implementJar(getTokenToMain(argv[1]), getPathToMain(argv[2]));
            }
        } catch (ImplerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get {@link String} of new class name of {@link Class}
     *
     * @param token the given token
     * @return new name of the token
     */
    private static String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Get {@code n} times {@literal \t}
     *
     * @param n the number
     * @return {@link String} of n-times repeated {@literal \t}
     */
    private static String tabN(int n) {
        return Stream.generate(() -> "\t").limit(n).collect(Collectors.joining());
    }

    /**
     * Get default {@link String} value by {@link Class} token
     *
     * @param token the given {@link Class} token
     * @return {@link String} of default value
     */
    private static String getDefaultValue(Class<?> token) {
        if (token.equals(void.class)) return "";
        if (token.equals(boolean.class)) return " false";
        if (token.isPrimitive()) return " 0";
        return " null";
    }

    /**
     * Get {@link String} of given parameters of {@link Executable}
     * <p>
     * if typed is {@code true} then adds class name before each parameter
     * else only parameters
     * </p>
     *
     * @param exec  the {@link Executable} instance
     * @param typed flag of adding class name before parameter
     * @return {@link String} of parameters in brackets with delimiter {@literal ,}
     */
    private static String getParams(Executable exec, boolean typed) {
        return Arrays.stream(exec.getParameters())
                .map(parameter ->
                        (typed ? parameter.getType().getCanonicalName() + " " : "")
                                + parameter.getName())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Get {@link String} of throwable classes of {@link Executable}
     *
     * @param exec the {@link Executable} instance
     * @return {@link String} of execptions with delimiter {@literal}
     */
    private static String getExceptions(Executable exec) {
        Class<?>[] exceptions = exec.getExceptionTypes();
        if (exceptions.length == 0)
            return "";
        return Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", " throws ", ""));
    }

    /**
     * Get {@link String} name of {@link Executable} instance
     *
     * @param token the given {@link Class}
     * @param exec  the {@link Executable} instance
     * @return the {@link String} of method name
     */
    private static String getMethodName(Class<?> token, Executable exec) {
        Method method = (Method) exec;
        return method.getReturnType().getCanonicalName() + " " + method.getName();
    }

    /**
     * Get {@link String} name of {@link Executable} instance
     *
     * @param token the given {@link Class}
     * @param exec  the {@link Executable} instance
     * @return the {@link String} of method name
     */
    private static String getConstructorName(Class<?> token, Executable exec) {
        return getClassName(token);
    }

    /**
     * Get {@link String} body code of {@link Executable} instance
     *
     * @param exec the {@link Executable} instance
     * @return the {@link String} of method body
     */
    private static String getMethodBody(Executable exec) {
        return "return " + getDefaultValue(((Method) exec).getReturnType());
    }


    /**
     * Get {@link String} body code of {@link Executable} instance
     *
     * @param exec the {@link Executable} instance
     * @return the {@link String} of method body
     */
    private static String getConstructorBody(Executable exec) {
        return "super" + getParams(exec, false);
    }

    /**
     * Implements interface {@link info.kgeorgiy.java.advanced.implementor.Impler} to {@code root} directory
     *
     * @param token {@link Class} token to create implementation for.
     * @param root  {@link Path} root of directory.
     * @throws ImplerException in any exceptional situation:
     *                         <ul>
     *                          <li> if token is primitive </li>
     *                          <li> if token is array</li>
     *                          <li> if token is {@link Enum}</li>
     *                          <li> if token is private</li>
     *                          <li> if token is protected</li>
     *                          <li> if happened {@link ImplerException} in methods:<ul>
     *                              <li>{@link Implementor#implMethods(Class, Writer)}</li>
     *                              <li>{@link Implementor#implHead(Class, Writer)}</li>
     *                              <li>{@link Implementor#implConstructor(Class, Writer)}</li>
     *                          </ul> </li>
     *                         </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        argumentChecker(token, root);
        if (token.isPrimitive()
                || token.isArray()
                || token == Enum.class
                || Modifier.isPrivate(token.getModifiers())
                || Modifier.isFinal(token.getModifiers())
        ) {
            throw new ImplerException("Unimplementable class");
        }
        root = getFilePath(root, token, ".java");
        createPath(root);
        try (Writer writer = Files.newBufferedWriter(root)) {
            implHead(token, writer);
            if (!token.isInterface()) {
                implConstructor(token, writer);
            }
            implMethods(token, writer);
            write(writer, "}" + lineSeparator);
        } catch (IOException e) {
            throw new ImplerException("Cannot open result file", e);
        }
    }

    /**
     * Push data {@code str} converted to {@literal Unicode} to {@link Writer} {@code writer}
     *
     * @param writer the given {@link Writer}
     * @param str    the given {@link String}
     * @throws ImplerException if has {@link IOException}
     */
    private void write(Writer writer, String str) throws ImplerException {
        try {
            StringBuilder unicodeStr = new StringBuilder();
            for (char c : str.toCharArray()) {
                if (c >= 128)
                    unicodeStr.append("\\u").append(String.format("%04X", (int) c));
                else
                    unicodeStr.append(c);
            }
            writer.write(unicodeStr.toString());
        } catch (IOException e) {
            throw new ImplerException("Write output file fail", e);
        }
    }

    /**
     * Implement head of the {@code token} to {@link Writer} {@code writer}
     *
     * @param token  the given {@link Class}
     * @param writer the given {@link Writer}
     * @throws ImplerException throws by {@link Implementor#write(Writer, String)}
     */
    private void implHead(Class<?> token, Writer writer) throws ImplerException {
        String ans = "";
        if (!token.getPackageName().equals("")) {
            ans = "package " + token.getPackageName() + ";" + lineSeparator;
        }
        ans += "public class " + getClassName(token) + (token.isInterface() ? " implements " : " extends ")
                + token.getCanonicalName() + " {" + lineSeparator;
        write(writer, ans);
    }

    /**
     * Implement {@code executable} method of {@code token} to {@link Writer} {@code writer}
     *
     * @param token  the given {@link Class}
     * @param writer the given {@link Writer}
     * @param exec   the given {@link Executable}
     * @throws ImplerException throws by {@link Implementor#write(Writer, String)}
     */
    private void implExecutable(Class<?> token, Writer writer, Executable exec,
                                BiFunction<Class<?>, Executable, String> getExecutableName,
                                Function<Executable, String> getExecutableBody) throws ImplerException {
        write(writer, tabN(1) +
                Modifier.toString(exec.getModifiers() & ~(Modifier.ABSTRACT ^ Modifier.NATIVE ^ Modifier.TRANSIENT)) +
                " " +
                getExecutableName.apply(token, exec) +
                getParams(exec, true) +
                getExceptions(exec) +
                "{" +
                lineSeparator +
                tabN(2) +
                getExecutableBody.apply(exec) +
                ";" +
                lineSeparator +
                tabN(1) +
                "}" +
                lineSeparator);
    }

    /**
     * Add all boxed {@code methods} to {@code set}
     *
     * @param methods the given {@link Method[]}
     * @param set     the given {@link Set}
     */
    private void addMethodsToSet(Method[] methods, Set<MethodWrap> set) {
        Arrays.stream(methods).map(MethodWrap::new).collect(Collectors.toCollection(() -> set));
    }

    /**
     * Implement all methods of {@code token} to {@link Writer} {@code writer}
     *
     * @param token  the given {@link Class}
     * @param writer the given {@link Writer}
     * @throws ImplerException throws {@link Implementor#write(Writer, String)}
     */
    private void implMethods(Class<?> token, Writer writer) throws ImplerException {
        Set<MethodWrap> methods = new HashSet<>();
        addMethodsToSet(token.getMethods(), methods);
        while (token != null) {
            addMethodsToSet(token.getDeclaredMethods(), methods);
            token = token.getSuperclass();
        }
        methods = methods.stream()
                .filter(x -> Modifier.isAbstract(x.getMethod().getModifiers()))
                .collect(Collectors.toSet());
        for (MethodWrap exec : methods) {
            implExecutable(null, writer, exec.getMethod(), Implementor::getMethodName, Implementor::getMethodBody);
        }
    }

    /**
     * Implement all constructors of {@code token} to {@link Writer} {@code writer}
     *
     * @param token  the given {@link Class}
     * @param writer the given {@link Writer}
     * @throws ImplerException throws {@link Implementor#write(Writer, String)}
     *                         or in case if zero non-private constructors
     */
    private void implConstructor(Class<?> token, Writer writer) throws ImplerException {
        Constructor<?>[] constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .toArray(Constructor[]::new);
        if (constructors.length == 0) {
            throw new ImplerException("No valid constructor");
        }
        for (Constructor<?> constructor : constructors) {
            implExecutable(token, writer, constructor, Implementor::getConstructorName, Implementor::getConstructorBody);
        }
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.implementor jar-class ru.ifmo.rain.maslov.implementor.Implementor hello
