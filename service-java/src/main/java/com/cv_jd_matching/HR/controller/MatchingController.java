package com.cv_jd_matching.HR.controller;


import com.cv_jd_matching.HR.dto.JobMatchRequestDTO;
import com.cv_jd_matching.HR.dto.JobViewMatchDTO;
import com.cv_jd_matching.HR.error.InputException;

import com.cv_jd_matching.HR.error.WrongWeightsException;
import com.cv_jd_matching.HR.service.MatchingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/api")
@CrossOrigin(origins = "http://localhost:4200")
public class MatchingController {

    private final MatchingClient matchingClient;

    @RequestMapping(method = RequestMethod.POST, value = "/match")
    public ResponseEntity<?> displayMatchScore(@RequestParam("cvId") Integer cvId, @RequestParam("jdId") Integer jobDescriptionId) throws InputException {
        return new ResponseEntity<>(matchingClient.match(cvId, jobDescriptionId).block(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/match/cv")
    public ResponseEntity<?> displayMatchScoreForCV(@RequestParam("cvId") Integer cvId){

        return new ResponseEntity<>(matchingClient.matchCv(cvId), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/match/jd")
    public ResponseEntity<?> displayMatchScoreForJobDescription(@RequestBody JobViewMatchDTO jobViewMatchDTO) throws WrongWeightsException, InputException {
        return new ResponseEntity<>(matchingClient.matchJobDescription(jobViewMatchDTO.getJdId(), jobViewMatchDTO.getAdditionalSkills()), HttpStatus.OK);
    }

}