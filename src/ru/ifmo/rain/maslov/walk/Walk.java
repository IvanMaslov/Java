package ru.ifmo.rain.maslov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.function.Function;

public class Walk {
    private static final int BUFSIZE = 1024;

    public static void main(String[] argv) {
        if (argv.length != 2) {
            System.err.println("Incorrect argument: use Walk <input file> <output file>");
            return;
        }
        execute(argv[0], argv[1], Walk::hash);
    }

    private static void execute(String fileIn, String fileOut, Function<String, String> f) {
        if (Paths.get(fileOut).getParent() != null) {
            try {
                Files.createDirectories(Paths.get(fileOut).getParent());
            } catch (IOException e) {
                System.err.println("Cannot create output file");
                e.printStackTrace();
                return;
            }
        }
        try (BufferedReader in = new BufferedReader(new FileReader(fileIn, StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new FileWriter(fileOut, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                out.write(f.apply(line));
            }
        } catch (FileNotFoundException e) {
            System.err.println("No such file error");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Some read\\write error occurred");
            e.printStackTrace();
        }
    }

    static void logger(String reason, Exception e) {
        System.err.println("ERROR: \t" + reason + "\n\t" + e.getMessage());
    }

    static void logger(String reason) {
        System.err.println("ERROR: \t" + reason);
    }

    static String formatFileOutput(int res, String fileName) {
        return String.format("%08x", res) + " " + fileName + "\n";
    }

    static String hash(String fileName) {
        if(fileName == null || fileName.isBlank())
            return formatFileOutput(0, fileName);
        try {
            Paths.get(fileName);
        } catch (InvalidPathException e) {
            logger("Invalid path " + fileName, e);
            return formatFileOutput(0, fileName);
        }
        final int FNV_32_PRIME = 0x01000193;
        int res = 0x811c9dc5;
        try (FileInputStream file = new FileInputStream(fileName)) {
            byte[] buf = new byte[BUFSIZE];
            int readed;
            for (readed = file.read(buf, 0, BUFSIZE); readed > 0; readed = file.read(buf, 0, BUFSIZE)) {
                for (int i = 0; i < readed; ++i) {
                    res *= FNV_32_PRIME;
                    res ^= buf[i] & 0xff;
                }
            }
        } catch (IOException e) {
            logger("IOException in " + fileName, e);
            return formatFileOutput(0, fileName);
        }
        return formatFileOutput(res, fileName);
    }

}
