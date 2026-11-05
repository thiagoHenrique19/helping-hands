package com.helpinghands.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpinghands.infrastructure.web.error.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    ProblemDetailSecurityHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, Problems.create(HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication required",
                "A valid bearer token is required to access this resource"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, Problems.create(HttpStatus.FORBIDDEN, "forbidden", "Access denied",
                "You are not allowed to perform this operation"));
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ProblemDetail problem)
            throws IOException {
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
