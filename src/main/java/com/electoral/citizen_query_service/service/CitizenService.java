package com.electoral.citizen_query_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.electoral.citizen_query_service.cache.RedisCacheAdapter;
import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;
import com.electoral.citizen_query_service.exception.ResourceNotFoundException;
import com.electoral.citizen_query_service.mapper.VoterMapper;
import com.electoral.citizen_query_service.repository.VoterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CitizenService {

    private final VoterRepository repository;
    private final RedisCacheAdapter cache;
    private final VoterMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(CitizenService.class);

    public VoterResponse getVoterInfo(String document) {

        String key = "voter:" + document;

        Object cached = cache.get(key);
        if (cached != null) {
            log.info("CACHE HIT - document={}", document);
            return (VoterResponse) cached;
        }

        log.info("CACHE MISS - querying DB - document={}", document);

        Voter voter = repository.findById(document)
                .orElseThrow(() -> {
                    log.error("Voter not found - document={}", document);
                    return new ResourceNotFoundException("Voter not found");
                });

        VoterResponse response = mapper.toResponse(voter);

        cache.set(key, response);
        log.info("CACHE STORE - document={}", document);

        return response;
    }
}