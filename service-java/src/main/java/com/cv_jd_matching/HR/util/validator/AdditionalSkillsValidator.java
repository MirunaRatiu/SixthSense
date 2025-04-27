package com.cv_jd_matching.HR.util.validator;

import java.util.List;
import java.util.Map;

public class AdditionalSkillsValidator {
    public static String validateWeights(Map<String, Integer> additionalSkills){
        int sum = 0;
        for(Map.Entry<String, Integer> skill: additionalSkills.entrySet()){
            sum += skill.getValue();
        }
        if (sum!=100)
            return "The weights do not sum up to 100! Please adjust the sliders accordingly.";
        return null;
    }
}
