package com.idocs.shasum;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Main {
    private static final Logger log = Logger.getLogger("shasum");

    public static void main(String[] args) throws Exception {
        File workingDir = new File(System.getProperty("user.dir"));

        String fileAcceptRegex = null;
        if (args.length == 1) {
            fileAcceptRegex = args[0];
            log.info("Calculating SHA in " + workingDir.getName() + " for files matching "
                    + fileAcceptRegex);
        } else {
            log.info("Calculating SHA in " + workingDir.getName()
                    + " of all files (filter example: .*\\.jar)");
        }

        File outputReport = new File(workingDir, "shasums.txt");
        writeChecksumReport(workingDir, fileAcceptRegex, outputReport);
        log.info("Report saved by Omer in " + outputReport.getName());

    }

    private static void writeChecksumReport(File parentDir, final String fileAcceptRegex,
            File outputReport) throws Exception {
        DirectoryFileIterator dfi = new DirectoryFileIterator(parentDir.getAbsolutePath(), true,
                null, createFileFilter(fileAcceptRegex), log);
        String newline = System.getProperty("line.separator");

        BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                outputReport), "UTF-8"));
        try {
            while (dfi.hasNext()) {
                File nextFile = dfi.next();
                String hexChecksum = calculateDigestAsHex(nextFile);
                bf.write(hexChecksum + " " + nextFile.getAbsolutePath() + newline);
            }
        } finally {
            bf.close();
        }
    }

    private static FileFilter createFileFilter(final String regex) {
        if (regex == null) {
            return new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return !file.isHidden();
                }
            };
        } else {
            return new FileFilter() {
                final Pattern pattern = Pattern.compile(regex);

                @Override
                public boolean accept(File file) {
                    return !file.isHidden() && pattern.matcher(file.getName()).matches();
                }
            };
        }
    }

    public static String calculateDigestAsHex(File file) throws Exception {
        // Algorithms guaranteed to be supported:
        // MD2
        // MD5
        // SHA-1
        // SHA-256
        // SHA-384
        // SHA-512
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        InputStream is = new FileInputStream(file);
        try {
            int read;
            byte[] buffer = new byte[4096];
            while ((read = is.read(buffer)) != -1)
                md.update(buffer, 0, read);
        } finally {
            is.close();
        }
        return Hex.encodeHexString(md.digest());
    }
}
