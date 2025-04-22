package com.cv_jd_matching.HR.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import com.cv_jd_matching.HR.util.StringListConverter;
import lombok.Data;

import java.sql.Date;
import java.util.List;

@Entity
@Table(name ="cv")
@Data
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

    @Column(name = "technical_skills", columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> technicalSkills;

    @Column(name = "foreign_languages", columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> foreignLanguages;

    @Column(name = "education", columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> education;

    @Column(name = "certifications", columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> certifications;

    @Column(name = "project_experience", columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> projectExperience;

    @Column(name = "work_experience", columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> workExperience;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "others")
    @Convert(converter = StringListConverter.class)
    private List<String> others;



    public Cv() {
        createdAt = new Date(System.currentTimeMillis());
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public void setTechnicalSkills(List<String> technicalSkills) {
        this.technicalSkills = technicalSkills;
    }

    public void setForeignLanguages(List<String> foreignLanguages) {
        this.foreignLanguages = foreignLanguages;
    }

    public void setEducation(List<String> education) {
        this.education = education;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public void setProjectExperience(List<String> projectExperience) {
        this.projectExperience = projectExperience;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOthers(List<String> others) {
        this.others = others;
    }

    public void setWorkExperience(List<String> workExperience) {
        this.workExperience = workExperience;
    }

    public String getName() {
        return name;
    }
}
