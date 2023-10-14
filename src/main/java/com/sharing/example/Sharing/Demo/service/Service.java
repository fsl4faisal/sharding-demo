package com.sharing.example.Sharing.Demo.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.PathParam;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public interface Service {

    //https://localhost:8081/fhy2h
    @GetMapping("/{urlId}")
    Triplet get(@PathVariable String urlId);

    @PostMapping("/**")
    Triplet post(HttpServletRequest request);

    @GetMapping("/health/check")
    String health();
}
