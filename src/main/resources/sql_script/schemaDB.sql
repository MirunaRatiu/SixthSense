DROP SCHEMA IF EXISTS match_db;
CREATE SCHEMA match_db;
USE match_db;
CREATE TABLE cv (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    file_name VARCHAR(255),
                    path_name VARCHAR(255),
                    name VARCHAR(255),
                    technical_skills JSON,
                    foreign_languages JSON,
                    education JSON,
                    certifications JSON,
                    project_experience JSON,
                    work_experience JSON,
                    others JSON,
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

