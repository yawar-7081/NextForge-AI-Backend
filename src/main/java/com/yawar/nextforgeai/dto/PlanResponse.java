package com.yawar.nextforgeai.dto;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanResponse {
        String id;
        String name;
        Integer maxProjects;
        Integer maxTokenPerDay;
        Boolean unlimitedAi;
        String price;
}
