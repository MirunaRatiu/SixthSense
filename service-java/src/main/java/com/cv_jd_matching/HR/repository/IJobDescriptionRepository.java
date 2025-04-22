package com.cv_jd_matching.HR.repository;

import com.cv_jd_matching.HR.entity.JobDescription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IJobDescriptionRepository extends CrudRepository<JobDescription, Integer> {
    default int countAll() {
        return Math.toIntExact(count());
    }

    Optional<JobDescription> findJobDescriptionByJobTitle(String title);
}
