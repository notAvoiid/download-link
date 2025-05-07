package com.abreu.download_link.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class FileMetadata {
    private boolean inUse;
    private Instant lastUpdated;

    public FileMetadata(boolean inUse) {
        this.inUse = inUse;
        this.lastUpdated = Instant.now();
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
        this.lastUpdated = Instant.now();
    }

}
