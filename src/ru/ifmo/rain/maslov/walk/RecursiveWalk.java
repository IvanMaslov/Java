package ru.ifmo.rain.maslov.walk;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    public static void main(String[] argv) {
        if (argv.length != 2 || argv[0] == null || argv[1] == null) {
            System.err.println("Incorrect argument: use RecursiveWalk <input file> <output file>");
            return;
        }
        execute(argv[0], argv[1]);
    }

    private static void execute(String fileIn, String fileOut) {
        try {
            Paths.get(fileIn);
            Paths.get(fileOut);
        } catch (InvalidPathException e) {
            System.err.println("Invalid input or output path");
            return;
        }
        if (Paths.get(fileOut).getParent() != null) {
            try {
                Files.createDirectories(Paths.get(fileOut).getParent());
            } catch (IOException e) {
                System.err.println("Cannot create output file");
                return;
            }
        }
        try (BufferedReader in = new BufferedReader(new FileReader(fileIn, StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new FileWriter(fileOut, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    MyVisitor visitor = new MyVisitor(out);
                    Files.walkFileTree(Paths.get(line), visitor);
                    if (!visitor.isActed()) {
                        out.write(Walk.formatFileOutput(0, line));
                    }
                } catch (FileNotFoundException e) {
                    System.err.println("No such path: " + line);
                    out.write(Walk.formatFileOutput(0, line));
                } catch (InvalidPathException e) {
                    System.err.println("Invalid path: " + line);
                    out.write(Walk.formatFileOutput(0, line));
                } catch (IOException ignore) {
                    ///file or directory not exists
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("No such file error");
        } catch (IOException e) {
            System.err.println("Some read\\write error occurred");
        }
    }

    public static class MyVisitor extends SimpleFileVisitor<Path> {
        private final Writer writer;
        private boolean acted;

        MyVisitor(Writer writer) {
            this.writer = writer;
            this.acted = false;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            writer.write(Walk.hash(path.toString()));
            acted = true;
            return FileVisitResult.CONTINUE;
        }

        boolean isActed() {
            return acted;
        }
    }
}
