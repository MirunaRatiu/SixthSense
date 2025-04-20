package com.cv_jd_matching.HR.controller;

import com.cv_jd_matching.HR.error.InvalidFileFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import com.cv_jd_matching.HR.service.BlobServiceClient;

import java.io.IOException;

@Controller
@RequestMapping("/api/files")

public class FileUploadController {
    @Autowired
    private BlobServiceClient blobServiceClient;

    @PostMapping("/upload-cv")
    @ResponseBody

    public String uploadCv(@RequestParam("file") MultipartFile file) {
        try {
            String uploadedCvUrl = blobServiceClient.uploadCv(file);
            return "{\"message\": \"CV uploaded successfully. URL: " + uploadedCvUrl + "\"}";
        } catch (MultipartException e) {
            return "{\"error\": \"Invalid multipart request: " + e.getMessage() + "\"}";
        } catch (IOException e) {
            return "{\"error\": \"Error uploading the CV: " + e.getMessage() + "\"}";
        } catch (InvalidFileFormatException e) {
            return "{\"error\": \"Error processing the CV: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"An unexpected error occurred: " + e.getMessage() + "\"}";
        }
    }


    @PostMapping("/upload-jobDescription")
    @ResponseBody
    public String uploadJobDescription(@RequestParam("file") MultipartFile file) {
        try {
            String uploadedJobDescriptionUrl = blobServiceClient.uploadJobDescription(file);
            return "{\"message\": \"Job Description uploaded successfully. URL: " + uploadedJobDescriptionUrl + "\"}";
        } catch (IOException e) {
            return "{\"error\": \"Error uploading the JobDescription: " + e.getMessage() + "\"}";
        } catch (InvalidFileFormatException e) {
            return "{\"error\": \"Invalid file format for Job Description: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"An unexpected error occurred during Job Description upload: " + e.getMessage() + "\"}";
        }
    }
}
