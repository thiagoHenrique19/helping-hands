package com.helpinghands.infrastructure.web.error;

import com.helpinghands.infrastructure.web.RequestIdFilter;
import java.net.URI;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class Problems {

    private static final String TYPE_PREFIX = "urn:helping-hands:problem:";

    private Problems() {
    }

    public static ProblemDetail create(HttpStatus status, String slug, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + slug));
        problem.setTitle(title);
        return enrich(problem);
    }

    public static ProblemDetail enrich(ProblemDetail problem) {
        problem.setProperty("timestamp", Instant.now());
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }
}
