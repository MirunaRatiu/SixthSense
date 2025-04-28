package com.cv_jd_matching.HR.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;

import com.cv_jd_matching.HR.error.InputException;

import com.cv_jd_matching.HR.error.PathException;
import com.cv_jd_matching.HR.mapper.JobDescriptionMapper;
import com.cv_jd_matching.HR.repository.IJobDescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class JobDescriptionServiceImpl implements JobDescriptionService {
    private final IJobDescriptionRepository jobDescriptionRepository;
    private final MatchingClient matchingClient;

    private final WebClient webClient;


    @Value("${spring.cloud.azure.storage.connection-string}")
    private String connectionString;

    @Value("${spring.cloud.azure.storage.blob.container-name-2}")
    private String containerName2;

    public void deleteFiles(List<Integer> jobIDs){
        List<JobDescription> jobDescriptions = jobIDs.stream()
                .map(jobDescriptionRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        jobDescriptionRepository.deleteAll(jobDescriptions);
        for(Integer id: jobIDs){
            matchingClient.deleteJobDescription(id).subscribe();

        }
    }
    private BlobContainerClient getContainerClient(String containerName) {
        com.azure.storage.blob.BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        return blobServiceClient.getBlobContainerClient(containerName);

    }
    public void deleteJobDescription(List<Integer> jobIDs) {
        // Obținem job descriptions din baza de date pe baza jobIDs
        List<JobDescription> jobDescriptions = jobIDs.stream()
                .map(id -> jobDescriptionRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Ștergem descrierile din baza de date
        jobDescriptionRepository.deleteAll(jobDescriptions);

        // Ștergem fișierele din Azure Blob Storage
        BlobContainerClient containerClient = getContainerClient(containerName2); // Numele containerului
        for (JobDescription jobDescription : jobDescriptions) {
            String fileName = jobDescription.getFileName(); // Presupunem că ai un câmp fileName în JobDescription
            if (fileName != null && !fileName.isEmpty()) {
                BlobClient blobClient = containerClient.getBlobClient(fileName);

                // Verificăm dacă fișierul există în Azure și îl ștergem
                if (blobClient.exists()) {
                    try {
                        blobClient.delete();
                        System.out.println("Job description file deleted from Azure: " + fileName);
                    } catch (Exception e) {
                        System.err.println("Failed to delete job description from Azure: " + fileName);
                        e.printStackTrace(); // Sau folosește un logger pentru a gestiona excepțiile
                    }
                } else {
                    System.out.println("Job description file not found in Azure: " + fileName);
                }
            }
        }

        // Trimiterea cererii de ștergere către serverul extern pentru fiecare jobID
        for (Integer id : jobIDs) {
            webClient.method(HttpMethod.DELETE)
                    .uri("/delete/jd")
                    .bodyValue(id)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe();

        }
    }


    public List<JobDescriptionViewDTO> getJobDescriptions(){
        Iterable<JobDescription> jobs = jobDescriptionRepository.findAll();
        List<JobDescription> jobList = StreamSupport.stream(jobs.spliterator(), false).toList();
        return jobList.stream().map(JobDescriptionMapper::mapEntityToViewDTO).toList();
    }

    public JobDescriptionViewDTO getJobDescriptionById(Integer id) throws InputException {
        Optional<JobDescription> job = jobDescriptionRepository.findById(id);
        if(job.isEmpty()){
            throw new InputException("Wrong id");
        }
        return JobDescriptionMapper.mapEntityToViewDTO(job.get());
    }

    public JobDescriptionDTO getJobDescriptionByPath(String path) throws PathException {
        Optional<JobDescription> job = jobDescriptionRepository.findJobDescriptionByPathName(path);
        if(job.isEmpty()){
            throw new PathException("Wrong url");
        }
        return JobDescriptionMapper.mapEntityToDTO(job.get());
    }
}
