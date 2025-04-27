package com.cv_jd_matching.HR.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Date;

@Entity
@Table(name = "job_description")
@Data
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id",
        scope = JobDescription.class)
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "path_name")
    private String pathName;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company_overview", columnDefinition = "TEXT")
    private String companyOverview;

    @Column(name = "key_responsibilities", columnDefinition = "TEXT")
    private String keyResponsibilities;

    @Column(name = "required_qualifications", columnDefinition = "TEXT")
    private String requiredQualifications;

    @Column(name = "preferred_skills", columnDefinition = "TEXT")
    private String preferredSkills;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at")
    private Date createdAt;

    public JobDescription() {
        createdAt = new Date(System.currentTimeMillis());
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setCompanyOverview(String companyOverview) {
        this.companyOverview = companyOverview;
    }

    public void setKeyResponsibilities(String keyResponsibilities) {
        this.keyResponsibilities = keyResponsibilities;
    }

    public void setRequiredQualifications(String requiredQualifications) {
        this.requiredQualifications = requiredQualifications;
    }

    public void setPreferredSkills(String preferredSkills) {
        this.preferredSkills = preferredSkills;
    }

    public void setBenefits(String benefits){
        this.benefits = benefits;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }
}
