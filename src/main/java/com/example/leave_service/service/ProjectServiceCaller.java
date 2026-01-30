package com.example.leave_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ProjectServiceCaller {

    private final RestClient restClient;

    public Long getManagerId(Long employeeId) {
        return restClient.get()
                .uri("http://localhost:8082/projects/manager/{id}", employeeId)
                .retrieve()
                .body(Long.class);
    }

}
