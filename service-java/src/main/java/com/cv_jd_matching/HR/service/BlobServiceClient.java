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
import jakarta.transaction.Transactional;
import org.apache.tika.Tika;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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


    @Transactional
    public String uploadCv(MultipartFile file) throws IOException, InvalidFileFormatException {
        BlobContainerClient containerClient = getContainerClient(containerName1);
        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("File name is missing.");
        }

        String content = FileTextExtractor.extractTextFromFile(new ByteArrayInputStream(fileBytes), originalFilename);
        Map<String, Object> cvData = CvParser.parseCv(content);
        String extractedName = (String) cvData.get("name");

        if (extractedName == null || extractedName.trim().isEmpty()) {
            throw new InvalidFileFormatException("Could not extract name from the CV.");
        }

        String normalizedName = extractedName.trim().replaceAll("[\\s-]+", "_");

        Optional<Cv> existingCvOptional = cvRepository.findFirstByFileNameContaining(normalizedName);

        Cv cvToSave;
        String finalFileName;
        boolean isUpdate = false;
        String oldBlobNameToDelete = null;

        if (existingCvOptional.isPresent()) {
            isUpdate = true;
            Cv existingCv = existingCvOptional.get();
            cvToSave = existingCv;
            finalFileName = existingCv.getFileName();
            oldBlobNameToDelete = finalFileName;

        } else {
            isUpdate = false;

            long nextCvNumber = 1;
            Optional<Cv> lastCvOptional = cvRepository.findTopByOrderByFileNameDesc();
            if (lastCvOptional.isPresent()) {
                String lastFileName = lastCvOptional.get().getFileName();
                String[] parts = lastFileName.split("_");
                if (parts.length > 1) {
                    try {
                        long lastNumber = Long.parseLong(parts[1]);
                        nextCvNumber = lastNumber + 1;
                    } catch (NumberFormatException e) {
                        System.err.println("Could not parse number from last CV filename: " + lastFileName);
                    }
                }
            }
            String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            finalFileName = "cv_" + nextCvNumber + "_" + normalizedName + extension;

            cvToSave = new Cv();
            cvToSave.setFileName(finalFileName);
        }

        BlobClient blobClient = containerClient.getBlobClient(finalFileName);
        InputStream newFileStream = new ByteArrayInputStream(fileBytes);
        try {
            blobClient.upload(newFileStream, fileBytes.length, true);

            if (isUpdate && oldBlobNameToDelete != null && !oldBlobNameToDelete.equals(finalFileName)) {
                BlobClient oldBlobClient = containerClient.getBlobClient(oldBlobNameToDelete);
                oldBlobClient.deleteIfExists();
            }

        } catch (Exception e) {
            throw new IOException("Failed to upload CV file to storage for " + finalFileName, e);
        }

        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(30);
        BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        String sasToken = blobClient.generateSas(sasValues);
        String urlWithSas = blobClient.getBlobUrl() + "?" + sasToken;

        cvToSave.setName(extractedName);
        try {
            cvToSave.setTechnicalSkills(objectToStringRepresentation(cvData.get("technical_skills")));
            cvToSave.setForeignLanguages(objectToStringRepresentation(cvData.get("foreign_languages")));
            cvToSave.setEducation(objectToStringRepresentation(cvData.get("education")));
            cvToSave.setCertifications(objectToStringRepresentation(cvData.get("certifications")));
            cvToSave.setProjectExperience(objectToStringRepresentation(cvData.get("project_experience")));
            cvToSave.setOthers(objectToStringRepresentation(cvData.get("others")));
            cvToSave.setWorkExperience(objectToStringRepresentation(cvData.get("work_experience")));
        } catch (ClassCastException e) {
            throw new InvalidFileFormatException("Internal error processing parsed CV data types: " + e.getMessage());

        }
        cvToSave.setPathName(urlWithSas);

        try {
            Cv savedCv = cvRepository.save(cvToSave);
            return urlWithSas;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save CV record to database for file " + finalFileName, e);
        }
    }


    private String objectToStringRepresentation(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            if (list.isEmpty()) {
                return "";
            }

            return list.toString();
        }
        return data.toString();
    }


    public static String extractCoreJobTitle(String jobTitle) {
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            return "JobTitle";
        }

        List<String> seniorityTerms = Arrays.asList(
                "junior", "mid-level", "senior", "tech lead", "team lead", "lead developer",
                "principal engineer", "staff engineer", "architect", "it manager", "cto"
        );

        String[] words = jobTitle.trim().split("\\s+");
        int i = 0;
        int lastWords = 0;

        while (i < words.length) {
            String currentWord = words[i].toLowerCase();
            String nextWord = (i + 1 < words.length) ? words[i + 1].toLowerCase() : "";
            String combined = currentWord + " " + nextWord;

            lastWords = 0;
            if (seniorityTerms.contains(combined)) {
                i += 2;
                lastWords += 2;
            } else if (seniorityTerms.contains(currentWord)) {
                i += 1;
                lastWords += 1;
            } else {
                break;
            }
        }

        if (i >= words.length) {
            i -= lastWords;
        }

        String coreTitle = String.join(" ", Arrays.copyOfRange(words, i, words.length));


        if (words.length >= 2) {
            String lastTwo = words[words.length - 2].toLowerCase() + " " + words[words.length - 1].toLowerCase();
            if (seniorityTerms.contains(lastTwo)) {
                return capitalizeFully(lastTwo);
            }
        }

        return coreTitle;
    }

    private static String capitalizeFully(String input) {
        String[] parts = input.split("\\s+");
        return Arrays.stream(parts)
                .map(s -> s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
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


        if (jobTitle.contains("-")) {
            jobTitle = jobTitle.substring(jobTitle.lastIndexOf("-") + 1).trim();
        }

        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            jobTitle = "UnknownJobTitle";
        }


        long nextJdNumber = 1;
        Optional<JobDescription> lastJd = jobDescriptionRepository.findTopByOrderByFileNameDesc();
        if (lastJd.isPresent()) {
            String lastFileName = lastJd.get().getFileName();
            String[] parts = lastFileName.split("_");
            if (parts.length > 1) {
                try {
                    long lastNumber = Long.parseLong(parts[1]);
                    nextJdNumber = lastNumber + 1;
                } catch (NumberFormatException e) {
                    System.err.println("Could not parse number from last JD filename: " + lastFileName);
                }
            }
        }

        String correctFileName = "jd_" + nextJdNumber + "_" + jobTitle + extension;

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
