package com.electoral.citizen_query_service.dto;

import lombok.Data;

@Data
public class VoterResponse {

    private String document;
    private String pollingStation;
    private String status;
}