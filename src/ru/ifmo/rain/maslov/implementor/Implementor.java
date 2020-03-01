package ru.ifmo.rain.maslov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Implementor implements Impler {
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (root == null || token == null)
            throw new ImplerException(new IllegalArgumentException("Null arguments"));
        root = root
                .resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(token.getSimpleName() + "Impl.java");
        Path parent = root.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new ImplerException("Cannot create result file directories", e);
            }
        }
        try (Writer writer = Files.newBufferedWriter(root)) {
            writer.write("some");
        } catch (IOException e) {
            throw new ImplerException("Error during writing to result file", e);
        }
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.implementor interface ru.ifmo.rain.maslov.implementor.Implementor hello
