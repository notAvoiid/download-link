package com.abreu.download_link.service;

import com.abreu.download_link.domain.ProcessResult;
import com.abreu.download_link.exceptions.DownloadFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class YoutubeProcessManager {

    private static final String YT_DLP_PATH = Optional.ofNullable(System.getenv("YT_DLP_PATH"))
            .orElse("yt-dlp");

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
        return Arrays.stream(output.split("\n"))
                .filter(line -> line.contains("[ExtractAudio] Destination:") ||
                        line.endsWith(".mp3"))
                .map(line -> {
                    if (line.contains("Destination:")) {
                        return Paths.get(downloadDir, line.replace("[ExtractAudio] Destination:", "").trim()).toString();
                    }
                    return Paths.get(downloadDir, line.trim()).toString();
                })
                .findFirst()
                .orElseThrow(() -> new DownloadFailedException("File path not found in output: " + output));
    }

    public String getExpectedFileName(String url, String downloadDir) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(createCommand(url));
        command.add("--simulate");
        command.add("--print");
        command.add("filename");

        Process process = new ProcessBuilder(command)
                .directory(new File(downloadDir))
                .redirectErrorStream(true)
                .start();

        ProcessResult result = handleProcessOutput(process);
        if (result.exitCode() != 0) {
            throw new IOException("Failed to retrieve file name: " + result.error());
        }

        return Paths.get(downloadDir, result.output().trim()).toString();
    }

    private List<String> createCommand(String url) {
        return List.of(
                YT_DLP_PATH,
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--yes-playlist",
                "--parse-metadata", "%(title)s:%(artist)s - %(title)s",
                "-o", "%(artist|Unknown)s - %(title)s.%(ext)s",
                "--restrict-filenames",
                "--force-overwrites",
                "--no-keep-video",
                "-c",
                url
        );
    }

    private ProcessResult handleProcessOutput(Process process) throws InterruptedException, IOException {
        StringBuilder outputBuffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuffer.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        String output = outputBuffer.toString().trim();
        String firstLine = output.split("\n")[0].trim();

        return new ProcessResult(
                exitCode,
                firstLine,
                "",
                output
        );
    }

}
