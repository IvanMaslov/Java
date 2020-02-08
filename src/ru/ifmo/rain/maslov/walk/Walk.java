package ru.ifmo.rain.maslov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileIn, StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new FileWriter(fileOut, StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                out.write(f.apply(line));
            }
            out.flush();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static String hash(String fileName) {
        final int FNV_32_PRIME = 0x01000193;
        int res = 0x811c9dc5;
        try {
            FileInputStream file = new FileInputStream(fileName);
            byte[] buf = new byte[BUFSIZE];
            int readed;
            for (readed = file.read(buf, 0, BUFSIZE); readed > 0; readed = file.read(buf, 0, BUFSIZE)) {
                for (int i = 0; i < readed; ++i) {
                    res *= FNV_32_PRIME;
                    res ^= buf[i] & 0xff;
                }
            }

            file.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "00000000" + ' ' + fileName + '\n';
        }
        return String.format("%08x", res) + ' ' + fileName + '\n';
    }

}
