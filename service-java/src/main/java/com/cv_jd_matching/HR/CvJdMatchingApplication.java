package com.cv_jd_matching.HR;

import com.cv_jd_matching.HR.parser.CvParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Miruna
 */
@SpringBootApplication
@EnableScheduling
public class CvJdMatchingApplication {

	public static void main(String[] args) {

       SpringApplication.run(CvJdMatchingApplication.class, args);
	}

}
