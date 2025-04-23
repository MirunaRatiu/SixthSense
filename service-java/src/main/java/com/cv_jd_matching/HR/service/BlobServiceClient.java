package com.cv_jd_matching.HR.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.error.InvalidFileFormatException;
import com.cv_jd_matching.HR.parser.FileTextExtractor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.cv_jd_matching.HR.parser.CvParser;
import com.cv_jd_matching.HR.parser.JobDescriptionParser;
import com.cv_jd_matching.HR.repository.ICvRepository;
import com.cv_jd_matching.HR.repository.IJobDescriptionRepository;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.cv_jd_matching.HR.parser.FileTextExtractor;
import org.springframework.web.multipart.MultipartFile;


@Service
public class BlobServiceClient {
    @Value("${spring.cloud.azure.storage.connection-string}")
    private String connectionString;

    @Value("${spring.cloud.azure.storage.blob.container-name-1}")
    private String containerName1;

    @Value("${spring.cloud.azure.storage.blob.container-name-2}")
    private String containerName2;

    @Autowired
    private ICvRepository cvRepository;

    @Autowired
    private IJobDescriptionRepository jobDescriptionRepository;

    private BlobContainerClient getContainerClient(String containerName) {
        com.azure.storage.blob.BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        return blobServiceClient.getBlobContainerClient(containerName);

    }

    public String uploadCv(MultipartFile file) throws IOException, InvalidFileFormatException {
        BlobContainerClient containerClient = getContainerClient(containerName1);
        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name is missing.");
        }
        String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String content = FileTextExtractor.extractTextFromFile(new ByteArrayInputStream(fileBytes), originalFilename);
        Map<String, Object> cvData = CvParser.parseCv(content);

        String extractedName = (String) cvData.get("name");
        System.out.println("extractedName"+extractedName);
        if (extractedName == null || extractedName.trim().isEmpty()) {
            throw new InvalidFileFormatException("Could not extract name from the CV.");
        }

        int currentCvsNumber = cvRepository.countAll() + 1;
        String correctFileName = "cv_" + currentCvsNumber + "_" + extractedName.replaceAll("[ -]", "_") + extension;
        System.out.println("Correct File Name: " + correctFileName);
        System.out.println("Extracted Name: " + extractedName);

        BlobClient blobClient = containerClient.getBlobClient(correctFileName);
        InputStream newFileStream = new ByteArrayInputStream(fileBytes);
        blobClient.upload(newFileStream, fileBytes.length, true);

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);
        BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        String sasToken = blobClient.generateSas(sasValues);
        String urlWithSas = blobClient.getBlobUrl() + "?" + sasToken;

        try {
            Cv cv = new Cv();
            cv.setName(extractedName);
            cv.setTechnicalSkills(String.valueOf((List<String>) cvData.get("technical_skills")));
            cv.setForeignLanguages(String.valueOf((List<String>) cvData.get("foreign_languages")));
            cv.setEducation(String.valueOf((List<String>) cvData.get("education")));
            cv.setCertifications(String.valueOf((List<String>) cvData.get("certifications")));
            cv.setProjectExperience(String.valueOf((List<String>) cvData.get("project_experience")));
            cv.setOthers(String.valueOf((List<String>) cvData.get("others")));
            cv.setWorkExperience(String.valueOf((List<String>) cvData.get("work_experience")));
            cv.setFileName(correctFileName);
            cv.setPathName(urlWithSas);

            cvRepository.save(cv);

            return urlWithSas;
        } catch (Exception e) {
            blobClient.delete();
            throw e;
        }
    }


    public static String extractCoreJobTitle(String jobTitle) {
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            return "JobTitle";
        }

        List<String> seniorityTerms = Arrays.asList(
                "junior", "mid-level", "senior", "tech lead", "team lead", "lead developer",
                "principal engineer", "staff engineer", "architect", "it manager", "cto"
        );

        String[] words = jobTitle.trim().split("\s+");
        int i = 0;

        while (i < words.length) {
            String currentWord = words[i].toLowerCase();
            String nextWord = (i + 1 < words.length) ? words[i + 1].toLowerCase() : "";
            String combined = currentWord + " " + nextWord;

            if (seniorityTerms.contains(combined)) {
                i += 2;
            } else if (seniorityTerms.contains(currentWord)) {
                i += 1;
            } else {
                break;
            }
        }

        if (i >= words.length) {
            return "JobTitle";
        }

        return String.join(" ", Arrays.copyOfRange(words, i, words.length));
    }


    public String uploadJobDescription(MultipartFile file) throws IOException {
        BlobContainerClient containerClient = getContainerClient(containerName2);
        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name is missing.");
        }

        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String content = FileTextExtractor.extractTextFromFile(new ByteArrayInputStream(fileBytes), originalFilename);
        Map<String, Object> jdData = JobDescriptionParser.parseJd(content);

        String jobTitle = (String) jdData.get("job_title");
        jobTitle = extractCoreJobTitle(jobTitle);
        System.out.println(jdData);

// ia doar partea de după ultimul "-"
        if (jobTitle.contains("-")) {
            jobTitle = jobTitle.substring(jobTitle.lastIndexOf("-") + 1).trim();
        }

        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            jobTitle = "UnknownJobTitle";
        }

        int currentJdsNumber = jobDescriptionRepository.countAll() + 1;
        String correctFileName = "jd_" + currentJdsNumber + "_" + jobTitle.replaceAll("[^a-zA-Z0-9]", "_") + extension;

        BlobClient blobClient = containerClient.getBlobClient(correctFileName);
        InputStream newFileStream = new ByteArrayInputStream(fileBytes);
        blobClient.upload(newFileStream, fileBytes.length, true);

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);
        BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        String sasToken = blobClient.generateSas(sasValues);
        String urlWithSas = blobClient.getBlobUrl() + "?" + sasToken;

        try {
            JobDescription jd = new JobDescription();
            jd.setFileName(correctFileName);
            jd.setPathName(urlWithSas);
            jd.setJobTitle((String) jdData.get("job_title"));
            jd.setCompanyOverview((String) jdData.get("company_overview"));
            jd.setKeyResponsibilities(
                    ((List<?>) jdData.get("key_responsibilities"))
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.joining("\n"))
            );
            jd.setRequiredQualifications(((List<?>) jdData.get("required_qualifications"))
                    .stream().map(Object::toString).collect(Collectors.joining("\n")));
            jd.setPreferredSkills(((List<?>) jdData.get("preferred_skills"))
                    .stream().map(Object::toString).collect(Collectors.joining("\n")));
            jd.setBenefits(((List<?>) jdData.get("benefits"))
                    .stream().map(Object::toString).collect(Collectors.joining("\n")));

            jd.setMessage((String) jdData.get("message"));

            jobDescriptionRepository.save(jd);

            return urlWithSas;
        } catch (Exception e) {
            blobClient.delete();
            throw e;
        }
    }


}
