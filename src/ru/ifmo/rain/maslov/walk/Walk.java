package ru.ifmo.rain.maslov.walk;

import java.io.*;
import java.nio.*;

public class Walk {
    private static final int BUFSIZE = 1024;

    public static void main(String[] argv) {
        if (argv.length != 2) {
            System.err.println("Incorrect argument: use Walk <input file> <output file>");
            return;
        }
        walk(argv[0], argv[1]);
    }

    private static void walk(String fileIn, String fileOut) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileIn));
            BufferedWriter out = new BufferedWriter(new FileWriter(fileOut));
            String line;
            while ((line = in.readLine()) != null) {
                out.write(hash(line) + ' ' + line + '\n');
            }
            out.flush();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String hash(String fileName) {
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
            return "00000000";
        }
        return String.format("%08x", res);
    }

}
