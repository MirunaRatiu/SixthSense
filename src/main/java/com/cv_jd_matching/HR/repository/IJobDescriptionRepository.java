package com.cv_jd_matching.HR.repository;

import com.cv_jd_matching.HR.entity.JobDescription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IJobDescriptionRepository extends CrudRepository<JobDescription, Integer> {
    default int countAll() {
        return Math.toIntExact(count());
    }
}
