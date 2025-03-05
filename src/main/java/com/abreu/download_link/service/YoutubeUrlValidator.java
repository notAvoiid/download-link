package com.abreu.download_link.service;

import com.abreu.download_link.exceptions.InvalidYoutubeUrlException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class YoutubeUrlValidator {
    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.*"
    );
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    public String validateAndExtractVideoId(String url) {
        if (!YOUTUBE_URL_PATTERN.matcher(url).matches()) {
            throw new InvalidYoutubeUrlException("Invalid URL: " + url);
        }

        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new InvalidYoutubeUrlException("Video ID not found in URL: " + url);
    }
}
