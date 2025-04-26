package com.cv_jd_matching.HR.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;

import java.sql.Date;

@Entity
@Table(name = "cv")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id",
        scope = Cv.class)
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "path_name")
    private String pathName;

    @Column(name = "name")
    private String name;

    @Column(name = "technical_skills", columnDefinition = "TEXT")
    private String technicalSkills;

    @Column(name = "foreign_languages", columnDefinition = "TEXT")
    private String foreignLanguages;

    @Column(name = "education", columnDefinition = "TEXT")
    private String education;

    @Column(name = "certifications", columnDefinition = "TEXT")
    private String certifications;

    @Column(name = "project_experience", columnDefinition = "TEXT")
    private String projectExperience;

    @Column(name = "work_experience", columnDefinition = "TEXT")
    private String workExperience;

    @Column(name = "others", columnDefinition = "TEXT")
    private String others;

    @Column(name = "created_at")
    private Date createdAt;

    public Cv() {
        this.createdAt = new Date(System.currentTimeMillis());
    }

    // Setters
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTechnicalSkills(String technicalSkills) {
        this.technicalSkills = technicalSkills;
    }

    public void setForeignLanguages(String foreignLanguages) {
        this.foreignLanguages = foreignLanguages;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public void setProjectExperience(String projectExperience) {
        this.projectExperience = projectExperience;
    }

    public void setWorkExperience(String workExperience) {
        this.workExperience = workExperience;
    }

    public void setOthers(String others) {
        this.others = others;
    }


    // Getters (exemplu)
    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getId() {
        return id;
    }
}
