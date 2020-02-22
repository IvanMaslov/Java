package ru.ifmo.rain.maslov.walk;


import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class RecursiveWalk {

    public static void main(String[] argv) {
        if (argv == null || argv.length != 2 || argv[0] == null || argv[1] == null) {
            Walk.logger("Incorrect argument: use RecursiveWalk <input file> <output file>");
            return;
        }
        execute(argv[0], argv[1]);
    }

    private static Path getPathFromUser(String str, String toLog) throws InvalidPathException {
        try {
            return Paths.get(str);
        } catch (InvalidPathException e) {
            Walk.logger(toLog + str, e);
            throw e;
        }
    }

    private static void execute(String fileIn, String fileOut) {
        try {
            Path inPath = getPathFromUser(fileIn, "Invalid input path: ");
            Path outPath = getPathFromUser(fileOut, "Invalid output path: ");
            if (outPath.getParent() != null) {
                try {
                    Files.createDirectories(outPath.getParent());
                } catch (IOException e) {
                    Walk.logger("Cannot create output file directories: " + fileOut, e);
                    return;
                }
            }
            try (BufferedReader in = Files.newBufferedReader(inPath);
                 BufferedWriter out = Files.newBufferedWriter(outPath)) {
                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        Files.walkFileTree(getPathFromUser(line, "Invalid path in input file: "),
                                new MyVisitor(out));
                    } catch (FileNotFoundException e) {
                        Walk.logger("No such path: " + line, e);
                        out.write(Walk.formatFileOutput(0, line));
                    } catch (InvalidPathException e) {
                        out.write(Walk.formatFileOutput(0, line));
                    } catch (IOException ignore) {
                        Walk.logger(null, ignore);
                        out.write(Walk.formatFileOutput(0, line));
                    }
                }
            } catch (IOException ignored) {
            }
        } catch (InvalidPathException ignored) {
        }
    }

    public static class MyVisitor extends SimpleFileVisitor<Path> {
        private final Writer writer;

        MyVisitor(Writer writer) {
            this.writer = writer;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            writer.write(Walk.hash(path.toString()));
            return FileVisitResult.CONTINUE;
        }
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.walk RecursiveWalk ru.ifmo.rain.maslov.walk.RecursiveWalk
