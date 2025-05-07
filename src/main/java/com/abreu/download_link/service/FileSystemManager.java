package com.abreu.download_link.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

@Component
@Slf4j
public class FileSystemManager {

    public void createDirectoryWithPermissions(String path) throws IOException {
        Path dirPath = Paths.get(path);

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        } else {
            log.debug("Reusing existing directory: {}", dirPath.toAbsolutePath());
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

    private String getPosixPermissions(Path path) {
        if (isPosixCompatible()) {
            try {
                return PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
            } catch (IOException e) {
                return "Error reading permissions: " + e.getMessage();
            }
        }
        return "POSIX not supported";
    }
}