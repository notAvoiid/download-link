package com.abreu.download_link.service;

import com.abreu.download_link.domain.ProcessResult;
import com.abreu.download_link.exceptions.DownloadFailedException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class YoutubeProcessManager {

    private static final String DEFAULT_YT_DLP_PATH =
            System.getProperty("os.name").toLowerCase().contains("win")
                    ? "venv\\Scripts\\yt-dlp.exe"
                    : "/venv/bin/yt-dlp";
    private String YT_DLP_PATH;

    @PostConstruct
    public void init() {
        YT_DLP_PATH = Optional.ofNullable(System.getenv("YT_DLP_PATH"))
                .orElse(DEFAULT_YT_DLP_PATH);

        log.info("Using yt-dlp path: {}", YT_DLP_PATH);

        checkYtDlpInstallation();
    }


    private void checkYtDlpInstallation() {
        log.info("Verifying yt-dlp installation at: {}", YT_DLP_PATH);

        File ytDlpFile = new File(YT_DLP_PATH);

        if (!ytDlpFile.exists()) {
            log.error("yt-dlp not found at: {}", YT_DLP_PATH);
            log.error("Current working directory: {}", System.getProperty("user.dir"));
            log.error("Directory contents:");
            try {
                Path path = YT_DLP_PATH.contains("Scripts")
                        ? Paths.get("venv/Scripts")
                        : Paths.get("/venv/bin");
                Files.list(path).forEach(file -> log.error("- {}", file));
            } catch (IOException e) {
                log.error("Failed to list directory contents", e);
            }
            throw new IllegalStateException("yt-dlp not found at specified path");
        }

        if (!ytDlpFile.canExecute()) {
            log.error("yt-dlp is not executable: {}", YT_DLP_PATH);
            throw new IllegalStateException("yt-dlp is not executable");
        }

        try {
            log.info("Checking yt-dlp version...");
            Process process = new ProcessBuilder(YT_DLP_PATH, "--version").start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("yt-dlp version check failed. Exit code: {}", exitCode);
                throw new IllegalStateException("yt-dlp version check failed");
            }

            log.info("yt-dlp is available. Version: {}", output.toString());
        } catch (IOException | InterruptedException e) {
            log.error("Failed to verify yt-dlp installation", e);
            throw new IllegalStateException("yt-dlp verification failed", e);
        }
    }

    public ProcessResult executeDownload(String url, String downloadDir) throws IOException, InterruptedException {
        List<String> command = createCommand(url);
        Process process = new ProcessBuilder(command)
                .directory(new File(downloadDir))
                .redirectErrorStream(true)
                .start();

        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder errorBuffer = new StringBuilder();

        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuffer.append(line).append("\n");
                    log.info("Process Output: {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading process output: {}", e.getMessage());
            }
        });

        outputThread.start();

        int exitCode = process.waitFor();
        outputThread.join();

        String fullOutput = outputBuffer.toString();
        String filePath = extractFilePath(fullOutput, downloadDir);

        if (exitCode != 0) {
            log.error("Download process failed with exit code {}. Output: {}", exitCode, fullOutput);
            errorBuffer.append("Download failed. Exit code: ").append(exitCode).append("\n");
            errorBuffer.append(fullOutput);
        }

        return new ProcessResult(
                exitCode,
                filePath,
                errorBuffer.toString(),
                fullOutput
        );
    }

    private String extractFilePath(String output, String downloadDir) {
        Pattern pattern = Pattern.compile("\\[ExtractAudio] Destination: (.+\\.mp3)");
        for (String line : output.split("\n")) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return Arrays.stream(output.split("\n"))
                .filter(line -> line.trim().endsWith(".mp3"))
                .findFirst()
                .map(String::trim)
                .orElseThrow(() -> new DownloadFailedException("File path not found in output: " + output));
    }

    public String getExpectedFileName(String url, String downloadDir) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(createCommand(url));
        command.add("--simulate");
        command.add("--print");
        command.add("after_move:filepath");
        command.add("--no-simulate");

        Process process = new ProcessBuilder(command)
                .directory(new File(downloadDir))
                .redirectErrorStream(true)
                .start();

        StringBuilder outputBuffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuffer.append(line).append("\n");
                log.info("yt-dlp output: {}", line);
            }
        }

        int exitCode = process.waitFor();
        String output = outputBuffer.toString().trim();

        log.info("yt-dlp command exited with code: {}", exitCode);
        log.info("Full output: {}", output);

        if (exitCode != 0) {
            String errorMsg = "Failed to retrieve file name. Exit code: " + exitCode + "\nOutput: " + output;
            log.error(errorMsg);
            throw new IOException(errorMsg);
        }

        Optional<String> fileLine = Arrays.stream(output.split("\n"))
                .filter(line -> line.contains(".mp3"))
                .findFirst();

        if (fileLine.isPresent()) {
            String filePath = fileLine.get().trim();
            log.info("Extracted file path: {}", filePath);
            return filePath;
        }

        throw new IOException("File path not found in output: " + output);
    }

    private List<String> createCommand(String url) {
        return List.of(
                YT_DLP_PATH,
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "-o" ,"%(title)s.%(ext)s",
                "--restrict-filenames",
                "--force-overwrites",
                "--no-keep-video",
                url
        );
    }
}
