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
     * @throws ImplerException if one of argument is null
     */
    private void argumentChecker(Class<?> token, Path path) throws ImplerException {
        if (path == null || token == null)
            throw new ImplerException(new IllegalArgumentException("Null arguments"));
    }

    /**
     * creates all directories of path if them do not exists
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
     * return path where the token implementation should be
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
     * makes jar file of class or interface witch implements token class
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
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
            // System.out.println(tmpDirPath.toString() + File.pathSeparator + System.getProperty("java.class.path"));
            // System.out.println(getFilePath(tmpDirPath, token, ".java").toString());
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
     * class to wrap Method and push it to HashSet collection
     */
    static class MethodWrap {
        private final Method method;

        /**
         * @param method the given method to wrap
         */
        private MethodWrap(Method method) {
            this.method = method;
        }

        /**
         * @return the inner method
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
     * return {@link Class} token by {@link String}
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
     * executable method of class
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
     * return {@link String} of new class name of {@link Class}
     *
     * @param token the given token
     * @return new name of the token
     */
    private static String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * return {@code n} times {@literal \t}
     *
     * @param n the number
     * @return {@link String} of n-times repeated {@literal \t}
     */
    private String tabN(int n) {
        return Stream.generate(() -> "\t").limit(n).collect(Collectors.joining());
    }

    /**
     * return default value {@link String} by {@link Class} token
     *
     * @param token the given {@link Class}
     * @return {@link String} of default value
     */
    private String getDefaultValue(Class<?> token) {
        if (token.equals(void.class)) return "";
        if (token.equals(boolean.class)) return " false";
        if (token.isPrimitive()) return " 0";
        return " null";
    }

    /**
     * return {@link String} of given parameters of {@link Executable}
     * if typed is {@code true} then adds class name before each parameter
     * else only parameters
     *
     * @param exec  the {@link Executable} instance
     * @param typed flag of adding class name before parameter
     * @return {@link String} of parameters in brackets with delimiter {@literal ,}
     */
    private String getParams(Executable exec, boolean typed) {
        return Arrays.stream(exec.getParameters())
                .map(parameter ->
                        (typed ? parameter.getType().getCanonicalName() + " " : "")
                                + parameter.getName())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * return {@link String} of throwable classes of {@link Executable}
     *
     * @param exec the {@link Executable} instance
     * @return {@link String} of execptions with delimiter {@literal}
     */
    private String getExceptions(Executable exec) {
        Class<?>[] exceptions = exec.getExceptionTypes();
        if (exceptions.length == 0)
            return "";
        return Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", " throws ", ""));
    }

    /**
     * return {@link String} name of {@link Executable} instance
     *
     * @param token the given {@link Class}
     * @param exec  the {@link Executable} instance
     * @return the {@link String} of method name
     */
    private String getMethodName(Class<?> token, Executable exec) {
        if (exec instanceof Method) {
            Method method = (Method) exec;
            return method.getReturnType().getCanonicalName() + " " + method.getName();
        }
        return getClassName(token);
    }

    /**
     * return {@link String} body code of {@link Executable} instance
     *
     * @param exec the {@link Executable} instance
     * @return the {@link String} of method body
     */
    private String getMethodBody(Executable exec) {
        if (exec instanceof Method) {
            return "return " + getDefaultValue(((Method) exec).getReturnType());
        }
        return "super" + getParams(exec, false);
    }

    /**
     * implements interface {@link info.kgeorgiy.java.advanced.implementor.Impler}
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException in any exceptional situation
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        argumentChecker(token, root);
        if (token.isPrimitive()
                || token.isArray()
                || token == Enum.class
                || Modifier.isPrivate(token.getModifiers())
                || Modifier.isFinal(token.getModifiers()))
            throw new ImplerException("Unimplementable class");
        root = getFilePath(root, token, ".java");
        createPath(root);
        try (Writer writer = Files.newBufferedWriter(root)) {
            implHead(token, writer);
            implMethods(token, writer);
            if (!token.isInterface())
                implConstructor(token, writer);
            write(writer, "}" + lineSeparator);
        } catch (IOException e) {
            throw new ImplerException("Cannot open result file", e);
        }
    }

    /**
     * write data {@code str} to {@code writer}
     *
     * @param writer the given {@link Writer}
     * @param str    the given {@link String}
     * @throws ImplerException if has {@link IOException}
     */
    private void write(Writer writer, String str) throws ImplerException {
        try {
            writer.write(str);
        } catch (IOException e) {
            throw new ImplerException("Write output file fail", e);
        }
    }

    /**
     * implement head of the {@code token} to output stream {@code writer}
     *
     * @param token the given {@link Class}
     * @param writer the given {@link Writer}
     * @throws ImplerException throws by {@link Implementor#write(Writer, String)}
     */
    private void implHead(Class<?> token, Writer writer) throws ImplerException {
        StringBuilder ans = new StringBuilder();
        if (!token.getPackageName().equals("")) {
            ans.append("package ")
                    .append(token.getPackageName())
                    .append(";")
                    .append(lineSeparator);
        }
        ans.append("public class ")
                .append(getClassName(token))
                .append(token.isInterface() ? " implements " : " extends ")
                .append(token.getCanonicalName())
                .append(" {")
                .append(lineSeparator);
        write(writer, ans.toString());
    }

    /**
     * implements {@code executable} method of {@code token} to output stream {@code writer}
     *
     * @param token the given {@link Class}
     * @param writer the given {@link Writer}
     * @param exec the given {@link Executable}
     * @throws ImplerException throws by {@link Implementor#write(Writer, String)}
     */
    private void implExecutable(Class<?> token, Writer writer, Executable exec) throws ImplerException {
        write(writer, (new StringBuilder(tabN(1)))
                .append(Modifier.toString(exec.getModifiers() & ~(Modifier.ABSTRACT ^ Modifier.NATIVE ^ Modifier.TRANSIENT)))
                .append(" ")
                .append(getMethodName(token, exec))
                .append(getParams(exec, true))
                .append(getExceptions(exec))
                .append("{")
                .append(lineSeparator)
                .append(tabN(2))
                .append(getMethodBody(exec))
                .append(";")
                .append(lineSeparator)
                .append(tabN(1))
                .append("}")
                .append(lineSeparator)
                .toString());
    }

    /**
     * add all abstract boxed {@code methods} to {@code set}
     *
     * @param methods the given {@link Method[]}
     * @param set the given {@link Set<MethodWrap>}
     */
    private void getAbstractMethods(Method[] methods, Set<MethodWrap> set) {
        Arrays.stream(methods)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodWrap::new)
                .collect(Collectors.toCollection(() -> set));
    }

    /**
     * implement all methods of {@code token} to output stream {@code writer}
     *
     * @param token the given {@link Class}
     * @param writer the given {@link Writer}
     * @throws ImplerException throws {@link Implementor#write(Writer, String)}
     */
    private void implMethods(Class<?> token, Writer writer) throws ImplerException {
        Set<MethodWrap> methods = new HashSet<>();
        getAbstractMethods(token.getMethods(), methods);
        while (token != null) {
            getAbstractMethods(token.getDeclaredMethods(), methods);
            token = token.getSuperclass();
        }
        for (MethodWrap exec : methods) {
            implExecutable(null, writer, exec.getMethod());
        }
    }

    /**
     * implement all constructors of {@code token} to output stream {@code writer}
     *
     * @param token the given {@link Class}
     * @param writer the given {@link Writer}
     * @throws ImplerException throws {@link Implementor#write(Writer, String)}
     */
    private void implConstructor(Class<?> token, Writer writer) throws ImplerException {
        Constructor<?>[] constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .toArray(Constructor[]::new);
        if (constructors.length == 0) {
            throw new ImplerException("No valid constructor");
        }
        for (Constructor<?> constructor : constructors) {
            implExecutable(token, writer, constructor);
        }
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.implementor jar-class ru.ifmo.rain.maslov.implementor.Implementor hello
