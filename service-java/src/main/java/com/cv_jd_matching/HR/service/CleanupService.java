package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.repository.ICvRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CleanupService {
    private final ICvRepository cvRepository;
    private final BlobServiceClient blobServiceClient;
    private final MatchingClient matchingClient;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteCvs(){
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        List<Cv> cvs = cvRepository.findByCreatedAtBefore(Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        for(Cv cv: cvs){
            blobServiceClient.deleteCvBlob(cv.getFileName());
            matchingClient.deleteCv(cv.getId()).subscribe();
        }
        cvRepository.deleteAllByCreatedAtBefore(Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }
}
