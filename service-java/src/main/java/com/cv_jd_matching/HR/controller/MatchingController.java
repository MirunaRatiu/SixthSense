package com.cv_jd_matching.HR.controller;

import com.cv_jd_matching.HR.service.MatchingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/api")
public class MatchingController {

    private final MatchingClient matchingClient;

    @RequestMapping(method = RequestMethod.POST, value = "/match")
    public ResponseEntity<?> displayMatchScore(@RequestParam("cvId") Integer cvId, @RequestParam("jdId") Integer jobDescriptionId){
        return new ResponseEntity<>(matchingClient.match(cvId, jobDescriptionId).block(), HttpStatus.OK);
    }

}