package com.abreu.download_link.service;

import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.domain.YoutubeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

@Service
@Slf4j
public class YoutubeDownloadService {

    private static final String DOWNLOAD_DIR = System.getProperty("user.dir") + "/downloads";
    private static final String YT_DLP_PATH = System.getenv("YT_DLP_PATH") != null ? System.getenv("YT_DLP_PATH") : "/venv/bin/yt-dlp";

    public CompletableFuture<YoutubeResponse> downloadAudio(YoutubeLinkRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!request.url().matches("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.*")) {
                    throw new IllegalArgumentException("Invalid YouTube URL");
                }

                Process process = getProcess(request.url());

                List<String> outputLines = Collections.synchronizedList(new ArrayList<>());
                Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), log::info, outputLines));
                Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), log::error));

                stdoutThread.start();
                stderrThread.start();

                int exitCode = process.waitFor();
                stdoutThread.join();
                stderrThread.join();

                if (exitCode != 0) {
                    return new YoutubeResponse(false, "Download failed with exit code: " + exitCode, null);
                }

                String filePath = outputLines.stream()
                        .filter(line -> line.contains("[ExtractAudio] Destination:"))
                        .map(line -> line.replace("[ExtractAudio] Destination: ", "").trim())
                        .findFirst()
                        .orElse(null);

                if (filePath == null) {
                    return new YoutubeResponse(false, "Download completed, but file path could not be determined.", null);
                }

                return new YoutubeResponse(true, "Download completed successfully.", filePath);
            } catch (IOException e) {
                throw new CompletionException("I/O error during download", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException("Download interrupted", e);
            } catch (Exception e) {
                throw new CompletionException("Error downloading audio: " + e.getMessage(), e);
            }
        });
    }

    private static Process getProcess(String youtubeUrl) throws IOException {
        File downloadDirectory = new File(DOWNLOAD_DIR);
        if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
            log.error("Failed to create download directory: {}", DOWNLOAD_DIR);
            throw new IOException("Failed to create download directory: " + DOWNLOAD_DIR);
        }

        List<String> command = List.of(
                YT_DLP_PATH,
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--yes-playlist",
                "--parse-metadata", "%(title)s:%(artist)s - %(title)s",
                "-o", DOWNLOAD_DIR + "/%(artist|Desconhecido)s - %(title)s.%(ext)s",
                "--restrict-filenames",
                "-c",
                youtubeUrl
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(downloadDirectory);
        Process process = processBuilder.start();

        try {
            Files.setPosixFilePermissions(Paths.get(DOWNLOAD_DIR), PosixFilePermissions.fromString("rwxrwxrwx"));
        } catch (UnsupportedOperationException e) {
            log.warn("Skipping file permission setting. Unsupported on this OS.");
        }

        return process;
    }

    private void readStream(InputStream inputStream, Consumer<String> logger, List<String> outputLines) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.accept(line);
                outputLines.add(line);
            }
        } catch (IOException e) {
            log.error("Error reading process stream", e);
        }
    }

    private void readStream(InputStream inputStream, Consumer<String> logger) {
        readStream(inputStream, logger, new ArrayList<>());
    }
}