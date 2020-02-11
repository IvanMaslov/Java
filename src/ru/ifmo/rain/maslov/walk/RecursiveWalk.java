package ru.ifmo.rain.maslov.walk;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    public static void main(String[] argv) {
        if (argv == null || argv.length != 2 || argv[0] == null || argv[1] == null) {
            System.err.println("Incorrect argument: use RecursiveWalk <input file> <output file>");
            return;
        }
        execute(argv[0], argv[1]);
    }

    private static void execute(String fileIn, String fileOut) {
        try {
            Paths.get(fileIn);
        } catch (InvalidPathException e) {
            Walk.logger("Invalid input path: " + fileIn, e);
            return;
        }
        try {
            Paths.get(fileOut);
        } catch (InvalidPathException e) {
            Walk.logger("Invalid output path: " + fileIn, e);
            return;
        }
        if (Paths.get(fileOut).getParent() != null) {
            try {
                Files.createDirectories(Paths.get(fileOut).getParent());
            } catch (IOException e) {
                Walk.logger("Cannot create output file directories: " + fileOut, e);
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
                    Walk.logger("No such path: " + line, e);
                    out.write(Walk.formatFileOutput(0, line));
                } catch (InvalidPathException e) {
                    Walk.logger("Invalid path: " + line, e);
                    out.write(Walk.formatFileOutput(0, line));
                } catch (IOException ignore) {
                    Walk.logger(null, ignore);
                    out.write(Walk.formatFileOutput(0, line));
                    ///file or directory not exists
                }
            }
        } catch (FileNotFoundException e) {
            Walk.logger("No such file error", e);
        } catch (IOException e) {
            Walk.logger("Some read\\write error occurred", e);
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
