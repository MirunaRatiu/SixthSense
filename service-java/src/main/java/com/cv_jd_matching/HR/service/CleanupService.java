package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.repository.ICvRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CleanupService {
    private final ICvRepository cvRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteCvs(){
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        cvRepository.deleteAllByCreatedAtBefore(Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }
}
