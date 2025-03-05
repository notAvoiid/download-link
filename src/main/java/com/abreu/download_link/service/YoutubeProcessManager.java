package com.abreu.download_link.service;

import com.abreu.download_link.domain.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
@Slf4j
public class YoutubeProcessManager {
    private static final String YT_DLP_PATH = Optional.ofNullable(System.getenv("YT_DLP_PATH"))
            .orElse("/venv/bin/yt-dlp");
    private final ExecutorService streamReaderExecutor = Executors.newFixedThreadPool(2);

    public ProcessResult executeDownload(String url, String downloadDir) throws IOException, InterruptedException {
        Process process = buildProcess(url, downloadDir);
        return handleProcessOutput(process);
    }

    public String getExpectedFileName(String url, String downloadDir) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(createCommand(url, downloadDir));
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

        return result.output().lines()
                .findFirst()
                .orElseThrow(() -> new IOException("File name not found in output"));
    }

    private Process buildProcess(String url, String downloadDir) throws IOException {
        return new ProcessBuilder(createCommand(url, downloadDir))
                .directory(new File(downloadDir))
                .start();
    }

    private List<String> createCommand(String url, String downloadDir) {
        return List.of(
                YT_DLP_PATH,
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--yes-playlist",
                "--parse-metadata", "%(title)s:%(artist)s - %(title)s",
                "-o", downloadDir + "/%(artist|Unknown)s - %(title)s.%(ext)s",
                "--restrict-filenames",
                "--force-overwrites",
                "-c",
                url
        );
    }

    private ProcessResult handleProcessOutput(Process process) throws InterruptedException {
        List<String> outputLines = Collections.synchronizedList(new ArrayList<>());
        List<String> errorLines = Collections.synchronizedList(new ArrayList<>());

        CompletableFuture<Void> stdoutFuture = readStreamAsync(process.getInputStream(), outputLines, log::info);
        CompletableFuture<Void> stderrFuture = readStreamAsync(process.getErrorStream(), errorLines, log::error);

        int exitCode = process.waitFor();
        CompletableFuture.allOf(stdoutFuture, stderrFuture).join();

        return new ProcessResult(
                exitCode,
                String.join("\n", outputLines),
                String.join("\n", errorLines)
        );
    }

    private CompletableFuture<Void> readStreamAsync(InputStream inputStream, List<String> lines, Consumer<String> logger) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.accept(line);
                    lines.add(line);
                }
            } catch (IOException e) {
                log.error("Error reading process stream: {}", e.getMessage());
            }
        }, streamReaderExecutor);
    }
}
