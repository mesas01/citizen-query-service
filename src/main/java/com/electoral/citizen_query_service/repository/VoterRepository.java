package com.electoral.citizen_query_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.electoral.citizen_query_service.entity.Voter;

public interface VoterRepository extends JpaRepository<Voter, String> {
}