DROP SCHEMA IF EXISTS match_db;
CREATE SCHEMA match_db;
USE match_db;
CREATE TABLE cv (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    file_name VARCHAR(255),
                    path_name VARCHAR(255),
                    name VARCHAR(255),
                    technical_skills TEXT,
                    foreign_languages TEXT,
                    education TEXT,
                    certifications TEXT,
                    project_experience TEXT,
                    work_experience TEXT,
                    others TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_description (
                                 id INT PRIMARY KEY AUTO_INCREMENT,
                                 file_name VARCHAR(255),
                                 path_name VARCHAR(255),
                                 job_title VARCHAR(255),
                                 company_overview TEXT,
                                 key_responsibilities TEXT,
                                 required_qualifications TEXT,
                                 preferred_skills TEXT,
                                 benefits TEXT,
                                 message TEXT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

