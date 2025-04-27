package com.cv_jd_matching.HR.controller;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.error.InvalidFileFormatException;
import com.cv_jd_matching.HR.service.CvService;
import com.cv_jd_matching.HR.service.JobDescriptionService;
import com.cv_jd_matching.HR.service.MatchingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import com.cv_jd_matching.HR.service.BlobServiceClient;

import java.io.IOException;
import java.util.Optional;
@CrossOrigin(origins = "http://localhost:4200") // Allow requests from http://localhost:4200
@Controller
@RequestMapping("/api/files")

public class FileUploadController {
    @Autowired
    private BlobServiceClient blobServiceClient;

    @Autowired
    private JobDescriptionService jobDescriptionService;

    @Autowired
    private MatchingClient matchingClient;

    @Autowired
    private CvService cvService;

    @PostMapping("/upload-cv")
    @ResponseBody

    public String uploadCv(@RequestParam("file") MultipartFile file) {
        try {
            String uploadedCvUrl = blobServiceClient.uploadCv(file);
            //embed
            CvDTO cvDTO = cvService.getCvByPath(uploadedCvUrl);
            matchingClient.embedCv(cvDTO).subscribe();
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
        String uploadedJobDescriptionUrl;
        try {
            uploadedJobDescriptionUrl = blobServiceClient.uploadJobDescription(file);
            //embed the file
            JobDescriptionDTO job = jobDescriptionService.getJobDescriptionByPath(uploadedJobDescriptionUrl);
            matchingClient.embedJobDescription(job).subscribe();
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
