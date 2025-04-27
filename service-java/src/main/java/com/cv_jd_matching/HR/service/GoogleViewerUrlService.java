package com.cv_jd_matching.HR.service;

import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleViewerUrlService {

    private static final String VIEWER_BASE = "https://docs.google.com/gview?url=";
    private static final String SUFFIX = "&embedded=true";

    public String createGoogleViewerUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            return null;
        }

            try {
                String encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8.toString());

                StringBuilder finalUrlBuilder = new StringBuilder(VIEWER_BASE);
                finalUrlBuilder.append(encodedUrl);
                finalUrlBuilder.append(SUFFIX);

                String finalUrl = finalUrlBuilder.toString();

                return finalUrl;

            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
}

