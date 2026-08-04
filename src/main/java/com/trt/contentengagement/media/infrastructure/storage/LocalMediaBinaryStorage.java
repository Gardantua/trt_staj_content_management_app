package com.trt.contentengagement.media.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.trt.contentengagement.media.application.MediaBinaryStorage;
import com.trt.contentengagement.media.domain.MediaRuleViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalMediaBinaryStorage implements MediaBinaryStorage {
    private final Path storageRoot;

    public LocalMediaBinaryStorage(@Value("${app.media.storage-directory}") String storageDirectory) {
        storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException directoryFailure) {
            throw new IllegalStateException("Media storage directory could not be created.", directoryFailure);
        }
    }

    @Override
    public void store(String storageKey, byte[] content) {
        Path target = resolve(storageKey);
        try {
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException storageFailure) {
            throw new MediaRuleViolationException(
                    "MEDIA_STORAGE_FAILED", "Image content could not be stored."
            );
        }
    }

    @Override
    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException readFailure) {
            throw new MediaRuleViolationException(
                    "MEDIA_CONTENT_UNAVAILABLE", "Image content is temporarily unavailable."
            );
        }
    }

    private Path resolve(String storageKey) {
        if (!storageKey.matches("[0-9a-fA-F-]{36}")) {
            throw new MediaRuleViolationException(
                    "MEDIA_INVALID_STORAGE_KEY", "Media storage key is invalid."
            );
        }
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.getParent().equals(storageRoot)) {
            throw new MediaRuleViolationException(
                    "MEDIA_INVALID_STORAGE_KEY", "Media storage key is invalid."
            );
        }
        return resolved;
    }
}
