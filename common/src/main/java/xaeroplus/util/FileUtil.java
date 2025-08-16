package xaeroplus.util;

import java.io.File;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public class FileUtil {
    private FileUtil() {}

    public static void safeSave(File outputFile, Consumer<Writer> fileWriter) throws RuntimeException {
        try {
            Path tempFilePath = Files.createTempFile("xaeroplus", ".tmp");
            try (var writer = Files.newBufferedWriter(tempFilePath)) {
                fileWriter.accept(writer);
            }
            try {
                Files.move(tempFilePath, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFilePath, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final Exception e) {
            throw new RuntimeException("Error during safeSave", e);
        }
    }
}
