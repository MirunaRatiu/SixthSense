package com.cv_jd_matching.HR.controller;


import com.cv_jd_matching.HR.service.GoogleViewerUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/view")
public class DocumentViewController {

    private final GoogleViewerUrlService googleViewerUrlService;

    @Autowired
    public DocumentViewController(GoogleViewerUrlService googleViewerUrlService) {
        this.googleViewerUrlService = googleViewerUrlService;
    }

    @GetMapping("/generate-viewer-url")
    public ResponseEntity<Map<String, String>> getViewerUrl(@RequestParam("docUrl") String docUrl) {

        String viewerUrl = googleViewerUrlService.createGoogleViewerUrl(docUrl);

        if (viewerUrl != null) {
            Map<String, String> response = new HashMap<>();
            response.put("originalUrl", docUrl);
            response.put("viewerUrl", viewerUrl);
            return ResponseEntity.ok(response);
        } else {

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate viewer URL. Check input URL.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}
