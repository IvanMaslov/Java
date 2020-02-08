package ru.ifmo.rain.maslov.walk;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    public static void main(String[] argv) {
        if (argv.length != 2) {
            System.err.println("Incorrect argument: use RecursiveWalk <input file> <output file>");
            return;
        }
        execute(argv[0], argv[1]);
    }

    private static void execute(String fileIn, String fileOut) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileIn, StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new FileWriter(fileOut, StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                Files.walkFileTree(Paths.get(line), new MyVisitor(out));
            }
            out.flush();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
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
