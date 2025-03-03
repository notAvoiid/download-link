package com.abreu.download_link.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

@Service
@Slf4j
public class YoutubeDownloadService {

    private static final String DOWNLOAD_DIR = System.getProperty("user.dir") + "/downloads";
    private static final String YT_DLP_PATH = System.getenv("YT_DLP_PATH") != null ? System.getenv("YT_DLP_PATH") : "/venv/bin/yt-dlp";

    public String downloadAudio(String youtubeUrl) throws Exception {
        Process process = getProcess(youtubeUrl);

        Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), true));
        Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), false));

        stdoutThread.start();
        stderrThread.start();

        stdoutThread.join();
        stderrThread.join();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Download failed with exit code: {}", exitCode);
            throw new RuntimeException("Download failed with exit code: " + exitCode);
        }

        log.info("Download completed. Check directory: {}", DOWNLOAD_DIR);
        return "Download completed. Check " + DOWNLOAD_DIR;
    }

    private static Process getProcess(String youtubeUrl) throws IOException {
        File downloadDirectory = new File(DOWNLOAD_DIR);
        if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
            log.error("Failed to create download directory: {}", DOWNLOAD_DIR);
            throw new IOException("Failed to create download directory: " + DOWNLOAD_DIR);
        }

        List<String> command = List.of(
                YT_DLP_PATH, "-x", "--audio-format", "mp3", "--audio-quality", "0",
                "-o", DOWNLOAD_DIR + "/%(title)s.%(ext)s",
                youtubeUrl
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(downloadDirectory);
        Process process = processBuilder.start();

        try {
            Files.setPosixFilePermissions(Paths.get(DOWNLOAD_DIR), PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException e) {
            log.warn("Skipping file permission setting. Unsupported on this OS.");
        }

        return process;
    }

    private void readStream(InputStream inputStream, boolean isStdOut) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isStdOut) {
                    log.info(line);
                } else {
                    log.error(line);
                }
            }
        } catch (IOException e) {
            log.error("Error reading process stream", e);
        }
    }
}
