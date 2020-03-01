package ru.ifmo.rain.maslov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Implementor implements Impler {

    private static final String lineSeparator = System.lineSeparator();

    private String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (root == null || token == null)
            throw new ImplerException(new IllegalArgumentException("Null arguments"));
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
            implTail(token, writer);
        } catch (IOException e) {
            throw new ImplerException("Cannot open result file", e);
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
                .append(token.isInterface() ? " implements " : "extends ")
                .append(token.getSimpleName())
                .append(" {")
                .append(lineSeparator);
        try {
            writer.write(ans.toString());
        } catch (IOException e) {
            throw new ImplerException("Write output file fail", e);
        }
    }

    private void implTail(Class<?> token, Writer writer) throws ImplerException {
        try {
            writer.write("}");
            writer.write(lineSeparator);
        } catch (IOException e) {
            throw new ImplerException("Write output file fail", e);
        }
    }

    private void implMethods(Class<?> token, Writer writer) throws ImplerException {

    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.implementor interface ru.ifmo.rain.maslov.implementor.Implementor hello
