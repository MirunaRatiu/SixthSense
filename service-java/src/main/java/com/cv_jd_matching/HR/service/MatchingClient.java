package com.cv_jd_matching.HR.service;

import reactor.core.publisher.Mono;

public interface MatchingClient {
    Mono<Integer> match(Integer cvId, Integer jobDescriptionId);
}