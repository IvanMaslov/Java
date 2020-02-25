package ru.ifmo.rain.maslov.walk;


import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


public class RecursiveWalk {
    private static final int BUFSIZE = 1024;

    private static class RecursiveWalkException extends Exception {
        RecursiveWalkException(String s, Exception e) {
            super(s + "\n" + e.getMessage());
        }

        RecursiveWalkException(String s) {
            super(s);
        }

        RecursiveWalkException(Exception s) {
            this(s.getMessage());
        }

        void print() {
            System.err.println(getMessage());
        }
    }

    public static void main(String[] argv) {
        try {
            if (argv == null) {
                throw new RecursiveWalkException("Incorrect argument: argv is null");
            }
            if (argv.length != 2) {
                throw new RecursiveWalkException("Incorrect argument: argv length is not equal 2");
            }
            if (argv[0] == null) {
                throw new RecursiveWalkException("Incorrect argument: first argument is null");
            }
            if (argv[1] == null) {
                throw new RecursiveWalkException("Incorrect argument: second argument is null");
            }
            execute(argv[0], argv[1]);
        } catch (RecursiveWalkException e) {
            e.print();
        }

    }

    private static Path getPathFromUser(String str, String toLog) throws RecursiveWalkException {
        try {
            return Paths.get(str);
        } catch (InvalidPathException e) {
            throw new RecursiveWalkException(toLog + str, e);
        }
    }

    private static void execute(String fileIn, String fileOut) throws RecursiveWalkException {
        Path inPath = getPathFromUser(fileIn, "Invalid input path: ");
        Path outPath = getPathFromUser(fileOut, "Invalid output path: ");

        Path parent = outPath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new RecursiveWalkException("Cannot create output file directories: " + fileOut, e);
            }
        }

        try (BufferedReader in = Files.newBufferedReader(inPath)) {
            try (BufferedWriter out = Files.newBufferedWriter(outPath)) {
                try {
                    MyVisitor visitor = new MyVisitor(out);
                    String line;
                    while ((line = in.readLine()) != null) {
                        try {
                            Files.walkFileTree(Paths.get(line), visitor);
                        } catch (InvalidPathException e) {
                            out.write(formatFileOutput(0, line));
                        }
                    }
                } catch (IOException e) {
                    throw new RecursiveWalkException(e);
                }
            } catch (IOException e) {
                throw new RecursiveWalkException("Output file error: ", e);
            }
        } catch (IOException e) {
            throw new RecursiveWalkException("Input file error: ", e);
        }
    }

    private static String formatFileOutput(int res, String fileName) {
        return String.format("%08x", res) + " " + fileName + "\n";
    }

    private static String hash(Path filePath) {
        final int FNV_32_PRIME = 0x01000193;
        int res = 0x811c9dc5;
        try (InputStream file = Files.newInputStream(filePath)) {
            byte[] buf = new byte[BUFSIZE];
            int readed;
            for (readed = file.read(buf, 0, BUFSIZE); readed > 0;
                 readed = file.read(buf, 0, BUFSIZE)) {
                for (int i = 0; i < readed; ++i) {
                    res *= FNV_32_PRIME;
                    res ^= buf[i] & 0xff;
                }
            }
        } catch (IOException e) {
            res = 0;
        }
        return formatFileOutput(res, filePath.toString());

    }

    public static class MyVisitor extends SimpleFileVisitor<Path> {
        private final Writer writer;

        MyVisitor(Writer writer) {
            this.writer = writer;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            writer.write(hash(path));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
            writer.write(formatFileOutput(0, path.toString()));
            return FileVisitResult.CONTINUE;
        }
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.walk RecursiveWalk ru.ifmo.rain.maslov.walk.RecursiveWalk
