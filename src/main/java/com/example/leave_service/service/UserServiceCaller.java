package com.example.leave_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UserServiceCaller {

    private final RestClient restClient;

    public String getUserRole(Long userId) {
        return restClient.get()
                .uri("http://localhost:8081/users/{id}/role", userId)
                .retrieve()
                .body(String.class);
    }
}
