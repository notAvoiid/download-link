package com.abreu.download_link.service;

import com.abreu.download_link.domain.enums.Status;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DownloadStatusManager {
    private final ConcurrentMap<String, Status> statusMap = new ConcurrentHashMap<>();

    public void updateStatus(String videoId, Status status) {
        statusMap.put(videoId, status);
    }

    public Status getStatus(String videoId) {
        return statusMap.getOrDefault(videoId, Status.NOT_FOUND);
    }

}