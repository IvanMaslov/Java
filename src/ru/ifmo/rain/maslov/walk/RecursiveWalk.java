package ru.ifmo.rain.maslov.walk;


import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {
    private static final int BUFSIZE = 1024;

    public static void main(String[] argv) {
        if (argv == null) {
            logger("Incorrect argument: argv is null");
            return;
        }
        if (argv.length != 2) {
            logger("Incorrect argument: argv length is not equal 2");
            return;
        }
        if (argv[0] == null) {
            logger("Incorrect argument: first argument is null");
            return;
        }
        if (argv[1] == null) {
            logger("Incorrect argument: second argument is null");
            return;
        }
        execute(argv[0], argv[1]);
    }

    private static Path getPathFromUser(String str, String toLog) throws InvalidPathException {
        try {
            return Paths.get(str);
        } catch (InvalidPathException e) {
            logger(toLog + str, e);
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
                    logger("Cannot create output file directories: " + fileOut, e);
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
                    } catch (InvalidPathException e) {
                        out.write(formatFileOutput(0, line));
                    } catch (IOException ignore) {
                        logger("Missed file: " + line, ignore);
                        out.write(formatFileOutput(0, line));
                    }
                }
            } catch (IOException ignored) {
            }
        } catch (InvalidPathException ignored) {
        }
    }


    private static void logger(String reason, Exception e) {
        System.err.println("ERROR: \t" + reason + "\n\t" + e.getMessage());
    }

    private static void logger(String reason) {
        System.err.println("ERROR: \t" + reason);
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
            logger("Reading fail in " + filePath.toString(), e);
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
            try{
                writer.write(hash(path));
            } catch (IOException e) {
                logger("Fail to write output file", e);
                return  FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.walk RecursiveWalk ru.ifmo.rain.maslov.walk.RecursiveWalk
