package com.electoral.citizen_query_service.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class VoterResponse implements Serializable {

    private String document;
    private String pollingStation;
    private String status;
}