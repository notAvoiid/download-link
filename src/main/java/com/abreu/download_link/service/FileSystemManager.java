package com.abreu.download_link.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;

@Component
@Slf4j
public class FileSystemManager {

    public void createDirectoryWithPermissions(String path) throws IOException {
        Path dirPath = Paths.get(path);

        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
                log.info("Directory created: {}", dirPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Error creating directory: {}", e.getMessage());
                throw new IOException("Failed to create directory: " + path, e);
            }
        }
        setDirectoryPermissions(dirPath);
    }

    private void setDirectoryPermissions(Path path) {
        if (isPosixCompatible()) {
            try {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"));
                log.debug("Permissions set for: {}", path);
            } catch (IOException e) {
                log.error("Error setting POSIX permissions: {}", e.getMessage());
            }
        } else {
            log.info("System does not support POSIX permissions. Permissions were not changed.");
        }
    }

    private boolean isPosixCompatible() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    public boolean cleanDirectory(String path) {
        try (Stream<Path> stream = Files.walk(Paths.get(path))) {
            return stream.filter(Files::isRegularFile)
                    .allMatch(this::deleteFile);
        } catch (IOException e) {
            log.error("Error cleaning directory: {}", e.getMessage());
            return false;
        }
    }

    private boolean deleteFile(Path filePath) {
        try {
            Files.delete(filePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", filePath, e.getMessage());
            return false;
        }
    }
}
