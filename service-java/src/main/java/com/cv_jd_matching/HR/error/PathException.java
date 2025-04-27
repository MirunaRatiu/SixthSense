package com.cv_jd_matching.HR.error;

public class PathException extends Exception{
    public PathException(String message){
        super(message);
    }

    public PathException(String message, Throwable cause) {
        super(message, cause);
    }
}
