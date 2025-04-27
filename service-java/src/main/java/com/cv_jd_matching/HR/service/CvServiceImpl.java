package com.cv_jd_matching.HR.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.CvViewDTO;
import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.error.PathException;
import com.cv_jd_matching.HR.mapper.CvMapper;
import com.cv_jd_matching.HR.repository.ICvRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class CvServiceImpl implements CvService{

    private final ICvRepository cvRepository;
    private final MatchingClient matchingClient;
    private final WebClient webClient;


    @Value("${spring.cloud.azure.storage.connection-string}")
    private String connectionString;

    @Value("cvs")
    private String containerName1;

    public List<CvViewDTO> getCvs(){
        Iterable<Cv> cvs = cvRepository.findAll();
        List<Cv> parsed = StreamSupport.stream(cvs.spliterator(), false).toList();
        return parsed.stream().map(CvMapper::mapEntityToViewDTO).toList();
    }
/*
    public void deleteFiles(List<Integer> ids){
        List<Cv> cvs = ids.stream()
                .map(cvRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        cvRepository.deleteAll(cvs);
        for(Integer id: ids){
            matchingClient.deleteCv(id).subscribe();
        }
    }
*/
private BlobContainerClient getContainerClient(String containerName) {
    com.azure.storage.blob.BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    return blobServiceClient.getBlobContainerClient(containerName);

}
public void deleteFiles(List<Integer> cvIds) {
    // Obținem job descriptions din baza de date pe baza jobIDs
    List<Cv> cvs = cvIds.stream()
            .map(id -> cvRepository.findById(id).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    // Ștergem descrierile din baza de date
    cvRepository.deleteAll(cvs);

    // Ștergem fișierele din Azure Blob Storage
    BlobContainerClient containerClient = getContainerClient(containerName1); // Numele containerului
    for (Cv cv : cvs) {
        String fileName = cv.getFileName(); // Presupunem că ai un câmp fileName în JobDescription
        if (fileName != null && !fileName.isEmpty()) {
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            // Verificăm dacă fișierul există în Azure și îl ștergem
            if (blobClient.exists()) {
                try {
                    blobClient.delete();
                    System.out.println("Cv file deleted from Azure: " + fileName);
                } catch (Exception e) {
                    System.err.println("Failed to delete Cv from Azure: " + fileName);
                    e.printStackTrace(); // Sau folosește un logger pentru a gestiona excepțiile
                }
            } else {
                System.out.println("Cv file not found in Azure: " + fileName);
            }
        }
    }

    // Trimiterea cererii de ștergere către serverul extern pentru fiecare jobID
    for (Integer id : cvIds) {
        webClient.method(HttpMethod.DELETE)
                .uri("/delete/cv")
                .bodyValue(id)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe();
    }
}

    public CvDTO getCvByPath(String path) throws PathException {
        Optional<Cv> cv = cvRepository.findCvByPathName(path);
        if(cv.isEmpty()){
            throw new PathException("Wrong path");
        }
        return CvMapper.mapEntityToDTO(cv.get());
    }
}
