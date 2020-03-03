package ru.ifmo.rain.maslov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Implementor implements Impler {

    private static final String lineSeparator = System.lineSeparator();

    static class MethodWrap {
        private final Method method;

        private MethodWrap(Method method) {
            this.method = method;
        }

        Method getMethod() {
            return method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodWrap that = (MethodWrap) o;
            return Objects.equals(method.getName(), that.method.getName())
                    && Objects.equals(method.getReturnType(), that.method.getReturnType())
                    && Arrays.equals(method.getParameterTypes(), that.method.getParameterTypes());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    method.getName().hashCode(),
                    method.getReturnType().hashCode(),
                    Arrays.hashCode(method.getParameterTypes()));
        }
    }

    private String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    private String tabN(int n) {
        return Stream.generate(() -> "\t").limit(n).collect(Collectors.joining());
    }

    private String getDefaultValue(Class<?> token) {
        if (token.equals(void.class)) return "";
        if (token.equals(boolean.class)) return " false";
        if (token.isPrimitive()) return " 0";
        return " null";
    }

    private String getParams(Executable exec, boolean typed) {
        return Arrays.stream(exec.getParameters())
                .map(parameter ->
                        (typed ? parameter.getType().getCanonicalName() + " " : "")
                                + parameter.getName()
                ).collect(Collectors.joining(", ", "(", ")"));
    }

    private String getExceptions(Executable exec) {
        Class<?>[] exceptions = exec.getExceptionTypes();
        if (exceptions.length == 0)
            return "";
        return Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", " throws ", ""));
    }

    private String getMethodName(Class<?> token, Executable exec) {
        if (exec instanceof Method) {
            Method method = (Method) exec;
            return method.getReturnType().getCanonicalName() + " " + method.getName();
        }
        return getClassName(token);
    }

    private String getMethodBody(Executable exec) {
        if (exec instanceof Method) {
            return "return " + getDefaultValue(((Method) exec).getReturnType());
        }
        return "super" + getParams(exec, false);
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (root == null || token == null)
            throw new ImplerException(new IllegalArgumentException("Null arguments"));
        if (token.isPrimitive()
                || token.isArray()
                || token == Enum.class
                || Modifier.isPrivate(token.getModifiers())
                || Modifier.isFinal(token.getModifiers()))
            throw new ImplerException("Unimplementable class");
        root = root
                .resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(getClassName(token) + ".java");
        Path parent = root.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new ImplerException("Cannot create result file directories", e);
            }
        }
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

    private void write(Writer writer, String str) throws ImplerException {
        try {
            writer.write(str);
        } catch (IOException e) {
            throw new ImplerException("Write output file fail", e);
        }
    }

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

    private void implExecutable(Class<?> token, Writer writer, Executable exec) throws ImplerException {
        StringBuilder res = new StringBuilder(tabN(1));
        final int mod = exec.getModifiers()
                & ~Modifier.ABSTRACT
                & ~Modifier.NATIVE
                & ~Modifier.TRANSIENT;
        res.append(Modifier.toString(mod))
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
                .append(lineSeparator);
        write(writer, res.toString());
    }

    private void getAbstractMethods(Method[] methods, Set<MethodWrap> set) {
        Arrays.stream(methods)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodWrap::new)
                .collect(Collectors.toCollection(() -> set));
    }

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

// java -cp . -p . -m info.kgeorgiy.java.advanced.implementor class ru.ifmo.rain.maslov.implementor.Implementor hello
