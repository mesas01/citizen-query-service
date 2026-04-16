package com.electoral.citizen_query_service.service;

import org.springframework.stereotype.Service;

import com.electoral.citizen_query_service.cache.RedisCacheAdapter;
import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;
import com.electoral.citizen_query_service.mapper.VoterMapper;
import com.electoral.citizen_query_service.repository.VoterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CitizenService {

    private final VoterRepository repository;
    private final RedisCacheAdapter cache;
    private final VoterMapper mapper;

    public VoterResponse getVoterInfo(String document) {

        String key = "voter:" + document;

        Object cached = cache.get(key);
        if (cached != null) {
            return (VoterResponse) cached;
        }

        Voter voter = repository.findById(document)
                .orElseThrow(() -> new RuntimeException("Voter not found"));

        VoterResponse response = mapper.toResponse(voter);

        cache.set(key, response);

        return response;
    }
}