package com.electoral.citizen_query_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.service.CitizenService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/citizen")
@RequiredArgsConstructor
public class CitizenController {

    private final CitizenService service;

    @GetMapping("/polling-station")
    @Operation(summary = "Get polling station and vote status")
    public VoterResponse getPollingStation(@RequestParam String document) {
        return service.getVoterInfo(document);
    }
}