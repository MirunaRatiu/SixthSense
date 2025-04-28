package com.cv_jd_matching.HR.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HttpErrorResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "MM-dd-yyy hh:mm:ss",
            timezone = "Europe/Bucharest")
    private LocalDateTime timeStamp;
    private Integer httpStatusCode;
    private String reason;
    private String message;
}
