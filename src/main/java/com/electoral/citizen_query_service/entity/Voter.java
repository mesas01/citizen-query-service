package com.electoral.citizen_query_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
public class Voter {

    @Id
    private String document;

    private String pollingStation;
    private boolean hasVoted;
}