package com.abreu.download_link.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
@Slf4j
public class YoutubeDownloadService {

    private static final String DOWNLOAD_DIR = System.getProperty("user.dir") + "/downloads";
    private static final String YT_DLP_PATH = System.getenv("YT_DLP_PATH") != null ? System.getenv("YT_DLP_PATH") : "/venv/bin/yt-dlp";

    public String downloadAudio(String youtubeUrl) throws Exception {
        Process process = getProcess(youtubeUrl);
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Download failed with exit code: {}", exitCode);
            throw new RuntimeException("Download failed with exit code: " + exitCode);
        }

        String downloadedFile = extractFilename(output.toString());
        if (downloadedFile != null) {
            log.info("Downloaded file: {}", downloadedFile);
        } else {
            log.warn("Downloaded file not found in the output. Check directory: {}", DOWNLOAD_DIR);
        }
        return "Download completed: " + (downloadedFile != null ? downloadedFile : "Check " + DOWNLOAD_DIR);
    }

    private static Process getProcess(String youtubeUrl) throws IOException {
        File downloadDirectory = new File(DOWNLOAD_DIR);
        if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
            log.error("Failed to create download directory: {}", DOWNLOAD_DIR);
            throw new IOException("Failed to create download directory: " + DOWNLOAD_DIR);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
        "/bin/bash", "-c",
                YT_DLP_PATH + " -x --audio-format mp3 --audio-quality 0 -o \""
                + DOWNLOAD_DIR + "/%(title)s.%(ext)s\" " + youtubeUrl
                + " && chmod -R 777 " + DOWNLOAD_DIR
        );
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    private String extractFilename(String output) {
        for (String line : output.split("\n")) {
            if (line.contains("[ExtractAudio] Destination:")) {
                return line.replace("[ExtractAudio] Destination: ", "").trim();
            }
        }
        return null;
    }
}