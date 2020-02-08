package ru.ifmo.rain.maslov.walk;

import ru.ifmo.rain.maslov.walk.Walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collector;

public class RecursiveWalk {
    private static final int BUFSIZE = 1024;

    public static void main(String[] argv) {
        if (argv.length != 2) {
            System.err.println("Incorrect argument: use RecrsiveWalk <input file> <output file>");
            return;
        }
        Walk.execute(argv[0], argv[1], (String a) -> recursiveGo(a).toString());
    }

    public static StringBuilder recursiveGo(String path) {
        StringBuilder result = new StringBuilder();
        Path p = Paths.get(path);
        if (Files.isDirectory(p)) {
            try {
                Files.walk(p)
                        .map((Path somePath) -> recursiveGo(somePath.toString()))
                        .forEach((StringBuilder bldr) -> result.append(bldr.toString()));
            } catch (IOException e) {
                return result;
            }
        }
        if (Files.isRegularFile(p)) {
            result.append(Walk.hash(path));
        }
        return result;
    }
}
