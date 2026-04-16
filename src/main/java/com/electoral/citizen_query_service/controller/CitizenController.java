package com.electoral.citizen_query_service.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.service.CitizenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/citizen")
@RequiredArgsConstructor
@Validated
public class CitizenController {

    private final CitizenService service;

    @GetMapping("/polling-station")
    @Operation(summary = "Get polling station and vote status by document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Voter not found")
    })
    public VoterResponse getPollingStation(
            @RequestParam
            @NotBlank(message = "document is required")
            @Pattern(regexp = "^[0-9]{3,20}$", message = "document must be numeric (3-20 digits)")
            String document
    ) {
        return service.getVoterInfo(document);
    }
}