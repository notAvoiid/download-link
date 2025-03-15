package com.abreu.download_link.service;

import com.abreu.download_link.domain.DownloadStatus;
import com.abreu.download_link.domain.enums.Status;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
public class DownloadStatusManager {

    private final ConcurrentHashMap<String, DownloadStatus> statusMap = new ConcurrentHashMap<>();

    public void updateStatus(String url, Status status) {
        statusMap.put(url, new DownloadStatus(status, ""));
    }

    public void updateStatus(String url, Status status, String message) {
        statusMap.put(url, new DownloadStatus(status, message));
    }

    public DownloadStatus getStatus(String url) {
        return statusMap.getOrDefault(url, new DownloadStatus(Status.NOT_FOUND, ""));
    }

}