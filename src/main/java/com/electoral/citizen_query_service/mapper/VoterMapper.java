package com.electoral.citizen_query_service.mapper;

import org.springframework.stereotype.Component;

import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;

@Component
public class VoterMapper {

    public VoterResponse toResponse(Voter voter) {
        VoterResponse response = new VoterResponse();
        response.setDocument(voter.getDocument());
        response.setPollingStation(voter.getPollingStation());
        response.setStatus(voter.isHasVoted() ? "VOTED" : "NOT_VOTED");
        return response;
    }
}