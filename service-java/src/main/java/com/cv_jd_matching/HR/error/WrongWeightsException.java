package com.cv_jd_matching.HR.error;

public class WrongWeightsException extends Exception{
    public WrongWeightsException(String message){
        super(message);
    }

    public WrongWeightsException(String message, Throwable cause) {
        super(message, cause);
    }
}
