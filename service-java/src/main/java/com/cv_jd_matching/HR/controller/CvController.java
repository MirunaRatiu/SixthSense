package com.cv_jd_matching.HR.controller;

import com.cv_jd_matching.HR.service.CvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping(value = "/api/cv")
public class CvController {
    private final CvService cvService;

    @RequestMapping(method = RequestMethod.GET, value = "/all")
    public ResponseEntity<?> displayAllCvs(){
        return new ResponseEntity<>(cvService.getCvs(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/delete")
    public ResponseEntity<?> deleteSelectedCvs(@RequestBody List<Integer> ids){
        cvService.deleteFiles(ids);
        return new ResponseEntity<>("Successfully deleted files", HttpStatus.OK);
    }

}
