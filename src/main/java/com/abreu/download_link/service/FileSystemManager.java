package com.abreu.download_link.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.Set;

@Component
@Slf4j
public class FileSystemManager {

    public void createDirectoryWithPermissions(String path) throws IOException {
        Path dirPath = Paths.get(path).normalize();

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("Created directory: {}", dirPath);
        } else {
            log.debug("Reusing existing directory: {}", dirPath);
        }

        setAppropriatePermissions(dirPath);
    }

    private void setAppropriatePermissions(Path path) throws IOException {
        if (isWindows()) {
            setWindowsPermissions(path);
        } else if (isPosixCompatible()) {
            setPosixPermissions(path);
        } else {
            log.warn("Unsupported file system. Using default permissions for: {}", path);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private boolean isPosixCompatible() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    private void setPosixPermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-x---"));
            log.debug("Set POSIX permissions (750) for: {}", path);
        } catch (IOException e) {
            log.error("Error setting POSIX permissions for {}: {}", path, e.getMessage());
            throw e;
        }
    }

    private void setWindowsPermissions(Path path) throws IOException {
        try {
            AclFileAttributeView aclView = Files.getFileAttributeView(
                    path,
                    AclFileAttributeView.class
            );

            if (aclView == null) {
                log.warn("ACL view not supported for: {}", path);
                return;
            }

            UserPrincipal owner = Files.getOwner(path);
            Set<AclEntryPermission> fullControl = Set.of(
                    AclEntryPermission.READ_DATA,
                    AclEntryPermission.WRITE_DATA,
                    AclEntryPermission.APPEND_DATA,
                    AclEntryPermission.READ_ATTRIBUTES,
                    AclEntryPermission.WRITE_ATTRIBUTES,
                    AclEntryPermission.READ_NAMED_ATTRS,
                    AclEntryPermission.WRITE_NAMED_ATTRS,
                    AclEntryPermission.DELETE,
                    AclEntryPermission.READ_ACL,
                    AclEntryPermission.WRITE_ACL,
                    AclEntryPermission.SYNCHRONIZE
            );

            AclEntry entry = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(owner)
                    .setPermissions(fullControl)
                    .build();

            aclView.setAcl(Collections.singletonList(entry));
            log.debug("Set Windows permissions (FullControl to owner) for: {}", path);

        } catch (IOException e) {
            log.error("Error setting Windows permissions for {}: {}", path, e.getMessage());
            throw e;
        }
    }
}