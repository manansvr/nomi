package com.nomi.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LocalJsonStore {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LocalJsonStore() {
    }

    public static <T> T read(Path path, TypeReference<T> type, T fallback) {
        if (!Files.exists(path)) return fallback;
        try {
            return MAPPER.readValue(path.toFile(), type);
        } catch (IOException error) {
            throw new IllegalStateException("Could not read local nomi data from " + path, error);
        }
    }

    public static void write(Path path, Object value) {
        Path temporaryFile = null;
        try {
            Files.createDirectories(path.getParent());
            temporaryFile = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), value);
            try {
                Files.move(temporaryFile, path, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupError) {
                    error.addSuppressed(cleanupError);
                }
            }
            throw new IllegalStateException("Could not save local nomi data to " + path, error);
        }
    }
}
