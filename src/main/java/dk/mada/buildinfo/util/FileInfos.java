package dk.mada.buildinfo.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility to capture the necessary output file information.
 */
public final class FileInfos {
    /** The buffer size to use when calculating checksums. */
    private static final int CHECKSUM_BUFFER_SIZE = 8192;

    private FileInfos() {
    }

    /**
     * The captured file information.
     *
     * @param file      the file the information is from
     * @param size      the file's size
     * @param sha512sum the file's sha512 checksum
     */
    public record FileInfo(Path file, long size, String sha512sum) {
    }

    /**
     * Creates file information from file.
     *
     * @param file the file to get information from
     * @return information about the file
     */
    public static FileInfo from(Path file) {
        try {
            return new FileInfo(file, Files.size(file), sha512sum(file));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to capture data from file " + file, e);
        }
    }

    private static String sha512sum(Path file) throws IOException {
        MessageDigest md;
        byte[] buffer = new byte[CHECKSUM_BUFFER_SIZE];
        try (InputStream is = Files.newInputStream(file)) {
            md = MessageDigest.getInstance("SHA-512");
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to get digester for sha-512", e);
        }
        return HexFormat.of().formatHex(md.digest());
    }
}
