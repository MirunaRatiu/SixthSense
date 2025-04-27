package com.cv_jd_matching.HR.mapper;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.CvViewDTO;
import com.cv_jd_matching.HR.entity.Cv;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CvMapper {
    public static CvDTO mapEntityToDTO(Cv cv){
        return CvDTO.builder()
                .projectExperience(String.valueOf(cv.getProjectExperience()))
                .certifications(String.valueOf(cv.getCertifications()))
                .education(String.valueOf(cv.getEducation()))
                .foreignLanguages(String.valueOf(cv.getForeignLanguages()))
                .workExperience(String.valueOf(cv.getWorkExperience()))
                .others(String.valueOf(cv.getOthers()))
                .technicalSkills(String.valueOf(cv.getTechnicalSkills()))
                .id(cv.getId())
                .build();
    }

    public static CvViewDTO mapEntityToViewDTO(Cv cv){
        return CvViewDTO.builder()
                .name(cv.getName())
                .id(cv.getId())
                .skills(splitString(cv.getTechnicalSkills(), "skill"))
                .languages(splitString(cv.getForeignLanguages(), "language"))// this will be changed
                .accessLink(cv.getPathName())
                .build();
    }

    private static List<String> splitString(String jsonString, String fieldName) {
        String json = jsonString
                .replace("=", ":")
                .replaceAll("([a-zA-Z ]+):", "\"$1\":")
                .replaceAll(":([^\",}\\]]+)", ":\"$1\"");
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
        List<Map<String, String>> listOfMaps = gson.fromJson(json, listType);

        // Extract values for the specified field
        List<String> values = new ArrayList<>();
        for (Map<String, String> map : listOfMaps) {
            if (map.containsKey(fieldName)) {
                values.add(map.get(fieldName));
            }
        }
        return values;
    }
}