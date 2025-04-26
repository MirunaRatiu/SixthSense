package com.cv_jd_matching.HR.controller;

import com.cv_jd_matching.HR.service.JobDescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/api/jobDescription")

public class JobDescriptionController {
    private final JobDescriptionService jobDescriptionService;

    @RequestMapping(method = RequestMethod.GET, value="/all")
    public ResponseEntity<?> displayAllJobDescriptions(){
        return new ResponseEntity<>(jobDescriptionService.getJobDescriptions(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/delete")
    public ResponseEntity<?> deleteSelectedJobDescriptions(@RequestBody List<Integer> ids){
        jobDescriptionService.deleteFiles(ids);
        return new ResponseEntity<>("Successfully deleted files", HttpStatus.OK);
    }
}
