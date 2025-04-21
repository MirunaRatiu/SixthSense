package com.cv_jd_matching.HR;

import com.cv_jd_matching.HR.parser.CvParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Miruna
 */
@SpringBootApplication
public class CvJdMatchingApplication {

	public static void main(String[] args) {

//		String filePath = "hhhhh.docx";
//
//		// Deschide fișierul .docx ca InputStream
//		try (InputStream inputStream = new FileInputStream(filePath)) {
//			// Apelează funcția extractTextFromDocx
//			String text = CvParser.extractTextFromDocx(inputStream);
//
//			// Afișează textul extras
//			System.out.println("Text extrase din fișierul .docx:");
//			System.out.println(text);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
       SpringApplication.run(CvJdMatchingApplication.class, args);
	}

}
